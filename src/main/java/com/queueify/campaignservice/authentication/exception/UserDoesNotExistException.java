package com.queueify.campaignservice.authentication.exception;

public class UserDoesNotExistException extends RuntimeException {
    public UserDoesNotExistException( String message){
        super(message);
    }
}
