package com.queueify.campaignservice.campaign.entity;

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
        name = "campaigns",
        indexes = {
                @Index(name = "idx_campaigns_user_id", columnList = "user_id"),
                @Index(name = "idx_campaigns_user_status", columnList = "user_id,status"),
                @Index(name = "idx_campaigns_sender_email_account_id", columnList = "sender_email_account_id")
        }
)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String subject;

    @Column(name = "template_url", nullable = false, length = 1024)
    private String templateUrl;

    @Column(name = "csv_url", nullable = false, length = 1024)
    private String csvUrl;

    @Column(name = "total_emails", nullable = false)
    private Integer totalEmails;

    @Column(name = "emails_sent", nullable = false)
    private Integer emailsSent;

    @Column(name = "emails_failed", nullable = false)
    private Integer emailsFailed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CampaignStatus status;

    @Column(name = "sender_email_account_id", nullable = false)
    private Long senderEmailAccountId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "paused_reason")
    private String pausedReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Campaign(
            String campaignName,
            Long userId,
            String subject,
            String templateUrl,
            String csvUrl,
            Integer totalEmails,
            Integer emailsSent,
            Integer emailsFailed,
            CampaignStatus status,
            Long senderEmailAccountId,
            String failureReason,
            String pausedReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.campaignName = campaignName;
        this.userId = userId;
        this.subject = subject;
        this.templateUrl = templateUrl;
        this.csvUrl = csvUrl;
        this.totalEmails = totalEmails;
        this.emailsSent = emailsSent;
        this.emailsFailed = emailsFailed;
        this.status = status;
        this.senderEmailAccountId = senderEmailAccountId;
        this.failureReason = failureReason;
        this.pausedReason = pausedReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
