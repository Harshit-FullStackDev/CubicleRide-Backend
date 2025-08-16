package com.orangemantra.adminservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

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

    public Claims extractAllClaims(String token) { return parser.parseSignedClaims(token).getPayload(); }
    public String extractRole(String token) { return extractAllClaims(token).get("role", String.class); }
}
