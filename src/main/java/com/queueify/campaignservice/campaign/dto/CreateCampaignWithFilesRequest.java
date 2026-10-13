package com.queueify.campaignservice.campaign.dto;

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
public class CreateCampaignWithFilesRequest {
    @NotBlank(message = "Campaign name is required")
    @Size(max = 255)
    private String campaignName;

    @NotBlank(message = "Subject is required")
    @Size(max = 255)
    private String subject;

    @NotNull(message = "Total emails is required")
    @Min(1)
    private Integer totalEmails;

    @NotNull(message = "Sender email account ID is required")
    @Min(1)
    private Long senderEmailAccountId;
}
