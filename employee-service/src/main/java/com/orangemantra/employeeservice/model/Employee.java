package com.orangemantra.employeeservice.model;

import jakarta.persistence.*;
import lombok.*;
import com.orangemantra.employeeservice.util.StringCryptoConverter;

@Entity
@Table(
    indexes = {
        @Index(name = "idx_employee_emp_id", columnList = "empId"),
        @Index(name = "idx_employee_email_hash", columnList = "emailHash", unique = true)
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_emp_id", columnNames = "empId")
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

    @Convert(converter = StringCryptoConverter.class)
    @Column(length = 512)
    private String email;

    @Column(length = 64, unique = true)
    private String emailHash; // SHA-256(lowercase email)

    // --- Profile enhancement fields ---
    @Convert(converter = StringCryptoConverter.class)
    private String phone;          // Contact number

    @Convert(converter = StringCryptoConverter.class)
    private String department;     // Department name

    @Convert(converter = StringCryptoConverter.class)
    private String designation;    // Job title

    private String officeLocation; // Primary office / campus
    private String gender;         // Optional
    private String bio;            // Short profile bio
}
