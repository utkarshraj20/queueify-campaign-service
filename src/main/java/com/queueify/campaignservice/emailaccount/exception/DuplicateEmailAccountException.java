package com.queueify.campaignservice.emailaccount.exception;

public class DuplicateEmailAccountException extends RuntimeException {
    public DuplicateEmailAccountException(String message) {
        super(message);
    }
}
