package com.queueify.campaignservice.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank
    private String password;

    public LoginRequest(String email, String password){
        this.email = email;
        this.password = password;
    }
}
