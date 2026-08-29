package com.queueify.campaignservice.emailaccount.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class EmailAccountPageResponse {

    private final List<EmailAccountResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public EmailAccountPageResponse(
            List<EmailAccountResponse> content,
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
