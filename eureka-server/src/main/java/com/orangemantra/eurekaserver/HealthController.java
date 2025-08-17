package com.orangemantra.eurekaserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/eureka/health")
    public Map<String,Object> health(){
        return Map.of("status","UP","service","eureka-server","timestamp",System.currentTimeMillis());
    }
}
