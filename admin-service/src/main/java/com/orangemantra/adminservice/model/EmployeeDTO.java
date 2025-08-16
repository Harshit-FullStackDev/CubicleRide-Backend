package com.orangemantra.adminservice.model;

import lombok.Data;

@Data
public class EmployeeDTO {
    private Long id; // internal DB id when coming directly from employee-service
    private String empId;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String designation;
    private String officeLocation;
    private String gender;
    private String bio;
//    private String route;
}
