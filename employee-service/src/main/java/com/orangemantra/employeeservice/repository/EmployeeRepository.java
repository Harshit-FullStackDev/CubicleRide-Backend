package com.orangemantra.employeeservice.repository;

import com.orangemantra.employeeservice.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Kept for backward compatibility; SHOULD be unique but may throw if duplicates exist.
    Optional<Employee> findByEmail(String email);

    // Use list-based accessors to guard against unexpected duplicates
    List<Employee> findAllByEmpId(String empId);
}
