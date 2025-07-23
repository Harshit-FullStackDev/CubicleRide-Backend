package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.dto.EmployeeRegisterRequest;
import com.orangemantra.employeeservice.dto.RouteRequest;
import com.orangemantra.employeeservice.model.Employee;
import com.orangemantra.employeeservice.repository.EmployeeRepository;
import com.orangemantra.employeeservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeRepository repository;
    @PostMapping("/route")
    public String assignRoute(@RequestBody RouteRequest request) {
        return employeeService.assignRoute(request);
    }

    @GetMapping("/{empId}")
    public Employee getProfile(@PathVariable String empId) {
        return employeeService.getProfile(empId);
    }
    @PostMapping("/save")
    public ResponseEntity<String> saveEmployee(@RequestBody EmployeeRegisterRequest req) {
        Employee employee = new Employee();
        employee.setEmpId(req.getEmpId());
        employee.setName(req.getName());
        employee.setEmail(req.getEmail());
        repository.save(employee);
        return ResponseEntity.ok("Employee saved successfully");
    }
    @GetMapping("/all")
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

}
