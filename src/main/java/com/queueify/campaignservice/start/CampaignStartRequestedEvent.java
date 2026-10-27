package com.queueify.campaignservice.start;

public record CampaignStartRequestedEvent(int schemaVersion, long eventId, long campaignId,
                                          long userId, String recipientsBucket, String recipientsKey) {
    public CampaignStartRequestedEvent {
        if (schemaVersion != 1 || eventId < 1 || campaignId < 1 || userId < 1) {
            throw new IllegalArgumentException("Invalid campaign start identity or version.");
        }
        if (recipientsBucket == null || recipientsBucket.isBlank()
                || recipientsKey == null || recipientsKey.isBlank() || recipientsKey.length() > 1024) {
            throw new IllegalArgumentException("Recipient bucket and object key are required.");
        }
    }

    public String runId() { return "start-" + eventId; }
}
