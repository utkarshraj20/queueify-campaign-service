package com.queueify.campaignservice.emailaccount.dto;

import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EmailAccountResponse {

    private final Long id;
    private final Long userId;
    private final String senderEmail;
    private final EmailProvider provider;
    private final EmailAuthType authType;
    private final EmailAccountStatus status;
    private final String metadata;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public EmailAccountResponse(
            Long id,
            Long userId,
            String senderEmail,
            EmailProvider provider,
            EmailAuthType authType,
            EmailAccountStatus status,
            String metadata,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.senderEmail = senderEmail;
        this.provider = provider;
        this.authType = authType;
        this.status = status;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmailAccountResponse from(EmailAccount emailAccount) {
        return new EmailAccountResponse(
                emailAccount.getId(),
                emailAccount.getUserId(),
                emailAccount.getSenderEmail(),
                emailAccount.getProvider(),
                emailAccount.getAuthType(),
                emailAccount.getStatus(),
                emailAccount.getMetadata(),
                emailAccount.getCreatedAt(),
                emailAccount.getUpdatedAt()
        );
    }
}
