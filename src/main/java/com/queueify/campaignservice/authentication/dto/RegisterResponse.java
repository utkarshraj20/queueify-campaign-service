package com.queueify.campaignservice.authentication.dto;

public class RegisterResponse {

    public String name;

    public String responseMessage;

    public String responseCode ;

    public RegisterResponse(String name, String responseMessage, String responseCode){
        this.name = name;
        this.responseMessage = responseMessage;
        this.responseCode = responseCode;
    }

    public String getName(){
        return this.name;
    }

    public String getResponseMessage(){
        return this.responseMessage;
    }

    public String getResponseCode(){
        return this.responseCode;
    }

}
