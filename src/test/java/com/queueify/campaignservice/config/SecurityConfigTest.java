package com.queueify.campaignservice.config;

import com.queueify.campaignservice.authentication.controller.AuthController;
import com.queueify.campaignservice.authentication.jwt.JwtAuthenticationFilter;
import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.service.AuthService;
import com.queueify.campaignservice.authentication.service.RefreshTokenService;
import com.queueify.campaignservice.emailaccount.controller.EmailAccountController;
import com.queueify.campaignservice.emailaccount.service.EmailAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, EmailAccountController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmailAccountService emailAccountService;

    @Test
    @WithAnonymousUser
    void allowsAnonymousAccessToAuthenticationEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void rejectsAnonymousAccessToProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/email-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAuthenticatedAccessToProtectedEndpoints() throws Exception {
        when(jwtService.extractSubject("valid-token")).thenReturn("user@example.com");

        mockMvc.perform(get("/api/v1/users/1/email-accounts")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }
}
