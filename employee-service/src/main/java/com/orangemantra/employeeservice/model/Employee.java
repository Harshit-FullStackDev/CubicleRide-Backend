package com.orangemantra.employeeservice.model;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(
    indexes = {
        @Index(name = "idx_employee_emp_id", columnList = "empId"),
        @Index(name = "idx_employee_email", columnList = "email")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_emp_id", columnNames = "empId"),
        @UniqueConstraint(name = "uk_employee_email", columnNames = "email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String empId;

    private String name;

    private String email;

    // --- Profile enhancement fields ---
    private String phone;          // Contact number
    private String department;     // Department name
    private String designation;    // Job title
    private String officeLocation; // Primary office / campus
    private String gender;         // Optional
    private String bio;            // Short profile bio
}
