package com.queueify.campaignservice.internal;

import com.queueify.campaignservice.authentication.jwt.JwtAuthenticationFilter;
import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes=InternalCampaignHttpTest.TestApp.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"queueify.internal.campaign-api.enabled=true","queueify.internal.campaign-api.sender-credentials-enabled=true","queueify.internal.campaign-api.token=test-service-token-12345678901234567890"})
class InternalCampaignHttpTest {
    private static final String TOKEN="test-service-token-12345678901234567890";
    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({SecurityConfig.class,JwtAuthenticationFilter.class,InternalCampaignSecurity.class,InternalCampaignController.class,InternalSenderCredentialController.class})
    static class TestApp {}
    @Autowired TestRestTemplate http;
    @MockitoBean SenderCredentialRepository senders;
    @MockitoBean com.queueify.campaignservice.emailaccount.credential.CredentialCipher cipher;
    @MockitoBean JwtService jwt;
    @MockitoBean CampaignContextRepository repository;
    private ResponseEntity<String> get(String path,String token) {
        var headers=new HttpHeaders();if(token!=null)headers.setBearerAuth(token);
        return http.exchange(path,HttpMethod.GET,new HttpEntity<>(headers),String.class);
    }
    @Test void onlyInternalTokenCanReadContext() {
        when(jwt.extractSubject("user-jwt")).thenReturn("user@example.com");
        for(String token:new String[]{null,"wrong-token","user-jwt"}) {
            assertThat(get("/internal/v1/campaigns/42/delivery-context",token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        verifyNoInteractions(repository);
    }
    @Test void returnsMetadataWithoutCredentialsAndPreservesErrorStatuses() {
        var result=new CampaignContextRepository.Result(new CampaignDeliveryContext(42,5,"Hello",
                "s3://assets/campaign-assets/users/5/uploads/templates/welcome.html",9),"RUNNING","ACTIVE",5);
        when(repository.find(42)).thenReturn(Optional.of(result));when(repository.eligible(result)).thenReturn(true);
        var response=get("/internal/v1/campaigns/42/delivery-context",TOKEN);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getBody()).contains("\"campaignId\":42","\"subject\":\"Hello\"","\"senderEmailAccountId\":9")
                .doesNotContain("password","credential",TOKEN);
        when(repository.eligible(result)).thenReturn(false);
        assertThat(get("/internal/v1/campaigns/42/delivery-context",TOKEN).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        when(repository.find(42)).thenReturn(Optional.empty());
        assertThat(get("/internal/v1/campaigns/42/delivery-context",TOKEN).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/internal/v1/campaigns/0/delivery-context",TOKEN).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @Test void senderCredentialsRequireServiceTokenAndEncryptedEligibleAccount() {
        for (String token : new String[]{null,"wrong-token","user-jwt"}) {
            assertThat(get("/internal/v1/campaigns/42/sender-credentials",token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        verifyNoInteractions(senders,cipher);
        var sender = new SenderCredentialRepository.Sender(5,"RUNNING",9,5,"sender@gmail.com","GMAIL","APP_PASSWORD","ACTIVE","qcred:v1:example");
        when(senders.find(42)).thenReturn(Optional.of(sender));
        when(cipher.decrypt(5,"qcred:v1:example")).thenReturn("test-only-password");
        var response = get("/internal/v1/campaigns/42/sender-credentials",TOKEN);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getBody()).contains("\"appPassword\":\"test-only-password\"", "\"senderEmailAccountId\":9");
        assertThat(sender.toString()).doesNotContain("qcred:");
        when(cipher.decrypt(5,"qcred:v1:example")).thenThrow(new IllegalArgumentException("sensitive-cause"));
        var failed = get("/internal/v1/campaigns/42/sender-credentials",TOKEN);
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(failed.getBody()).doesNotContain("sensitive-cause", "qcred:", "test-only-password");
        when(senders.find(42)).thenReturn(Optional.empty());
        assertThat(get("/internal/v1/campaigns/42/sender-credentials",TOKEN).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/internal/v1/campaigns/0/sender-credentials",TOKEN).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test void rejectsForwardedCleartextCredentialRequestsBeforeDatabaseAccess() {
        var headers = new HttpHeaders(); headers.setBearerAuth(TOKEN); headers.set("X-Forwarded-For","203.0.113.5");
        assertThat(http.exchange("/internal/v1/campaigns/42/sender-credentials",HttpMethod.GET,
                new HttpEntity<>(headers),String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(senders,cipher);
    }

    @Test void serviceTokenDoesNotGrantAccessToUserApi() {
        assertThat(get("/api/v1/me",TOKEN).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void serviceTokenCannotAccessOtherInternalRoutesOrWriteMethods() {
        assertThat(get("/internal/v1/unrelated",TOKEN).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var headers=new HttpHeaders();headers.setBearerAuth(TOKEN);
        assertThat(http.exchange("/internal/v1/campaigns/42/delivery-context",HttpMethod.POST,
                new HttpEntity<>(headers),String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(repository);
    }

}
