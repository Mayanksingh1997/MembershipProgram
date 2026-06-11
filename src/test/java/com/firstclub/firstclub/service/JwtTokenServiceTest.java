package com.firstclub.firstclub.service;

import com.firstclub.firstclub.exception.AuthException;
import com.firstclub.firstclub.service.support.ServiceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(ServiceTestFixtures.authProperties());
    }

    @Test
    void generateAccessToken_extractUserId_roundTrip() {
        String token = jwtTokenService.generateAccessToken("user-001");

        assertThat(jwtTokenService.extractUserId(token)).isEqualTo("user-001");
    }

    @Test
    void generateRefreshToken_returnsUniqueValues() {
        String first = jwtTokenService.generateRefreshToken();
        String second = jwtTokenService.generateRefreshToken();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void getRefreshTokenExpiry_isInFuture() {
        assertThat(jwtTokenService.getRefreshTokenExpiry()).isAfter(java.time.Instant.now());
    }

    @Test
    void extractUserId_throwsOnInvalidToken() {
        assertThatThrownBy(() -> jwtTokenService.extractUserId("not-a-valid-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid access token");
    }
}
