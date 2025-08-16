//This class is a utility for generating JWT tokens for authenticated users.
//Explanation:
//Annotated with @Component so it can be injected as a Spring bean.
//Reads the JWT secret and expiration time from application properties.
//The generateToken(User user) method creates a JWT:
//Sets the subject as the user's email.
//Adds custom claims: role, empId, and name.
//Sets issued and expiration dates.
//Signs the token using the HS256 algorithm and the secret key.
package com.orangemantra.authservice.util;

import com.orangemantra.authservice.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil implements InitializingBean {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @Override
    public void afterPropertiesSet() { this.key = Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        Date issued = new Date(now);
        Date exp = new Date(now + expiration);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("empId", user.getEmpId())
                .claim("name", user.getName())
                .issuedAt(issued)
                .expiration(exp)
                .signWith(key)
                .compact();
    }
}
