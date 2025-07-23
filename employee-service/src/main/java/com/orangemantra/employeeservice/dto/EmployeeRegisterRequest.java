package com.orangemantra.employeeservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRegisterRequest {
    private String empId;
    private String name;
    private String email;
}
