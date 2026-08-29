package com.queueify.campaignservice.emailaccount.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "email_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_email_accounts_user_sender_email",
                        columnNames = {"user_id", "sender_email"}
                )
        },
        indexes = {
                @Index(name = "idx_email_accounts_user_id", columnList = "user_id"),
                @Index(name = "idx_email_accounts_user_status", columnList = "user_id,status")
        }
)
public class EmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EmailProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 32)
    private EmailAuthType authType;

    @Column(name = "encrypted_app_password", nullable = false)
    private String encryptedAppPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EmailAccountStatus status;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public EmailAccount(
            Long userId,
            String senderEmail,
            EmailProvider provider,
            EmailAuthType authType,
            String encryptedAppPassword,
            EmailAccountStatus status,
            String metadata,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.userId = userId;
        this.senderEmail = senderEmail;
        this.provider = provider;
        this.authType = authType;
        this.encryptedAppPassword = encryptedAppPassword;
        this.status = status;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
