package com.queueify.campaignservice.emailaccount.controller;

import com.queueify.campaignservice.emailaccount.dto.CreateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountPageResponse;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountResponse;
import com.queueify.campaignservice.emailaccount.dto.UpdateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import com.queueify.campaignservice.emailaccount.service.EmailAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class EmailAccountControllerTest {

    private final EmailAccountService emailAccountService = mock(EmailAccountService.class);
    private final EmailAccountController emailAccountController = new EmailAccountController(emailAccountService);

    @Test
    void shouldCreateEmailAccount() {
        CreateEmailAccountRequest request = new CreateEmailAccountRequest(
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                null
        );
        EmailAccountResponse emailAccountResponse = emailAccountResponse();

        when(emailAccountService.createEmailAccount(1L, request)).thenReturn(emailAccountResponse);

        ResponseEntity<EmailAccountResponse> response = emailAccountController.createEmailAccount(1L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(emailAccountResponse, response.getBody());
    }

    @Test
    void shouldListEmailAccounts() {
        EmailAccountPageResponse pageResponse = new EmailAccountPageResponse(
                List.of(emailAccountResponse()),
                0,
                20,
                1,
                1,
                true
        );

        when(emailAccountService.listEmailAccounts(1L, EmailAccountStatus.ACTIVE, 0, 20)).thenReturn(pageResponse);

        ResponseEntity<EmailAccountPageResponse> response =
                emailAccountController.listEmailAccounts(1L, EmailAccountStatus.ACTIVE, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pageResponse, response.getBody());
    }

    @Test
    void shouldGetEmailAccount() {
        EmailAccountResponse emailAccountResponse = emailAccountResponse();

        when(emailAccountService.getEmailAccount(1L, 10L)).thenReturn(emailAccountResponse);

        ResponseEntity<EmailAccountResponse> response = emailAccountController.getEmailAccount(1L, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(emailAccountResponse, response.getBody());
    }

    @Test
    void shouldUpdateEmailAccount() {
        UpdateEmailAccountRequest request = new UpdateEmailAccountRequest(
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                EmailAccountStatus.ACTIVE,
                null
        );
        EmailAccountResponse emailAccountResponse = emailAccountResponse();

        when(emailAccountService.updateEmailAccount(1L, 10L, request)).thenReturn(emailAccountResponse);

        ResponseEntity<EmailAccountResponse> response = emailAccountController.updateEmailAccount(1L, 10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(emailAccountResponse, response.getBody());
    }

    @Test
    void shouldDeleteEmailAccount() {
        ResponseEntity<Void> response = emailAccountController.deleteEmailAccount(1L, 10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(emailAccountService).deleteEmailAccount(1L, 10L);
    }

    private EmailAccountResponse emailAccountResponse() {
        LocalDateTime now = LocalDateTime.now();

        return new EmailAccountResponse(
                10L,
                1L,
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                EmailAccountStatus.ACTIVE,
                null,
                now,
                now
        );
    }
}
