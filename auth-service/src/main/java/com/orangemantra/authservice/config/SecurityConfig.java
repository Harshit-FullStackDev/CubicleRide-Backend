//This class configures Spring Security for your auth-service:
//It defines a PasswordEncoder bean using BCrypt for secure password hashing.
//The SecurityFilterChain bean disables CSRF protection and allows all HTTP requests without
// authentication (anyRequest().permitAll()), meaning all endpoints are publicly accessible.
//This setup is typical for an authentication service where you want to handle authentication
// logic yourself, not restrict access at the gateway.
package com.orangemantra.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
