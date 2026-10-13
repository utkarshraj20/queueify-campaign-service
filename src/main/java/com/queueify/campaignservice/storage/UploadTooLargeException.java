package com.queueify.campaignservice.storage;

public class UploadTooLargeException extends RuntimeException {
    public UploadTooLargeException(String message) {
        super(message);
    }
}
