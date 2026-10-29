package com.queueify.campaignservice.start;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@JdbcTest(properties="queueify.processor-start.enabled=true")
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import(CampaignStartOutbox.class)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class CampaignStartOutboxTest {
    static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4");
    static { MYSQL.start(); }
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CampaignStartOutbox outbox;

    @BeforeEach void seed() {
        jdbc.update("DELETE FROM campaign_outbox_events");
        jdbc.update("DELETE FROM campaigns");
        jdbc.update("DELETE FROM email_accounts");
        jdbc.update("DELETE FROM users");
        jdbc.update("INSERT INTO users VALUES (5,'Test','test@example.com','hash',NOW(),NOW())");
        jdbc.update("""
                INSERT INTO email_accounts(id,user_id,sender_email,provider,auth_type,encrypted_app_password,status,created_at,updated_at)
                VALUES (9,5,'sender@example.com','GMAIL','APP_PASSWORD','encrypted','ACTIVE',NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO campaigns(id,campaign_name,user_id,subject,template_url,csv_url,total_emails,emails_sent,
                    emails_failed,status,sender_email_account_id,created_at,updated_at)
                VALUES (42,'test',5,'subject','s3://assets/template','s3://assets/campaign-assets/users/5/input.csv',
                    2,0,0,'RUNNING',9,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO campaign_outbox_events(id,campaign_id,user_id,event_type,status,created_at)
                VALUES (7,42,5,'CAMPAIGN_START_REQUESTED','PENDING',NOW())
                """);
    }
    @Test void claimsOnlyCommittedRunningCampaignIntentAndPreservesSnapshotOnRetry() {
        var first=outbox.claimOne().orElseThrow();
        assertThat(outbox.claimOne()).isEmpty();
        assertThat(outbox.savePayload(first,"{\"eventId\":7}")).isTrue();
        outbox.failed(first);
        assertThat(outbox.claimOne()).isEmpty();
        jdbc.update("UPDATE campaign_outbox_events SET next_attempt_at=TIMESTAMPADD(SECOND,-1,NOW())");
        var retry=outbox.claimOne().orElseThrow();
        assertThat(retry.payload()).contains("7");
        outbox.published(first);
        assertThat(jdbc.queryForObject("SELECT status FROM campaign_outbox_events",String.class)).isEqualTo("PENDING");
        outbox.published(retry);
        assertThat(outbox.claimOne()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM campaign_outbox_events",String.class)).isEqualTo("PUBLISHED");
    }
    @Test void pausedCampaignIsNotRelayed() {
        jdbc.update("UPDATE campaigns SET status='PAUSED'");
        assertThat(outbox.claimOne()).isEmpty();
    }
    @Test void expiredClaimCanBeRecovered() {
        var old=outbox.claimOne().orElseThrow();
        jdbc.update("UPDATE campaign_outbox_events SET relay_claim_until=TIMESTAMPADD(SECOND,-1,NOW())");
        var next=outbox.claimOne().orElseThrow();
        assertThat(next.token()).isNotEqualTo(old.token());
        assertThat(outbox.savePayload(old,"{}")).isFalse();
    }
    @Test void concurrentRelaysCannotOwnTheSameEvent() throws Exception {
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{start.await(); return outbox.claimOne();});
            var b=executor.submit(()->{start.await(); return outbox.claimOne();});
            start.countDown();
            int claimed=(a.get(5,TimeUnit.SECONDS).isPresent()?1:0)+(b.get(5,TimeUnit.SECONDS).isPresent()?1:0);
            assertThat(claimed).isEqualTo(1);
        }
    }
}
