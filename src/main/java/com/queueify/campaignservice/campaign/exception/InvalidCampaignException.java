package com.queueify.campaignservice.campaign.exception;

public class InvalidCampaignException extends RuntimeException {
    public InvalidCampaignException(String message) {
        super(message);
    }
}
