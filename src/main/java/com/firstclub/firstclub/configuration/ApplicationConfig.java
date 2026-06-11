package com.firstclub.firstclub.configuration;

import com.firstclub.firstclub.configuration.auth.AuthProperties;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.annotation.Bean;

@Configuration
@EnableConfigurationProperties({MembershipCatalogProperties.class, AuthProperties.class})
public class ApplicationConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
