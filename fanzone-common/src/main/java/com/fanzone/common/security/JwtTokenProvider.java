package com.fanzone.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiry;
    private final Duration refreshTokenExpiry;

    public JwtTokenProvider(
            @Value("${fanzone.jwt.secret}") String secret,
            @Value("${fanzone.jwt.access-token-expiry:PT1H}") Duration accessTokenExpiry,
            @Value("${fanzone.jwt.refresh-token-expiry:P7D}") Duration refreshTokenExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(UUID userId, String email, String username, UUID favoriteClubId) {
        return generateAccessToken(userId, email, username, favoriteClubId, false);
    }

    public String generateAccessToken(UUID userId, String email, String username, UUID favoriteClubId, boolean emailVerified) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("username", username)
                .claim("favoriteClubId", favoriteClubId != null ? favoriteClubId.toString() : null)
                .claim("emailVerified", emailVerified)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpiry)))
                .signWith(secretKey)
                .compact();
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = extractClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String username = claims.get("username", String.class);
        String clubIdStr = claims.get("favoriteClubId", String.class);
        UUID favoriteClubId = clubIdStr != null ? UUID.fromString(clubIdStr) : null;
        Boolean emailVerified = claims.get("emailVerified", Boolean.class);
        return new UserPrincipal(userId, email, username, favoriteClubId,
                emailVerified != null && emailVerified);
    }

    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
