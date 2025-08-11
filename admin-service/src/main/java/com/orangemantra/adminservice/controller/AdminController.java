package com.orangemantra.adminservice.controller;

import com.orangemantra.adminservice.feign.EmployeeClient;
import com.orangemantra.adminservice.feign.RideClient;
import com.orangemantra.adminservice.feign.UserClient;
import com.orangemantra.adminservice.model.EmployeeDTO;
import com.orangemantra.adminservice.model.EmployeeRegisterRequest;
import com.orangemantra.adminservice.model.RideDTO;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final UserClient userClient;

    @GetMapping("/employees")
    public List<EmployeeDTO> getAllEmployees() {
        return employeeClient.getAllEmployees();
    }

    @   GetMapping("/rides")
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

    @DeleteMapping("/employees/{empId}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String empId) {
        employeeClient.deleteEmployee(empId);
        try {
            userClient.deleteUser(empId);
        } catch (FeignException.NotFound e) {
        }
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/employees/{empId}")
    public EmployeeDTO getEmployee(@PathVariable String empId) {
        return employeeClient.getEmployee(empId);
    }
    private EmployeeRegisterRequest toRegisterRequest(EmployeeDTO dto) {
        EmployeeRegisterRequest req = new EmployeeRegisterRequest();
        req.setEmpId(dto.getEmpId());
        req.setName(dto.getName());
       req.setEmail(dto.getEmail());
        return req;
    }
    @PutMapping("/employees/{empId}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable String empId, @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO updatedEmployee = employeeClient.updateEmployee(empId, employeeDTO);
        userClient.updateUser(empId, toRegisterRequest(employeeDTO));
        return ResponseEntity.ok(updatedEmployee);
    }
}
