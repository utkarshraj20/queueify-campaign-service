package com.queueify.campaignservice.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(InternalCampaignProperties.class)
public class InternalCampaignSecurity {
    @Bean @Order(1)
    SecurityFilterChain internalCampaignChain(HttpSecurity http,InternalCampaignProperties properties) throws Exception {
        http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors->errors.authenticationEntryPoint((request,response,error)->response.setStatus(401))
                        .accessDeniedHandler((request,response,error)->response.setStatus(403)));
        if(properties.enabled()) {
            http.addFilterBefore(new TokenFilter(properties.token()),UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(requests->requests
                            .requestMatchers(HttpMethod.GET,"/internal/v1/campaigns/*/delivery-context").hasAuthority("INTERNAL_EMAIL")
                            .anyRequest().denyAll());
        } else {
            http.authorizeHttpRequests(requests->requests.anyRequest().denyAll());
        }
        return http.build();
    }
    private static class TokenFilter extends OncePerRequestFilter {
        private final byte[] expected;
        TokenFilter(String token) { expected=("Bearer "+token).getBytes(StandardCharsets.UTF_8); }
        @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
                throws IOException,ServletException {
            String header=request.getHeader("Authorization");
            if(header==null || !MessageDigest.isEqual(expected,header.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(401); return;
            }
            var context=SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken("queueify-email-service",null,
                    List.of(new SimpleGrantedAuthority("INTERNAL_EMAIL"))));
            SecurityContextHolder.setContext(context);
            chain.doFilter(request,response);
        }
    }
}
