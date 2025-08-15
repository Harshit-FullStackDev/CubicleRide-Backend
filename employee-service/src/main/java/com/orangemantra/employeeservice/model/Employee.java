package com.orangemantra.employeeservice.model;

import jakarta.persistence.*;
import lombok.*;
@Entity
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
