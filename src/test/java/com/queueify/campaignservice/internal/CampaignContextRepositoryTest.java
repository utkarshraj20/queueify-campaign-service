package com.queueify.campaignservice.internal;

import com.queueify.campaignservice.storage.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;
import static org.assertj.core.api.Assertions.*;

@JdbcTest(properties={"queueify.internal.campaign-api.enabled=true","queueify.internal.campaign-api.sender-credentials-enabled=true","queueify.aws.s3.bucket=assets"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({SenderCredentialRepository.class,CampaignContextRepository.class,CampaignContextRepositoryTest.Storage.class})
class CampaignContextRepositoryTest {
    static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4");
    static { MYSQL.start(); }
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Configuration(proxyBeanMethods=false) static class Storage {
        @Bean S3Properties storage() { var p=new S3Properties();p.setBucket("assets");p.setRegion("ap-southeast-2");p.setBasePrefix("campaign-assets");return p; }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CampaignContextRepository repository;
    @Autowired SenderCredentialRepository senders;
    @BeforeEach void seed() {
        jdbc.update("INSERT INTO users VALUES (5,'Test','test@example.com','hash',NOW(),NOW())");
        jdbc.update("INSERT INTO users VALUES (6,'Other','other@example.com','hash',NOW(),NOW())");
        jdbc.update("""
                INSERT INTO email_accounts(id,user_id,sender_email,provider,auth_type,encrypted_app_password,status,created_at,updated_at)
                VALUES (9,5,'sender@example.com','GMAIL','APP_PASSWORD','not-returned','ACTIVE',NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO campaigns(id,campaign_name,user_id,subject,template_url,csv_url,total_emails,emails_sent,emails_failed,
                    status,sender_email_account_id,created_at,updated_at)
                VALUES (42,'test',5,'Hello','s3://assets/campaign-assets/users/5/templates/welcome.html','s3://assets/input.csv',
                    1,0,0,'RUNNING',9,NOW(),NOW())
                """);
    }
    @Test void credentialQueryChecksCampaignSenderIdentityAndSupportedAuthentication() {
        jdbc.update("UPDATE email_accounts SET sender_email='sender@gmail.com'");
        assertThat(senders.find(999)).isEmpty();
        var sender=senders.find(42).orElseThrow();
        assertThat(sender.senderId()).isEqualTo(9);
        assertThat(sender.envelope()).isEqualTo("not-returned");
        assertThat(sender.eligible()).isTrue();
        for (String sql : new String[]{"UPDATE campaigns SET status='PAUSED'", "UPDATE email_accounts SET status='INACTIVE'",
                "UPDATE email_accounts SET user_id=6", "UPDATE email_accounts SET auth_type='OAUTH'",
                "UPDATE email_accounts SET provider='SMTP'"}) {
            jdbc.update(sql);
            assertThat(senders.find(42).orElseThrow().eligible()).isFalse();
            jdbc.update("UPDATE campaigns SET status='RUNNING'");
            jdbc.update("UPDATE email_accounts SET status='ACTIVE',user_id=5,auth_type='APP_PASSWORD',provider='GMAIL'");
        }
    }

    @Test void loadsOnlyTheRequestedCampaignAndChecksReadEligibility() {
        assertThat(repository.find(999)).isEmpty();
        assertThat(repository.eligible(repository.find(42).orElseThrow())).isTrue();
        jdbc.update("UPDATE campaigns SET status='PAUSED'");
        assertThat(repository.eligible(repository.find(42).orElseThrow())).isFalse();
        jdbc.update("UPDATE campaigns SET status='RUNNING'");jdbc.update("UPDATE email_accounts SET status='INACTIVE'");
        assertThat(repository.eligible(repository.find(42).orElseThrow())).isFalse();
    }
    @Test void rejectsForeignOwnerAndUnexpectedTemplateSource() {
        jdbc.update("UPDATE email_accounts SET user_id=6");
        assertThat(repository.eligible(repository.find(42).orElseThrow())).isFalse();
        jdbc.update("UPDATE email_accounts SET user_id=5");
        for(String uri:new String[]{"s3://other/campaign-assets/users/5/template.html","s3://assets/campaign-assets/users/6/template.html"}) {
            jdbc.update("UPDATE campaigns SET template_url=?",uri);
            assertThat(repository.eligible(repository.find(42).orElseThrow())).isFalse();
        }
    }
}
