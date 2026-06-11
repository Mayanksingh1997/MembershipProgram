package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.auth.AuthProperties;
import com.firstclub.firstclub.constants.AuthConstants;
import com.firstclub.firstclub.dto.request.LoginRequest;
import com.firstclub.firstclub.dto.response.BaseResponse;
import com.firstclub.firstclub.dto.response.LoginResponse;
import com.firstclub.firstclub.entity.RefreshToken;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.exception.AuthException;
import com.firstclub.firstclub.repository.RefreshTokenRepository;
import com.firstclub.firstclub.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenService jwtTokenService,
            BCryptPasswordEncoder passwordEncoder,
            AuthProperties authProperties) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        String accessToken = jwtTokenService.generateAccessToken(user.getExternalUserId());
        String refreshTokenValue = jwtTokenService.generateRefreshToken();

        refreshTokenRepository.deleteByEmail(user.getEmail());
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshTokenValue)
                .expiresAt(jwtTokenService.getRefreshTokenExpiry())
                .build());

        LoginResponse body = LoginResponse.builder()
                .status("SUCCESS")
                .message("Login successful")
                .userId(user.getExternalUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(accessToken).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(refreshTokenValue).toString())
                .body(body);
    }

    public ResponseEntity<BaseResponse> logout(HttpServletRequest request) {
        String refreshToken = extractCookieValue(request, AuthConstants.REFRESH_TOKEN_COOKIE);
        if (refreshToken != null) {
            Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);
            if (storedToken.isPresent()) {
                refreshTokenRepository.delete(storedToken.get());
            }
        }

        BaseResponse body = BaseResponse.builder()
                .status("SUCCESS")
                .message("Logout successful")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie(AuthConstants.ACCESS_TOKEN_COOKIE).toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie(AuthConstants.REFRESH_TOKEN_COOKIE).toString())
                .body(body);
    }

    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String refreshToken = extractCookieValue(request, AuthConstants.REFRESH_TOKEN_COOKIE);
        if (refreshToken == null) {
            throw new AuthException("Refresh token cookie missing", HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISSING");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new AuthException("Refresh token expired", HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED");
        }

        UserAccount user = userAccountRepository.findByEmail(storedToken.getEmail())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        String accessToken = jwtTokenService.generateAccessToken(user.getExternalUserId());

        LoginResponse body = LoginResponse.builder()
                .status("SUCCESS")
                .message("Token refreshed successfully")
                .userId(user.getExternalUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(accessToken).toString())
                .body(body);
    }

    private ResponseCookie buildAccessTokenCookie(String token) {
        return ResponseCookie.from(AuthConstants.ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(authProperties.getJwt().getAccessTokenTtlMinutes() * 60)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie buildRefreshTokenCookie(String token) {
        return ResponseCookie.from(AuthConstants.REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(authProperties.getJwt().getRefreshTokenTtlDays() * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
