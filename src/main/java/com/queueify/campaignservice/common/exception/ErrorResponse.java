package com.queueify.campaignservice.common.exception;


import com.queueify.campaignservice.common.dto.ValidationError;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
    private String path;
    private String error;
    private List<ValidationError> errors;


    public ErrorResponse(int status, List<ValidationError> errors, String error, String message, String path, long timestamp){
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
        this.path = path;
        this.error = error;
    }

}
