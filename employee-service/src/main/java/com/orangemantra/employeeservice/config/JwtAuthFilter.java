//This class is a Spring Security filter that authenticates requests using JWT tokens.
//How it works:
//It extends OncePerRequestFilter, so it runs once per request.
//It checks for an Authorization header starting with Bearer .
//If present, it extracts the JWT, parses claims using JwtUtil, and retrieves the user's role.
//It creates a UsernamePasswordAuthenticationToken with the user's role and sets it in the
// SecurityContextHolder, marking the request as authenticated.
//If the token is invalid, it returns HTTP 401 Unauthorized.
//If no token is present, the request proceeds unauthenticated.
//Purpose:
//This filter enables JWT-based authentication for your service endpoints.
package com.orangemantra.employeeservice.config;

import com.orangemantra.employeeservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                String role = claims.get("role", String.class);
                String empId = claims.get("empId", String.class); // Add this line

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                empId, // Use empId as principal
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("JWT authenticated successfully: principal={}, role={}, authorities={}, path={}", empId, role, authToken.getAuthorities(), request.getRequestURI());

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                log.error("JWT parse/validation failed: {} path={} token={}", e.getMessage(), request.getRequestURI(), token.substring(0, Math.min(token.length(), 50)) + "...");
                return;
            }
        }
        else {
            log.info("No Authorization header found for path: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
