package com.orangemantra.rideservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtParser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Optimized JWT utility: builds parser & key once (bean lifecycle) to avoid repeated
 * key reconstruction and parser building per request. Reduces per-request overhead.
 */
@Component
public class JwtUtil implements InitializingBean {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;
    private JwtParser parser;

    @Override
    public void afterPropertiesSet() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    public Claims extractAllClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public String extractEmpId(String token) { return extractAllClaims(token).get("empId", String.class); }
}
