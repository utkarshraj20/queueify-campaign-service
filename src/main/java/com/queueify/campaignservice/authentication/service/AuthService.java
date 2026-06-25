package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.dto.RegisterRequest;
import com.queueify.campaignservice.authentication.dto.RegisterResponse;
import com.queueify.campaignservice.authentication.entity.User;
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
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public boolean userExist(RegisterRequest registerRequest){
        Optional<User>  user = userRepository.findUserByEmail(registerRequest.getEmail());
        return user.isPresent();
    }

    public RegisterResponse saveUser(RegisterRequest registerRequest){

        if( userExist(registerRequest) ){
            return new RegisterResponse( registerRequest.getName() , "User already exist");
        }

        //sendVerifyEmail(registerRequest.getEmail());

        String passwordHash = passwordEncoder.encode(registerRequest.getPassword());
        User user = new User(registerRequest.getName(), registerRequest.getEmail(), passwordHash, LocalDateTime.now(), LocalDateTime.now());
        System.out.println(user);
        userRepository.save(user);

        return new RegisterResponse( registerRequest.getName() , "User registered successfully");
    }

//    public boolean sendVerifyEmail(String email){
//
//        String generatedOtp = OtpUtil.generateOtp();
//
//        int otpValidityMinutes = 10;
//
//        String body = String.format(
//                "Security Notification\n\n" +
//                        "Your security verification code is: %s\n\n" +
//                        "This code is valid for %d minutes. " +
//                        "For security and identity safety, do not share this security code with any third parties.\n\n" +
//                        "Enterprise Identity Platform Group",
//                generatedOtp,
//                otpValidityMinutes
//        );
//
//        emailService.sendEmail(email, "OTP Verification Email", body) ;
//
//        return true;
//    }

}
