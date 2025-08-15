package com.orangemantra.employeeservice.service;
import com.orangemantra.employeeservice.dto.RouteRequest;
import com.orangemantra.employeeservice.model.Employee;
import com.orangemantra.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.orangemantra.employeeservice.exception.EmployeeNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public String assignRoute(RouteRequest request) {
        Employee emp = repository.findByEmpId(request.getEmpId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID " + request.getEmpId() + " not found"));

//        emp.setRoute(request.getRoute());
        repository.save(emp);
        return "Route updated";
    }

    public Employee getProfile(String empId) {
        return repository.findByEmpId(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID " + empId + " not found"));
    }
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }
    public void deleteEmployee(String empId) {
        Employee emp = repository.findByEmpId(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID " + empId + " not found"));
        repository.delete(emp);
    }
    public Employee updateEmployee(String empId, Employee updated) {
        Employee emp = repository.findByEmpId(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID " + empId + " not found"));
    String oldName = emp.getName();
    emp.setName(updated.getName());
    // Email is OTP-verified at registration and must not be changed via profile updates.
    emp.setPhone(updated.getPhone());
    emp.setDepartment(updated.getDepartment());
    emp.setDesignation(updated.getDesignation());
    emp.setOfficeLocation(updated.getOfficeLocation());
    emp.setGender(updated.getGender());
    emp.setBio(updated.getBio());
        // set other fields as needed
        Employee saved = repository.save(emp);
        // Propagate name change to auth-service user table ONLY if name changed
        if (updated.getName() != null && !updated.getName().isBlank() && !updated.getName().equals(oldName)) {
            try {
                String url = "http://localhost:8081/auth/user/name/" + empId; // assuming auth-service on 8081
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String body = "{\"name\":\"" + updated.getName().replace("\"","\\\"") + "\"}";
                restTemplate.put(url, new HttpEntity<>(body, headers));
            } catch (Exception ignored) {}
        }
        return saved;
    }

}
