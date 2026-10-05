package com.queueify.campaignservice.campaign.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CampaignPageResponse {

    private final List<CampaignResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public CampaignPageResponse(
            List<CampaignResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }
}
