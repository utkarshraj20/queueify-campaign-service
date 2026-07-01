package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.dto.LoginRequest;
import com.queueify.campaignservice.authentication.dto.LoginResponse;
import com.queueify.campaignservice.authentication.dto.RegisterRequest;
import com.queueify.campaignservice.authentication.dto.RegisterResponse;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.exception.InvalidCredentialsException;
import com.queueify.campaignservice.authentication.exception.UserAlreadyExistsException;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        Optional<User> user = userRepository.findByEmail(loginRequest.getEmail());

        if(user.isEmpty()){
            throw new InvalidCredentialsException("Invalid email or password.") ;
        }

        if( !passwordEncoder.matches(loginRequest.getPassword(), user.get().getPasswordHash()) ){
            throw new InvalidCredentialsException("Invalid email or password.") ;
        }
        return new LoginResponse(user.get().getName(), "User logged in successfully.");
    }

}
