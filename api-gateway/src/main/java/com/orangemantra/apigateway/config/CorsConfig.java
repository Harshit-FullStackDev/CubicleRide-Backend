package com.orangemantra.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public WebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // Frontend origin
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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
