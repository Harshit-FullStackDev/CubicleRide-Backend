package com.orangemantra.authservice.config;

import com.orangemantra.authservice.util.CryptoUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {
    @Value("${jwt.secret:}")
    private String secret;

    @PostConstruct
    public void init() {
        CryptoUtils.init(secret);
    }
}

