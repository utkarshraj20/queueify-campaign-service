package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.dto.RegisterRequest;
import com.queueify.campaignservice.authentication.dto.RegisterResponse;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.exception.UserAlreadyExistsException;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void testExistingUserRegistration(){
        RegisterRequest registerRequest = new RegisterRequest( "Utkarsh" , "utkarsh@gmail.com" , "12343" ) ;

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        UserAlreadyExistsException userAlreadyExistsException = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.saveUser(registerRequest)
        );

        assertEquals( "A user with email 'utkarsh@gmail.com' already exists." , userAlreadyExistsException.getMessage()) ;

    }

    @Test
    void registerUserSuccessfully(){
        RegisterRequest registerRequest = new RegisterRequest( "Utkarsh" , "utkarsh@gmail.com" , "12343" ) ;

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("asnjadklfs");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse expectedResponse = new RegisterResponse("Utkarsh" , "User registered successfully") ;

        RegisterResponse testResponse = authService.saveUser(registerRequest);

        assertEquals(expectedResponse.getName(), testResponse.getName());
        assertEquals(expectedResponse.getResponseMessage(), testResponse.getResponseMessage());

    }

}
