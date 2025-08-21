package com.orangemantra.employeeservice.repository;

import com.orangemantra.employeeservice.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Deterministic lookup via emailHash (SHA-256 of lowercase email)
    Optional<Employee> findByEmailHash(String emailHash);
    boolean existsByEmailHash(String emailHash);

    // Use list-based accessors to guard against unexpected duplicates
    List<Employee> findAllByEmpId(String empId);
}
