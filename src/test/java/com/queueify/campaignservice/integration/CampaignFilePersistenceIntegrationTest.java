package com.queueify.campaignservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.campaign.repository.CampaignRepository;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "QUEUEIFY_MYSQL_INTEGRATION_TEST", matches = "true")
@SpringBootTest(properties = {"management.health.mail.enabled=false", "queueify.aws.s3.bucket=queueify-test-bucket"})
@AutoConfigureMockMvc
@Transactional
class CampaignFilePersistenceIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private EmailAccountRepository senders;
    @Autowired private CampaignRepository campaigns;
    @Autowired private JwtService jwt;
    @Autowired private EntityManager entityManager;
    @MockitoBean private S3Client s3;

    @Test
    void savesServerGeneratedS3UrisAndReloadsThemFromMysql() throws Exception {
        String email = "upload-test-" + UUID.randomUUID() + "@example.com";
        LocalDateTime now = LocalDateTime.now();
        User owner = users.saveAndFlush(new User("Upload Test", email, "unused-test-hash", now, now));
        EmailAccount sender = senders.saveAndFlush(new EmailAccount(owner.getId(), "sender@gmail.com",
                EmailProvider.GMAIL, EmailAuthType.APP_PASSWORD, "test-only-credential", EmailAccountStatus.ACTIVE,
                null, now, now));
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        String json = mapper.writeValueAsString(java.util.Map.of("campaignName", "Multipart persistence test",
                "subject", "Hello", "totalEmails", 1, "senderEmailAccountId", sender.getId()));

        var result = mvc.perform(multipart("/api/v1/users/" + owner.getId() + "/campaigns")
                        .file(new MockMultipartFile("campaign", "campaign.json", "application/json", json.getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("template", "welcome.html", "text/html", "<p>Hello</p>".getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("recipients", "leads.csv", "text/csv", "email\ntest@example.com\n".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwt.generateAccessToken(email)))
                .andExpect(status().isCreated()).andReturn();

        long campaignId = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        entityManager.flush();
        entityManager.clear();
        var saved = campaigns.findById(campaignId).orElseThrow();
        assertEquals(CampaignStatus.DRAFT, saved.getStatus());
        assertEquals(owner.getId(), saved.getUserId());
        String prefix = "s3://queueify-test-bucket/campaign-assets/users/" + owner.getId() + "/uploads/";
        assertTrue(saved.getTemplateUrl().startsWith(prefix + "templates/"));
        assertTrue(saved.getTemplateUrl().endsWith("-welcome.html"));
        assertTrue(saved.getCsvUrl().startsWith(prefix + "recipients/"));
        assertTrue(saved.getCsvUrl().endsWith("-leads.csv"));
    }
}
