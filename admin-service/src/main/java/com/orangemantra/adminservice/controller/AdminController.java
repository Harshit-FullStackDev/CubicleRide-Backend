package com.orangemantra.adminservice.controller;

import com.orangemantra.adminservice.feign.EmployeeClient;
import com.orangemantra.adminservice.feign.RideClient;
import com.orangemantra.adminservice.model.EmployeeDTO;
import com.orangemantra.adminservice.model.RideDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
