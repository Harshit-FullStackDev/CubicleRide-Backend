package com.orangemantra.employeeservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        secret = "01234567890123456789012345678901"; // 32 bytes
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        jwtUtil.afterPropertiesSet();
    }

    @Test
    void extractClaims_roleAndEmpId() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .claim("role", "ADMIN")
                .claim("empId", "E42")
                .signWith(key)
                .compact();

        var claims = jwtUtil.extractAllClaims(token);
        assertEquals("ADMIN", jwtUtil.extractRole(token));
        assertEquals("E42", jwtUtil.extractEmpId(token));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("E42", claims.get("empId", String.class));
    }
}

