package com.orangemantra.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:*,https://localhost:*,https://*.azurestaticapps.net,https://*.azurewebsites.net,https://*.vercel.app,https://*.netlify.app,https://*.github.io}")
    private String allowedOrigins;

    @Bean
    public WebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow multiple origins for development and production
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",                           // Local development
            "https://localhost:*",                          // Local HTTPS development
            "https://www.cubicleride.me",                  // Custom domain frontend
            "https://cubicleride.me",                      // Root custom domain
            "https://salmon-sand-087aeff00.1.azurestaticapps.net", // Specific frontend domain
            "https://*.azurestaticapps.net",               // Azure Static Web Apps
            "https://*.azurewebsites.net",                 // Azure App Service
            "https://*.vercel.app",                        // Vercel deployments
            "https://*.netlify.app",                       // Netlify deployments
            "https://*.github.io"                          // GitHub Pages
        ));
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        CorsWebFilter delegate = new CorsWebFilter(source);

        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/ws") || path.startsWith("/api/ws")) {
                // Let downstream (ride-service websocket endpoint) handle CORS for SockJS/WS
                return chain.filter(exchange);
            }
            return delegate.filter(exchange, chain);
        };
    }
}
