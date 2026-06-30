package com.queueify.campaignservice.authentication.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterResponse {

    private String name;
    private String responseMessage;

    public RegisterResponse(String name, String responseMessage){
        this.name = name;
        this.responseMessage = responseMessage;
    }

}
