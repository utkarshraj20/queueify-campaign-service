package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.dto.*;
import com.queueify.campaignservice.authentication.entity.RefreshToken;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.exception.InvalidCredentialsException;
import com.queueify.campaignservice.authentication.exception.UserAlreadyExistsException;
import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.repository.RefreshTokenRepository;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long expiryTime;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public boolean emailExists(String email){
       return userRepository.existsByEmail(email);
    }

    public RegisterResponse saveUser(RegisterRequest registerRequest){

        if( emailExists(registerRequest.getEmail()) ){
            throw new UserAlreadyExistsException("A user with email '" + registerRequest.getEmail() + "' already exists." );
        }

        String passwordHash = passwordEncoder.encode(registerRequest.getPassword());
        User user = new User(registerRequest.getName(), registerRequest.getEmail(), passwordHash, LocalDateTime.now(), LocalDateTime.now());

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException(
                    "A user with email '" + registerRequest.getEmail() + "' already exists."
            );
        }

        return new RegisterResponse( registerRequest.getName() , "User registered successfully");
    }

    public LoginResponse loginUser(LoginRequest loginRequest){

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password."));

        if( !passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash()) ){
            throw new InvalidCredentialsException("Invalid email or password.") ;
        }

        String accessedToken = jwtService.generateAccessToken(loginRequest.getEmail());
        String refreshedToken = jwtService.generateRefreshToken(loginRequest.getEmail());

        RefreshToken refreshToken = new RefreshToken(user.getId(), refreshedToken, false, LocalDateTime.now(), LocalDateTime.now().plus(expiryTime, ChronoUnit.MILLIS));

        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(user.getName(), "User logged in successfully." , accessedToken , refreshedToken);
    }

}
