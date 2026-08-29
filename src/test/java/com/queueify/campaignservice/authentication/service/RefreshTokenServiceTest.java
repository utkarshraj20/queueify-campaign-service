package com.queueify.campaignservice.authentication.service;

import com.queueify.campaignservice.authentication.entity.RefreshToken;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.exception.InvalidRefreshTokenException;
import com.queueify.campaignservice.authentication.repository.RefreshTokenRepository;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void shouldValidateRefreshTokenSuccessfully() {
        RefreshToken refreshToken = new RefreshToken(1L, "refresh-token", false, LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        RefreshToken response = refreshTokenService.validateRefreshToken("refresh-token");

        assertSame(refreshToken, response);
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.validateRefreshToken("missing-token")
        );

        assertEquals("Refresh token not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsRevoked() {
        RefreshToken refreshToken = new RefreshToken(1L, "refresh-token", true, LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.validateRefreshToken("refresh-token")
        );

        assertEquals("Refresh token has been revoked", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsExpired() {
        RefreshToken refreshToken = new RefreshToken(1L, "refresh-token", false, LocalDateTime.now(), LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.validateRefreshToken("refresh-token")
        );

        assertEquals("Refresh token has expired", exception.getMessage());
    }

    @Test
    void shouldReturnRefreshTokenOwnerEmail() {
        RefreshToken refreshToken = new RefreshToken(1L, "refresh-token", false, LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        User user = new User("Utkarsh", "utkarsh@gmail.com", "password-hash", LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String email = refreshTokenService.getRefreshTokenOwnerEmail(refreshToken);

        assertEquals("utkarsh@gmail.com", email);
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenOwnerDoesNotExist() {
        RefreshToken refreshToken = new RefreshToken(1L, "refresh-token", false, LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.getRefreshTokenOwnerEmail(refreshToken)
        );

        assertEquals("Refresh token owner not found", exception.getMessage());
    }
}
