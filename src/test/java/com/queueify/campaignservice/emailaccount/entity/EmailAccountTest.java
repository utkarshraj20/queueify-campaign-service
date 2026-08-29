package com.queueify.campaignservice.emailaccount.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailAccountTest {

    @Test
    void shouldCreateEmailAccount() {
        LocalDateTime now = LocalDateTime.now();

        EmailAccount emailAccount = new EmailAccount(
                1L,
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                EmailAccountStatus.ACTIVE,
                "{\"dailyLimit\":500}",
                now,
                now
        );

        assertEquals(1L, emailAccount.getUserId());
        assertEquals("sender@gmail.com", emailAccount.getSenderEmail());
        assertEquals(EmailProvider.GMAIL, emailAccount.getProvider());
        assertEquals(EmailAuthType.APP_PASSWORD, emailAccount.getAuthType());
        assertEquals("encrypted-password", emailAccount.getEncryptedAppPassword());
        assertEquals(EmailAccountStatus.ACTIVE, emailAccount.getStatus());
        assertEquals("{\"dailyLimit\":500}", emailAccount.getMetadata());
        assertEquals(now, emailAccount.getCreatedAt());
        assertEquals(now, emailAccount.getUpdatedAt());
    }
}
