package com.queueify.campaignservice.authentication.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.access-token-expiration}")
    private long accessExpiryTime ;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiryTime;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email) {
        return generateToken(new HashMap<>(), email , accessExpiryTime);
    }

    public String generateRefreshToken(String email){
        return generateToken(new HashMap<>(), email , refreshExpiryTime );
    }

    public String extractSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    private String generateToken(Map<String, Object> extraClaims, String email, long expirationTime) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }
}
