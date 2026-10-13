package com.queueify.campaignservice.storage;

public class InvalidUploadException extends RuntimeException {
    public InvalidUploadException(String message) {
        super(message);
    }
}
