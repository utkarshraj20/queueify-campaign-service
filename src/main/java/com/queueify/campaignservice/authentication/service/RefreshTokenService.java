package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.entity.RefreshToken;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.exception.InvalidRefreshTokenException;
import com.queueify.campaignservice.authentication.repository.RefreshTokenRepository;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new InvalidRefreshTokenException("Refresh token not found"));

        if (refreshToken.getRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        return refreshToken;
    }

    public String getRefreshTokenOwnerEmail(RefreshToken refreshToken) {
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token owner not found"));

        return user.getEmail();
    }
}
