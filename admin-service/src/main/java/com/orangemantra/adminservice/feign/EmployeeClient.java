package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import com.orangemantra.adminservice.model.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "employee-service",configuration = FeignClientConfig.class)
public interface EmployeeClient {
    @GetMapping("/employee/all")
    List<EmployeeDTO> getAllEmployees();
}
