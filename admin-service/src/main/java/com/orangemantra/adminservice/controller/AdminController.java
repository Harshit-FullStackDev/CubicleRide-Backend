package com.orangemantra.adminservice.controller;

import com.orangemantra.adminservice.feign.EmployeeClient;
import com.orangemantra.adminservice.feign.RideClient;
import com.orangemantra.adminservice.model.EmployeeDTO;
import com.orangemantra.adminservice.model.RideDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeClient employeeClient;
    private final RideClient rideClient;

    @GetMapping("/employees")
    public List<EmployeeDTO> getAllEmployees() {
        return employeeClient.getAllEmployees();
    }

    @GetMapping("/rides")
    public List<RideDTO> getAllRides() {
        return rideClient.getAllRides();
    }
    @GetMapping("/employees/count")
    public Map<String, Long> getEmployeeCount() {
        long count = employeeClient.getAllEmployees().size();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return response;
    }
    @GetMapping("/rides/count")
    public Map<String, Long> getRideCount() {
        long count = rideClient.getAllRides().size();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return response;
    }
}
