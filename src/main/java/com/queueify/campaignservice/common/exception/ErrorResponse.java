package com.queueify.campaignservice.common.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
    private String path;
    private String error;


    public ErrorResponse(int status, String error, String message, String path, long timestamp){
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.error = error;
        this.path = path;
    }

}
