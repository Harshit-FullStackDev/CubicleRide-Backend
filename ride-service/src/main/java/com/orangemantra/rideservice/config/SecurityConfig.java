package com.orangemantra.rideservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ride/health", "/ride/route/**", "/locations", "/ride/debug/auth").permitAll()
                        .requestMatchers("/vehicle/my").authenticated()
                        .requestMatchers("/ride/offer").hasRole(ROLE_EMPLOYEE)
                        .requestMatchers("/ride/my-rides").hasRole(ROLE_EMPLOYEE)
                        .requestMatchers("/ride/history/**").hasRole(ROLE_EMPLOYEE)
                        .requestMatchers("/ride/join/**", "/ride/leave/**", "/ride/approve/**", "/ride/decline/**").hasRole(ROLE_EMPLOYEE)
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
