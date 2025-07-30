package com.orangemantra.employeeservice.service;
import com.orangemantra.employeeservice.dto.RouteRequest;
import com.orangemantra.employeeservice.model.Employee;
import com.orangemantra.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.orangemantra.employeeservice.exception.EmployeeNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;

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
        emp.setName(updated.getName());
        emp.setEmail(updated.getEmail());
        // set other fields as needed
        return repository.save(emp);
    }

}
