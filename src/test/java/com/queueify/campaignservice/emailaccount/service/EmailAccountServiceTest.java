package com.queueify.campaignservice.emailaccount.service;

import com.queueify.campaignservice.emailaccount.dto.CreateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountPageResponse;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountResponse;
import com.queueify.campaignservice.emailaccount.dto.UpdateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import com.queueify.campaignservice.emailaccount.exception.DuplicateEmailAccountException;
import com.queueify.campaignservice.emailaccount.exception.EmailAccountNotFoundException;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailAccountServiceTest {

    @Mock
    private EmailAccountRepository emailAccountRepository;

    @InjectMocks
    private EmailAccountService emailAccountService;

    @Test
    void shouldCreateEmailAccount() {
        CreateEmailAccountRequest request = new CreateEmailAccountRequest(
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                "{\"dailyLimit\":500}"
        );

        when(emailAccountRepository.existsByUserIdAndSenderEmail(1L, "sender@gmail.com")).thenReturn(false);
        when(emailAccountRepository.save(any(EmailAccount.class))).thenAnswer(invocation -> {
            EmailAccount emailAccount = invocation.getArgument(0);
            emailAccount.setId(10L);
            return emailAccount;
        });

        EmailAccountResponse response = emailAccountService.createEmailAccount(1L, request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("sender@gmail.com", response.getSenderEmail());
        assertEquals(EmailProvider.GMAIL, response.getProvider());
        assertEquals(EmailAuthType.APP_PASSWORD, response.getAuthType());
        assertEquals(EmailAccountStatus.ACTIVE, response.getStatus());
        assertEquals("{\"dailyLimit\":500}", response.getMetadata());

        ArgumentCaptor<EmailAccount> emailAccountCaptor = ArgumentCaptor.forClass(EmailAccount.class);
        verify(emailAccountRepository).save(emailAccountCaptor.capture());
        assertNotNull(emailAccountCaptor.getValue().getCreatedAt());
        assertNotNull(emailAccountCaptor.getValue().getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateEmailAccount() {
        CreateEmailAccountRequest request = new CreateEmailAccountRequest(
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                null
        );

        when(emailAccountRepository.existsByUserIdAndSenderEmail(1L, "sender@gmail.com")).thenReturn(true);

        DuplicateEmailAccountException exception = assertThrows(
                DuplicateEmailAccountException.class,
                () -> emailAccountService.createEmailAccount(1L, request)
        );

        assertEquals("Email account already exists for this user.", exception.getMessage());
        verify(emailAccountRepository, never()).save(any());
    }

    @Test
    void shouldListEmailAccountsWithStatusFilter() {
        EmailAccount emailAccount = emailAccount(10L, 1L, "sender@gmail.com", EmailAccountStatus.ACTIVE);

        when(emailAccountRepository.findByUserIdAndStatus(eq(1L), eq(EmailAccountStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(emailAccount)));

        EmailAccountPageResponse response = emailAccountService.listEmailAccounts(1L, EmailAccountStatus.ACTIVE, 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals("sender@gmail.com", response.getContent().getFirst().getSenderEmail());
        assertEquals(0, response.getPage());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void shouldGetEmailAccount() {
        EmailAccount emailAccount = emailAccount(10L, 1L, "sender@gmail.com", EmailAccountStatus.ACTIVE);

        when(emailAccountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(emailAccount));

        EmailAccountResponse response = emailAccountService.getEmailAccount(1L, 10L);

        assertEquals(10L, response.getId());
        assertEquals("sender@gmail.com", response.getSenderEmail());
    }

    @Test
    void shouldThrowExceptionWhenEmailAccountDoesNotExist() {
        when(emailAccountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        EmailAccountNotFoundException exception = assertThrows(
                EmailAccountNotFoundException.class,
                () -> emailAccountService.getEmailAccount(1L, 10L)
        );

        assertEquals("Email account not found.", exception.getMessage());
    }

    @Test
    void shouldUpdateEmailAccount() {
        EmailAccount emailAccount = emailAccount(10L, 1L, "old@gmail.com", EmailAccountStatus.ACTIVE);
        UpdateEmailAccountRequest request = new UpdateEmailAccountRequest(
                "new@gmail.com",
                EmailProvider.OUTLOOK,
                EmailAuthType.OAUTH,
                "new-encrypted-password",
                EmailAccountStatus.INACTIVE,
                "{\"region\":\"us\"}"
        );

        when(emailAccountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(emailAccount));
        when(emailAccountRepository.existsByUserIdAndSenderEmail(1L, "new@gmail.com")).thenReturn(false);
        when(emailAccountRepository.save(any(EmailAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailAccountResponse response = emailAccountService.updateEmailAccount(1L, 10L, request);

        assertEquals("new@gmail.com", response.getSenderEmail());
        assertEquals(EmailProvider.OUTLOOK, response.getProvider());
        assertEquals(EmailAuthType.OAUTH, response.getAuthType());
        assertEquals(EmailAccountStatus.INACTIVE, response.getStatus());
        assertEquals("{\"region\":\"us\"}", response.getMetadata());
    }

    @Test
    void shouldThrowExceptionWhenUpdateWouldCreateDuplicateEmailAccount() {
        EmailAccount emailAccount = emailAccount(10L, 1L, "old@gmail.com", EmailAccountStatus.ACTIVE);
        UpdateEmailAccountRequest request = new UpdateEmailAccountRequest(
                "new@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                EmailAccountStatus.ACTIVE,
                null
        );

        when(emailAccountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(emailAccount));
        when(emailAccountRepository.existsByUserIdAndSenderEmail(1L, "new@gmail.com")).thenReturn(true);

        DuplicateEmailAccountException exception = assertThrows(
                DuplicateEmailAccountException.class,
                () -> emailAccountService.updateEmailAccount(1L, 10L, request)
        );

        assertEquals("Email account already exists for this user.", exception.getMessage());
        verify(emailAccountRepository, never()).save(any());
    }

    @Test
    void shouldDeleteEmailAccount() {
        EmailAccount emailAccount = emailAccount(10L, 1L, "sender@gmail.com", EmailAccountStatus.ACTIVE);

        when(emailAccountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(emailAccount));

        emailAccountService.deleteEmailAccount(1L, 10L);

        verify(emailAccountRepository).delete(emailAccount);
    }

    private EmailAccount emailAccount(Long id, Long userId, String senderEmail, EmailAccountStatus status) {
        LocalDateTime now = LocalDateTime.now();
        EmailAccount emailAccount = new EmailAccount(
                userId,
                senderEmail,
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                status,
                null,
                now,
                now
        );
        emailAccount.setId(id);
        return emailAccount;
    }
}
