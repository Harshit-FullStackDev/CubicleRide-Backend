package com.orangemantra.employeeservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRegisterRequest {
    private String empId;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String designation;
    private String officeLocation;
    private String gender;
    private String bio;
}
