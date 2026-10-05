package com.queueify.campaignservice.integration;

import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.service.AuthService;
import com.queueify.campaignservice.authentication.service.RefreshTokenService;
import com.queueify.campaignservice.campaign.service.CampaignService;
import com.queueify.campaignservice.emailaccount.service.EmailAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "management.health.mail.enabled=false"
        }
)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private EmailAccountService emailAccountService;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    void allowsAnonymousAccessToLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsAnonymousAccessToHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/email-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedBearerTokenAtProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/email-accounts")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidAccessTokenWithoutCreatingSession() throws Exception {
        String accessToken = jwtService.generateAccessToken("user@example.com");

        mockMvc.perform(get("/api/v1/users/1/email-accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
}
