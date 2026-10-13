package com.queueify.campaignservice.authentication.controller;

import com.queueify.campaignservice.authentication.dto.CurrentUserResponse;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CurrentUserController {
    private final UserRepository userRepository;

    public CurrentUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/v1/me")
    public CurrentUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
