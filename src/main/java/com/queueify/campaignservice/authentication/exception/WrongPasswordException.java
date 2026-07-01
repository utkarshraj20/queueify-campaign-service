package com.queueify.campaignservice.authentication.exception;

public class WrongPasswordException extends RuntimeException{
    public WrongPasswordException(String message){
        super(message);
    }
}
