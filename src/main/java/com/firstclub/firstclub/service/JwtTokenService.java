package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.auth.AuthProperties;
import com.firstclub.firstclub.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String USER_ID_CLAIM = "user_id";

    private final AuthProperties authProperties;
    private final SecretKey secretKey;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(authProperties.getJwt().getSigningKey()));
    }

    public String generateAccessToken(String userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(authProperties.getJwt().getAccessTokenTtlMinutes() * 60);

        return Jwts.builder()
                .claim(USER_ID_CLAIM, userId)
                .issuer(authProperties.getJwt().getIssuer())
                .audience().add(authProperties.getJwt().getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Instant getRefreshTokenExpiry() {
        return Instant.now().plusSeconds(authProperties.getJwt().getRefreshTokenTtlDays() * 24 * 60 * 60);
    }

    public String extractUserId(String token) {
        Claims claims = parseClaims(token);
        Object userId = claims.get(USER_ID_CLAIM);
        if (userId == null) {
            throw new AuthException("Missing user_id claim", HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }
        return userId.toString();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(authProperties.getJwt().getIssuer())
                    .requireAudience(authProperties.getJwt().getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new AuthException("Access token expired", HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
        } catch (Exception ex) {
            throw new AuthException("Invalid access token", HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }
    }
}
