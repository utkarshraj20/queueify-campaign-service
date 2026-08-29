package com.queueify.campaignservice.emailaccount.exception;

public class EmailAccountNotFoundException extends RuntimeException {
    public EmailAccountNotFoundException(String message) {
        super(message);
    }
}
