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

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public String assignRoute(RouteRequest request) {
        Employee emp = getSingleByEmpIdOrThrow(request.getEmpId());
        // emp.setRoute(request.getRoute()); // placeholder
        repository.save(emp);
        return "Route updated";
    }

    public Employee getProfile(String empId) {
        return getSingleByEmpIdOrThrow(empId);
    }
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }
    public void deleteEmployee(String empId) {
        List<Employee> matches = repository.findAllByEmpId(empId);
        if (matches.isEmpty()) {
            throw new EmployeeNotFoundException("Employee with ID " + empId + " not found");
        }
        repository.deleteAll(matches); // remove all duplicates if any
    }
    public Employee updateEmployee(String empId, Employee updated) {
        Employee emp = getSingleByEmpIdOrThrow(empId);
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

    /* Helper: ensure we operate on a deterministic single row even if data corruption introduced duplicates */
    private Employee getSingleByEmpIdOrThrow(String empId) {
        List<Employee> matches = repository.findAllByEmpId(empId);
        if (matches.isEmpty()) {
            throw new EmployeeNotFoundException("Employee with ID " + empId + " not found");
        }
        if (matches.size() > 1) {
            // Keep the earliest (smallest id) deterministically; caller may later trigger cleanup
            matches.sort(Comparator.comparingLong(Employee::getId));
            // Optionally purge extras asynchronously; here just log.
        }
        return matches.get(0);
    }

}
