package com.queueify.campaignservice.storage;

import lombok.Getter;

@Getter
public class StoredFile {
    private final String bucket;
    private final String key;
    private final String uri;
    private final String contentType;
    private final long size;

    public StoredFile(String bucket, String key, String contentType, long size) {
        this.bucket = bucket;
        this.key = key;
        this.uri = "s3://" + bucket + "/" + key;
        this.contentType = contentType;
        this.size = size;
    }
}