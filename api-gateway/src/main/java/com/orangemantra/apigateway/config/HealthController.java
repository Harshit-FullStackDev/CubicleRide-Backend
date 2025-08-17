package com.orangemantra.apigateway.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/gateway/health")
    public Map<String,Object> health(){
        return Map.of("status","UP","service","api-gateway","timestamp",System.currentTimeMillis());
    }
}
