package com.firstclub.firstclub.controller;

import com.firstclub.firstclub.constants.AuthConstants;
import com.firstclub.firstclub.dto.request.LoginRequest;
import com.firstclub.firstclub.dto.response.BaseResponse;
import com.firstclub.firstclub.dto.response.LoginResponse;
import com.firstclub.firstclub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuthConstants.AUTH_BASE_URL)
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse> logout(HttpServletRequest httpRequest) {
        return authService.logout(httpRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpRequest) {
        return authService.refresh(httpRequest);
    }
}
