package com.queueify.campaignservice.authentication.controller;

import com.queueify.campaignservice.authentication.dto.RegisterRequest;
import com.queueify.campaignservice.authentication.dto.RegisterResponse;
import com.queueify.campaignservice.authentication.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService ;

    public AuthController(AuthService authService) {
        System.out.println("Reached");
        this.authService = authService;
    }

    @PostMapping("/register")
    public RegisterResponse authUser( @Valid @RequestBody RegisterRequest registerRequest){
        System.out.println(registerRequest);
        return authService.saveUser(registerRequest) ;
    }
}
