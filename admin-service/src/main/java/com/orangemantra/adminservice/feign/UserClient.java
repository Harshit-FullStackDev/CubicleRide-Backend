package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface UserClient {
    @DeleteMapping("/auth/user/{empId}")
    void deleteUser(@PathVariable("empId") String empId);
}