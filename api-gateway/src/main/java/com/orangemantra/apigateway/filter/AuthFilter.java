//This code defines a global authentication filter for a Spring Cloud Gateway API
//Gateway. Here’s what it does:
//Class Setup:
//Annotated with @Component and @Slf4j, making it a Spring bean with logging support.
// Implements GlobalFilter and Ordered to intercept all requests and set filter order.
//JWT Secret:
//Injects the JWT secret from application properties.
//Filter Logic:
//Allows requests to /auth endpoints without authentication.
//For other requests, checks for an Authorization header starting with Bearer .
//If missing or invalid, responds with HTTP 401 Unauthorized.
// If present, extracts and parses the JWT using the secret. If parsing fails, logs the error and returns 401.
// If valid, allows the request to proceed.
//Order:
//Returns -1 to set high precedence for this filter.
//This filter ensures only requests with valid JWT tokens (except /auth) are allowed through
//the gateway.
package com.orangemantra.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getURI().getPath().contains("/auth")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser()
                    .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (Exception ex) {
            log.error("Invalid JWT: {}", ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
