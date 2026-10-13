package com.queueify.campaignservice.storage;

import java.util.Locale;

public enum CampaignAssetType {
    TEMPLATE("templates", 1024L * 1024),
    RECIPIENTS("recipients", 25L * 1024 * 1024);

    private final String directory;
    private final long maxBytes;

    CampaignAssetType(String directory, long maxBytes) {
        this.directory = directory;
        this.maxBytes = maxBytes;
    }

    public String getDirectory() {
        return directory;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public boolean accepts(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return this == RECIPIENTS ? lower.endsWith(".csv")
                : lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".txt");
    }

    public String contentType(String filename) {
        if (this == RECIPIENTS) return "text/csv; charset=UTF-8";
        return filename.toLowerCase(Locale.ROOT).endsWith(".txt")
                ? "text/plain; charset=UTF-8" : "text/html; charset=UTF-8";
    }
}
