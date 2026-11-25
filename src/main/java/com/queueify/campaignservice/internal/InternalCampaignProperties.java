package com.queueify.campaignservice.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="queueify.internal.campaign-api")
public record InternalCampaignProperties(boolean enabled,String token) {
    public InternalCampaignProperties {
        if(enabled && (token==null || !token.matches("[A-Za-z0-9_-]{32,256}"))) {
            throw new IllegalArgumentException("Internal campaign API requires a 32-256 character URL-safe token.");
        }
    }
    @Override public String toString() { return "InternalCampaignProperties[enabled="+enabled+",token=REDACTED]"; }
}
