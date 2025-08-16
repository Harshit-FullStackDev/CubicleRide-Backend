package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "employee-service", contextId = "vehicleClient", configuration = FeignClientConfig.class)
public interface VehicleClient {

    @GetMapping("/vehicle")
    List<Map<String, Object>> allVehicles();

    @GetMapping("/vehicle/{empId}")
    Map<String, Object> getByEmpId(@PathVariable("empId") String empId);

    @PutMapping("/vehicle/{id}/verify")
    Map<String, Object> verify(@PathVariable("id") Long id, @RequestBody Map<String, Object> body);
}
