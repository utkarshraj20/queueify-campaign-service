package com.queueify.campaignservice.storage;


import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class S3KeyBuilder {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final S3Properties s3Properties;
    private final Clock clock;

    public S3KeyBuilder(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
        this.clock = Clock.systemUTC();
    }

    public String campaignInputCsvKey(Long userId, Long campaignId, String originalFilename) {
        String timestamp = LocalDateTime.now(clock).format(TIMESTAMP_FORMATTER);
        String safeFilename = sanitizeFilename(originalFilename);

        return cleanPrefix(s3Properties.getBasePrefix())
                + "/users/" + userId
                + "/campaigns/" + campaignId
                + "/input/" + timestamp + "-" + safeFilename;
    }

    private String sanitizeFilename(String filename){
        if (filename == null || filename.isBlank()) {
            return "upload.csv";
        }

        return filename
                .replace("\\", "/")
                .substring(filename.replace("\\", "/").lastIndexOf("/") + 1)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String cleanPrefix(String prefix) {
        return prefix
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

}
