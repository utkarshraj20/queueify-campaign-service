package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.dto.RegisterRequest;
import com.queueify.campaignservice.authentication.dto.RegisterResponse;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.exception.UserAlreadyExistsException;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import com.queueify.campaignservice.common.util.OtpUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExists(RegisterRequest registerRequest){
       return userRepository.existsByEmail(registerRequest.getEmail());
    }

    public RegisterResponse saveUser(RegisterRequest registerRequest){

        if( emailExists(registerRequest) ){
            throw new UserAlreadyExistsException("A user with email '" + registerRequest.getEmail() + "' already exists." );
        }

        String passwordHash = passwordEncoder.encode(registerRequest.getPassword());
        User user = new User(registerRequest.getName(), registerRequest.getEmail(), passwordHash, LocalDateTime.now(), LocalDateTime.now());
        userRepository.save(user);

        return new RegisterResponse( registerRequest.getName() , "User registered successfully");
    }

}
