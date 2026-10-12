package com.queueify.campaignservice.storage;

public class StorageException extends RuntimeException{
    public StorageException( String message, Throwable cause){
        super(message, cause);
    }
}