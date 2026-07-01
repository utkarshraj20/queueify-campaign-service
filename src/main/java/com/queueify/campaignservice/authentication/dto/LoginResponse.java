package com.queueify.campaignservice.authentication.dto;

import lombok.Getter;

@Getter
public class LoginResponse {
    private String name;
    private String message;

    public LoginResponse(String name, String message){
        this.name = name ;
        this.message = message;
    }

}
