package com.queueify.campaignservice.authentication.dto;

public class RegisterResponse {

    public String name;

    public String responseMessage;

    public RegisterResponse(String name, String responseMessage){
        this.name = name;
        this.responseMessage = responseMessage;
    }

    public String getName(){
        return this.name;
    }

    public String getResponseMessage(){
        return this.responseMessage;
    }

}
