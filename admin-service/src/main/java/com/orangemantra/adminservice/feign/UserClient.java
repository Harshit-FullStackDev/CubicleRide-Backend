package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import com.orangemantra.adminservice.model.EmployeeRegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface UserClient {
    @DeleteMapping("/auth/user/{empId}")
    void deleteUser(@PathVariable("empId") String empId);
    @PutMapping("/auth/user/{empId}")
    void updateUser(@PathVariable("empId") String empId, @RequestBody EmployeeRegisterRequest employeeRegisterRequest);
    @PostMapping("/auth/admin/otp/verify/{empId}")
    void adminVerifyOtp(@PathVariable("empId") String empId);
    @PostMapping("/auth/admin/otp/resend/{empId}")
    void adminResendOtp(@PathVariable("empId") String empId);
}