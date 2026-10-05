package com.queueify.campaignservice.campaign.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CampaignTest {

    @Test
    void shouldCreateCampaign() {
        LocalDateTime now = LocalDateTime.now();

        Campaign campaign = new Campaign(
                "October launch",
                1L,
                "Hello from Queueify",
                "s3://templates/october.html",
                "s3://csv/october.csv",
                100,
                0,
                0,
                CampaignStatus.DRAFT,
                5L,
                null,
                null,
                now,
                now
        );

        assertEquals("October launch", campaign.getCampaignName());
        assertEquals(1L, campaign.getUserId());
        assertEquals("Hello from Queueify", campaign.getSubject());
        assertEquals("s3://templates/october.html", campaign.getTemplateUrl());
        assertEquals("s3://csv/october.csv", campaign.getCsvUrl());
        assertEquals(100, campaign.getTotalEmails());
        assertEquals(0, campaign.getEmailsSent());
        assertEquals(0, campaign.getEmailsFailed());
        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        assertEquals(5L, campaign.getSenderEmailAccountId());
        assertEquals(now, campaign.getCreatedAt());
        assertEquals(now, campaign.getUpdatedAt());
    }
}
