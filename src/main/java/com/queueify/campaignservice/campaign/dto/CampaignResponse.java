package com.queueify.campaignservice.campaign.dto;

import com.queueify.campaignservice.campaign.entity.Campaign;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CampaignResponse {

    private final Long id;
    private final String campaignName;
    private final Long userId;
    private final String subject;
    private final String templateUrl;
    private final String csvUrl;
    private final Integer totalEmails;
    private final Integer emailsSent;
    private final Integer emailsFailed;
    private final CampaignStatus status;
    private final Long senderEmailAccountId;
    private final String failureReason;
    private final String pausedReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CampaignResponse(
            Long id,
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
        this.id = id;
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

    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getCampaignName(),
                campaign.getUserId(),
                campaign.getSubject(),
                campaign.getTemplateUrl(),
                campaign.getCsvUrl(),
                campaign.getTotalEmails(),
                campaign.getEmailsSent(),
                campaign.getEmailsFailed(),
                campaign.getStatus(),
                campaign.getSenderEmailAccountId(),
                campaign.getFailureReason(),
                campaign.getPausedReason(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
