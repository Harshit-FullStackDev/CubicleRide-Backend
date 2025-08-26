package com.orangemantra.rideservice.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.orangemantra.rideservice.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authenticateFromToken(request, authHeader.substring(7));
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null && log.isDebugEnabled()) {
            log.debug("No authentication established for path {} (auth header present? {})", request.getRequestURI(), authHeader != null);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromToken(HttpServletRequest request, String jwt) {
        try {
            Claims claims = jwtUtil.extractAllClaims(jwt);
            String empId = claims.get("empId", String.class);
            String role = extractPrimaryRole(claims);
            List<SimpleGrantedAuthority> authorities = role == null || role.isBlank()
                    ? Collections.emptyList()
                    : List.of(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(empId, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            if (log.isDebugEnabled()) {
                log.debug("JWT authenticated empId={} authorities={} path={}", empId,
                        authorities.stream().map(SimpleGrantedAuthority::getAuthority).toList(), request.getRequestURI());
            }
        } catch (Exception e) {
            log.warn("JWT parse/validation failed for path {}: {}", request.getRequestURI(), e.getMessage());
        }
    }

    private String extractPrimaryRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (role != null && !role.isBlank()) return role;
        return Optional.ofNullable(resolveFromClaim(claims.get("roles")))
                .or(() -> Optional.ofNullable(resolveFromClaim(claims.get("authorities"))))
                .orElse(null);
    }

    private String resolveFromClaim(Object claim) {
        if (claim == null) return null;
        if (claim instanceof String s) {
            return s.isBlank() ? null : s.split(",")[0].trim();
        }
        if (claim instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first == null ? null : first.toString();
        }
        return null;
    }
}
