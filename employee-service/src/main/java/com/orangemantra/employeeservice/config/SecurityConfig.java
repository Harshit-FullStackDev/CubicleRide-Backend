package com.orangemantra.employeeservice.config;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/employee/save").permitAll()
                            .requestMatchers("/employee/all").hasAnyRole("ADMIN", "EMPLOYEE")
                            .requestMatchers("/employee/**").hasAnyRole("ADMIN","EMPLOYEE")
                            .requestMatchers("/vehicle/my").hasRole("EMPLOYEE")
                            .requestMatchers("/vehicle/{id}/verify").hasRole("ADMIN")
                            .requestMatchers("/vehicle/**").hasAnyRole("ADMIN","EMPLOYEE")
                            .requestMatchers("/notifications/**").hasAnyRole("ADMIN", "EMPLOYEE")
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .build();
        }
}
