package com.queueify.campaignservice.authentication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt ;

    @Column(name = "revoked", nullable = false)
    private Boolean revoked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt ;

    public RefreshToken(long user_id, String token, Boolean revoked,LocalDateTime createdAt, LocalDateTime expiredAt){
        this.userId = user_id;
        this.token = token;
        this.revoked = revoked;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
    }
}
