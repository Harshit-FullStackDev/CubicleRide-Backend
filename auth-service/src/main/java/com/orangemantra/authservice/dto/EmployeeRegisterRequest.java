package com.orangemantra.authservice.dto;

import lombok.Data;

@Data
public class EmployeeRegisterRequest {
    private String empId;
    private String name;
    private String email;
}
