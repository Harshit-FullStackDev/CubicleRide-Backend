package com.orangemantra.authservice.feign;

import com.orangemantra.authservice.dto.EmployeeRegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "employee-service")
public interface EmployeeClient {

    @PostMapping("/employee/save")
    void saveEmployee(@RequestBody EmployeeRegisterRequest request);
}

