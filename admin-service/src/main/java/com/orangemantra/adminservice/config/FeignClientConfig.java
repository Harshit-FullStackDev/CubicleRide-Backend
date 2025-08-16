package com.orangemantra.adminservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String headerToSend = null;

            // Primary: extract from current HTTP request (same thread)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                headerToSend = request.getHeader("Authorization");
            }

            // Fallback: extract token stored as credentials in SecurityContext (set by JwtAuthFilter)
            if (headerToSend == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getCredentials() instanceof String token && !token.isEmpty()) {
                    headerToSend = "Bearer " + token;
                }
            }

            if (headerToSend != null) {
                requestTemplate.header("Authorization", headerToSend);
            } else {
                log.warn("Feign request to {} missing Authorization header (user: {})", requestTemplate.path(),
                        SecurityContextHolder.getContext().getAuthentication() != null ? SecurityContextHolder.getContext().getAuthentication().getName() : "anonymous");
            }
        };
    }
}
