package com.queueify.campaignservice.authentication.dto;

import lombok.Getter;

@Getter
public class RegisterResponse {

    public String name;
    public String responseMessage;

    public RegisterResponse(String name, String responseMessage){
        this.name = name;
        this.responseMessage = responseMessage;
    }

}
