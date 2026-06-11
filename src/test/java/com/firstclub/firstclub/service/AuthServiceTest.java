package com.firstclub.firstclub.service;

import com.firstclub.firstclub.constants.AuthConstants;
import com.firstclub.firstclub.dto.request.LoginRequest;
import com.firstclub.firstclub.dto.response.BaseResponse;
import com.firstclub.firstclub.dto.response.LoginResponse;
import com.firstclub.firstclub.entity.RefreshToken;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.exception.AuthException;
import com.firstclub.firstclub.repository.RefreshTokenRepository;
import com.firstclub.firstclub.repository.UserAccountRepository;
import com.firstclub.firstclub.service.support.ServiceTestFixtures;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userAccountRepository,
                refreshTokenRepository,
                jwtTokenService,
                passwordEncoder,
                ServiceTestFixtures.authProperties());
    }

    @Test
    void login_success_setsCookiesAndReturnsUser() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        user.setEmail("user001@firstclub.co.in");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        when(userAccountRepository.findByEmail("user001@firstclub.co.in")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken("user-001")).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));

        LoginRequest request = LoginRequest.builder()
                .email("user001@firstclub.co.in")
                .password("password123")
                .build();

        ResponseEntity<LoginResponse> response = authService.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUserId()).isEqualTo("user-001");
        assertThat(response.getHeaders().get("Set-Cookie")).hasSize(2);
        verify(refreshTokenRepository).deleteByEmail("user001@firstclub.co.in");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_throwsWhenEmailNotFound() {
        when(userAccountRepository.findByEmail("missing@firstclub.co.in")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .email("missing@firstclub.co.in")
                .password("password123")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throwsWhenPasswordInvalid() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        user.setEmail("user001@firstclub.co.in");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        when(userAccountRepository.findByEmail("user001@firstclub.co.in")).thenReturn(Optional.of(user));

        LoginRequest request = LoginRequest.builder()
                .email("user001@firstclub.co.in")
                .password("wrong-password")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void logout_deletesRefreshTokenAndClearsCookies() {
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(AuthConstants.REFRESH_TOKEN_COOKIE, "refresh-token")
        });
        RefreshToken stored = RefreshToken.builder()
                .email("user001@firstclub.co.in")
                .token("refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(stored));

        ResponseEntity<BaseResponse> response = authService.logout(httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Logout successful");
        verify(refreshTokenRepository).delete(stored);
        assertThat(response.getHeaders().get("Set-Cookie")).hasSize(2);
    }

    @Test
    void refresh_success_returnsNewAccessTokenCookie() {
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(AuthConstants.REFRESH_TOKEN_COOKIE, "refresh-token")
        });
        RefreshToken stored = RefreshToken.builder()
                .email("user001@firstclub.co.in")
                .token("refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        UserAccount user = ServiceTestFixtures.user("user-001");
        user.setEmail("user001@firstclub.co.in");
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(stored));
        when(userAccountRepository.findByEmail("user001@firstclub.co.in")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken("user-001")).thenReturn("new-access-token");

        ResponseEntity<LoginResponse> response = authService.refresh(httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Token refreshed successfully");
        assertThat(response.getHeaders().get("Set-Cookie")).hasSize(1);
    }

    @Test
    void refresh_throwsWhenCookieMissing() {
        when(httpServletRequest.getCookies()).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Refresh token cookie missing");
    }

    @Test
    void refresh_throwsWhenTokenExpired() {
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(AuthConstants.REFRESH_TOKEN_COOKIE, "expired-token")
        });
        RefreshToken stored = RefreshToken.builder()
                .email("user001@firstclub.co.in")
                .token("expired-token")
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Refresh token expired");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).delete(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("expired-token");
        verify(userAccountRepository, never()).findByEmail(any());
    }
}
