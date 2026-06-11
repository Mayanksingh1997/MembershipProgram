package com.firstclub.firstclub.interceptor;

import com.firstclub.firstclub.configuration.auth.AuthProperties;
import com.firstclub.firstclub.constants.AuthConstants;
import com.firstclub.firstclub.exception.AuthException;
import com.firstclub.firstclub.service.JwtTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;

    public JwtAuthInterceptor(JwtTokenService jwtTokenService, AuthProperties authProperties) {
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (isPublicEndpoint(request.getRequestURI())) {
            return true;
        }

        String accessToken = extractAccessToken(request);
        if (accessToken == null) {
            throw new AuthException("Access token cookie missing", HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_MISSING");
        }

        String userId = jwtTokenService.extractUserId(accessToken);
        request.setAttribute(AuthConstants.USER_ID_ATTRIBUTE, userId);
        return true;
    }

    private boolean isPublicEndpoint(String requestUri) {
        for (String publicEndpoint : authProperties.getPublicEndpoints()) {
            if (requestUri.startsWith(publicEndpoint)) {
                return true;
            }
        }
        return false;
    }

    private String extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthConstants.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
