package com.orangemantra.adminservice.feign;

import com.orangemantra.adminservice.config.FeignClientConfig;
import com.orangemantra.adminservice.model.EmployeeDTO;
import com.orangemantra.adminservice.model.EmployeeRegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "employee-service", contextId = "employeeClient", configuration = FeignClientConfig.class)
public interface EmployeeClient {
    @GetMapping("/employee/all")
    List<EmployeeDTO> getAllEmployees();
    @DeleteMapping("/employee/{empId}")
    void deleteEmployee(@PathVariable("empId") String empId);
    @GetMapping("/employee/{empId}")
    EmployeeDTO getEmployee(@PathVariable("empId") String empId);
    @PutMapping("/employee/{empId}")
    EmployeeDTO updateEmployee(@PathVariable("empId") String empId, @RequestBody EmployeeDTO employeeDTO);
    @PostMapping("/employee/save")
    void createEmployee(@RequestBody EmployeeRegisterRequest request);
}
