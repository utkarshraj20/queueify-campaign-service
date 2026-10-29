package com.queueify.campaignservice.integration;

import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.jwt.JwtAuthenticationFilter;
import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import com.queueify.campaignservice.campaign.controller.CampaignController;
import com.queueify.campaignservice.campaign.service.CampaignService;
import com.queueify.campaignservice.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes=ErrorDispatchIntegrationTest.TestApp.class,
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorDispatchIntegrationTest {
    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({SecurityConfig.class,JwtAuthenticationFilter.class,CampaignController.class})
    static class TestApp {}

    @Autowired TestRestTemplate http;
    @MockitoBean JwtService jwt;
    @MockitoBean UserRepository users;
    @MockitoBean CampaignService campaigns;

    @Test void authenticatedWrongOwnerKeepsForbiddenThroughServletErrorDispatch() {
        when(jwt.extractSubject("test-token")).thenReturn("owner@example.com");
        var owner=mock(User.class);
        when(owner.getId()).thenReturn(1L);
        when(users.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        var headers=new HttpHeaders();
        headers.setBearerAuth("test-token");
        var result=http.exchange("/api/v1/users/2/campaigns/10/start",HttpMethod.POST,
                new HttpEntity<>(headers),String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(campaigns);
    }

    @Test void anonymousStartStillRequiresAuthentication() {
        assertThat(http.postForEntity("/api/v1/users/1/campaigns/10/start",null,String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(campaigns);
    }

    @Test void anonymousDirectErrorUrlIsNotMadePublic() {
        assertThat(http.getForEntity("/error",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
