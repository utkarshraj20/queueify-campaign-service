package com.queueify.campaignservice.authentication.controller;

import com.queueify.campaignservice.authentication.dto.*;
import com.queueify.campaignservice.authentication.entity.RefreshToken;
import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.service.AuthService;
import com.queueify.campaignservice.authentication.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService ;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        RegisterResponse response = authService.saveUser(registerRequest) ;
        return ResponseEntity.status(HttpStatus.CREATED).body(response) ;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse = authService.loginUser(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshToken refreshToken =
                refreshTokenService.validateRefreshToken(
                        request.getRefreshToken()
                );

        String accessToken =
                jwtService.generateAccessToken(refreshTokenService.getRefreshTokenOwnerEmail(refreshToken));

        return new RefreshTokenResponse(
                accessToken,
                refreshToken.getToken()
        );
    }
}
