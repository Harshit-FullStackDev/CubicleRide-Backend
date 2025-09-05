package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.dto.VehicleRequest;
import com.orangemantra.employeeservice.dto.VehicleResponse;
import com.orangemantra.employeeservice.dto.VehicleVerifyRequest;
import com.orangemantra.employeeservice.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public VehicleResponse submitOrUpdate(@RequestBody VehicleRequest request, 
                                        org.springframework.security.core.Authentication authentication) {
        return vehicleService.submitOrUpdate(request, authentication.getName());
    }

    @GetMapping("/my")
    public VehicleResponse myVehicle(org.springframework.security.core.Authentication authentication) {
        return vehicleService.myVehicle(authentication.getName());
    }

    @GetMapping("/{empId}")
    public VehicleResponse getByEmpId(@PathVariable("empId") String empId) {
        return vehicleService.getByEmpId(empId);
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public VehicleResponse verify(@PathVariable("id") Long id, @RequestBody VehicleVerifyRequest request) {
        return vehicleService.verify(id, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<?> allVehicles() {
        return vehicleService.allVehicles();
    }
}
