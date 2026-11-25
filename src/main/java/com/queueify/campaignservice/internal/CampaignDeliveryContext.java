package com.queueify.campaignservice.internal;

/** Internal campaign metadata; deliberately excludes provider credentials. */
public record CampaignDeliveryContext(long campaignId,long userId,String subject,String templateUri,
                                       long senderEmailAccountId) {}
