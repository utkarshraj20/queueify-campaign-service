package com.queueify.campaignservice.campaign.dto;

import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Campaign name must not exceed 255 characters")
    private String campaignName;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Template URL is required")
    @Size(max = 1024, message = "Template URL must not exceed 1024 characters")
    private String templateUrl;

    @NotBlank(message = "CSV URL is required")
    @Size(max = 1024, message = "CSV URL must not exceed 1024 characters")
    private String csvUrl;

    @NotNull(message = "Total emails is required")
    @Min(value = 1, message = "Total emails must be at least 1")
    private Integer totalEmails;

    @NotNull(message = "Campaign status is required")
    private CampaignStatus status;

    @NotNull(message = "Sender email account ID is required")
    @Min(value = 1, message = "Sender email account ID must be positive")
    private Long senderEmailAccountId;

    private String failureReason;

    private String pausedReason;

    public UpdateCampaignRequest(
            String campaignName,
            String subject,
            String templateUrl,
            String csvUrl,
            Integer totalEmails,
            CampaignStatus status,
            Long senderEmailAccountId,
            String failureReason,
            String pausedReason
    ) {
        this.campaignName = campaignName;
        this.subject = subject;
        this.templateUrl = templateUrl;
        this.csvUrl = csvUrl;
        this.totalEmails = totalEmails;
        this.status = status;
        this.senderEmailAccountId = senderEmailAccountId;
        this.failureReason = failureReason;
        this.pausedReason = pausedReason;
    }
}
