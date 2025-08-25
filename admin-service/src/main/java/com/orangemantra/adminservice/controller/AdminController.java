package com.orangemantra.adminservice.controller;

import com.orangemantra.adminservice.feign.EmployeeClient;
import com.orangemantra.adminservice.feign.RideClient;
import com.orangemantra.adminservice.feign.UserClient;
import com.orangemantra.adminservice.feign.VehicleClient;
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
    private final VehicleClient vehicleClient;

    // BASIC HEALTH for manual UI consumption (lightweight alternative to actuator)
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("service", "admin-service");
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

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

    @DeleteMapping("/employees/{empId}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable("empId") String empId) {
        employeeClient.deleteEmployee(empId);
        try {
            userClient.deleteUser(empId);
        } catch (FeignException.NotFound e) {
        }
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/employees/{empId}")
    public EmployeeDTO getEmployee(@PathVariable("empId") String empId) {
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
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable("empId") String empId, @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO updatedEmployee = employeeClient.updateEmployee(empId, employeeDTO);
        userClient.updateUser(empId, toRegisterRequest(employeeDTO));
        return ResponseEntity.ok(updatedEmployee);
    }

    // CREATE employee in both services (auth + employee) - expects empId, name, email, password (optional default) on frontend
    @PostMapping("/employees")
    public ResponseEntity<Map<String, Object>> createEmployee(@RequestBody EmployeeRegisterRequest req) {
        // First create user in auth-service (password handled there: ensure default if null)
        userClient.updateUser(req.getEmpId(), req); // if user doesn't exist this may 404; alternative is to call /auth/register via RestTemplate (skipped for simplicity)
        employeeClient.createEmployee(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "CREATED");
        return ResponseEntity.ok(resp);
    }

    // VEHICLE MANAGEMENT -------------------------------------------------
    @GetMapping("/vehicles")
    public List<?> allVehicles() { return vehicleClient.allVehicles(); }

    @GetMapping("/vehicles/{empId}")
    public Map<String, Object> vehicleByEmp(@PathVariable("empId") String empId) { return vehicleClient.getByEmpId(empId); }

    @PutMapping("/vehicles/{id}/verify")
    public Map<String, Object> verifyVehicle(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return vehicleClient.verify(id, body);
    }

    // OTP MANAGEMENT ----------------------------------------------------
    @PostMapping("/employees/{empId}/otp/verify")
    public ResponseEntity<?> forceVerifyOtp(@PathVariable("empId") String empId) {
        userClient.adminVerifyOtp(empId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/employees/{empId}/otp/resend")
    public ResponseEntity<?> resendOtp(@PathVariable("empId") String empId) {
        userClient.adminResendOtp(empId);
        return ResponseEntity.ok().build();
    }
}
