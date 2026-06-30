package com.queueify.campaignservice.authentication.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 20, message = "Name must be at least 1 character and at most 20 characters long")
    private String name ;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email ;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    public RegisterRequest(String name, String email, String password){
        this.name = name ;
        this.email = email;
        this.password = password;
    }
}

