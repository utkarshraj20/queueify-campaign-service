package com.queueify.campaignservice.progress;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.containers.MySQLContainer;
import static org.assertj.core.api.Assertions.*;

@JdbcTest(properties="queueify.progress.enabled=true")
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import(CampaignProgressRepository.class)
class CampaignProgressRepositoryTest {
    static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4");
    static {MYSQL.start();}
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CampaignProgressRepository repository;
    private final CampaignProgressRepository.Candidate candidate=new CampaignProgressRepository.Candidate(42,7);
    @BeforeEach void seed() {
        jdbc.update("INSERT INTO users VALUES (5,'Test','progress@example.com','hash',NOW(),NOW())");
        jdbc.update("INSERT INTO email_accounts(id,user_id,sender_email,provider,auth_type,encrypted_app_password,status,created_at,updated_at) VALUES (9,5,'sender@gmail.com','GMAIL','APP_PASSWORD','fixture','ACTIVE',NOW(),NOW())");
        jdbc.update("""
                INSERT INTO campaigns(id,campaign_name,user_id,subject,template_url,csv_url,total_emails,emails_sent,emails_failed,status,sender_email_account_id,created_at,updated_at)
                VALUES (42,'test',5,'Hello','s3://assets/template','s3://assets/recipients',99,0,0,'RUNNING',9,NOW(),NOW())
                """);
        jdbc.update("INSERT INTO campaign_outbox_events(id,campaign_id,user_id,event_type,status,created_at) VALUES (7,42,5,'CAMPAIGN_START_REQUESTED','PUBLISHED',NOW())");
    }
    private RunProgress ready(long sent,long failed,long unknown) {return new RunProgress(42,"start-7","READY",3L,0L,3,sent,failed,unknown,0,3-sent-failed-unknown);}
    private String state() {return jdbc.queryForObject("SELECT status FROM campaigns WHERE id=42",String.class);}
    @Test void projectionTimestampUsesTheSameApplicationClockAsCreation() {
        var created=java.time.LocalDateTime.now().minusSeconds(1);
        jdbc.update("UPDATE campaigns SET created_at=?,updated_at=?",created,created);
        repository.apply(candidate,ready(1,0,0));
        var updated=jdbc.queryForObject("SELECT updated_at FROM campaigns",java.time.LocalDateTime.class);
        assertThat(updated).isAfterOrEqualTo(created);
    }
    @Test void projectsRealCountsAndCompletesWithoutDoubleCounting() {
        assertThat(repository.candidates(0,100)).containsExactly(candidate);
        assertThat(repository.apply(candidate,ready(1,0,0))).isTrue();
        assertThat(jdbc.queryForObject("SELECT total_emails FROM campaigns",Integer.class)).isEqualTo(3);
        assertThat(repository.apply(candidate,ready(1,0,0))).isFalse();
        assertThat(repository.apply(candidate,ready(0,0,0))).isFalse();
        assertThat(repository.apply(candidate,ready(3,0,0))).isTrue();assertThat(state()).isEqualTo("COMPLETED");
        assertThat(repository.apply(candidate,ready(1,0,0))).isFalse();assertThat(repository.candidates(0,100)).isEmpty();
    }
    @Test void failuresCompleteAndUncertaintyNeverCompletes() {
        repository.apply(candidate,ready(1,0,1));assertThat(state()).isEqualTo("RUNNING");
        assertThat(jdbc.queryForObject("SELECT failure_reason FROM campaigns",String.class)).contains("manual reconciliation");
        repository.apply(candidate,ready(2,1,0));assertThat(state()).isEqualTo("COMPLETED_WITH_FAILURES");
        assertThat(jdbc.queryForObject("SELECT emails_failed FROM campaigns",Integer.class)).isEqualTo(1);
    }
    @Test void preservesPauseUntilTerminalAndDoesNotResurrectFailedCampaigns() {
        jdbc.update("UPDATE campaigns SET status='PAUSED',paused_reason='Operator pause'");
        repository.apply(candidate,ready(1,0,0));assertThat(state()).isEqualTo("PAUSED");
        repository.apply(candidate,ready(3,0,0));assertThat(state()).isEqualTo("COMPLETED");
        jdbc.update("UPDATE campaigns SET status='FAILED'");assertThat(repository.apply(candidate,ready(3,0,0))).isFalse();
    }
    @Test void pendingPreparationAndWrongRunCannotInventCompletion() {
        assertThat(repository.apply(candidate,new RunProgress(42,"start-7","PROCESSING",null,null,0,0,0,0,0,0))).isFalse();
        assertThat(state()).isEqualTo("RUNNING");
        assertThatThrownBy(()->repository.apply(candidate,new RunProgress(42,"start-8","READY",3L,0L,3,3,0,0,0,0))).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.apply(new CampaignProgressRepository.Candidate(42,8),new RunProgress(42,"start-8","READY",3L,0L,3,3,0,0,0,0))).isFalse();
    }
    @Test void blockedPreparationBecomesVisibleCampaignFailure() {
        repository.apply(candidate,new RunProgress(42,"start-7","BLOCKED",2L,1L,2,0,0,0,0,2));
        assertThat(state()).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT failure_reason FROM campaigns",String.class)).contains("CSV");
        assertThat(jdbc.queryForObject("SELECT emails_sent FROM campaigns",Integer.class)).isZero();
    }
    @Test void failedPreparationDoesNotClaimPartialCsvCountAsAuthoritative() {
        repository.apply(candidate,new RunProgress(42,"start-7","FAILED",null,null,1,0,0,0,0,1));
        assertThat(state()).isEqualTo("FAILED");assertThat(jdbc.queryForObject("SELECT total_emails FROM campaigns",Integer.class)).isEqualTo(99);
    }
}
