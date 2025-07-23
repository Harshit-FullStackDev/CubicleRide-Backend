package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import com.orangemantra.adminservice.model.RideDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ride-service",configuration = FeignClientConfig.class)
public interface RideClient {
    @GetMapping("/ride/all")
    List<RideDTO> getAllRides();
}
