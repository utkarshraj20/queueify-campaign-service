package com.queueify.campaignservice.authentication.dto;

import lombok.Getter;

@Getter
public class LoginResponse {
    private final String name;
    private final String message;
    private final String accessToken;
    private final String refreshToken;

    public LoginResponse(String name, String message, String accessToken, String refreshToken){
        this.name = name ;
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

}
