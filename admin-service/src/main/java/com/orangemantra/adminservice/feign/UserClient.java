package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import com.orangemantra.adminservice.model.EmployeeRegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface UserClient {
    @DeleteMapping("/auth/user/{empId}")
    void deleteUser(@PathVariable("empId") String empId);
    @PutMapping("/auth/user/{empId}")
    void updateUser(@PathVariable("empId") String empId, @RequestBody EmployeeRegisterRequest employeeRegisterRequest);
}