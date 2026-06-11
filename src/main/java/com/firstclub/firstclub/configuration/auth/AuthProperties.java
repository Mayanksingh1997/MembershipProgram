package com.firstclub.firstclub.configuration.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private List<String> publicEndpoints = new ArrayList<>();

    @Data
    public static class Jwt {
        private String signingKey;
        private String issuer;
        private String audience;
        private long accessTokenTtlMinutes;
        private long refreshTokenTtlDays;
    }
}
