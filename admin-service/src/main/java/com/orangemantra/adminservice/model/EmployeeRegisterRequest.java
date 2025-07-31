// admin-service/src/main/java/com/orangemantra/adminservice/model/EmployeeRegisterRequest.java
package com.orangemantra.adminservice.model;

import lombok.Data;

@Data
public class EmployeeRegisterRequest {
    private String empId;
    private String name;
    private String email;
    // add other fields if needed
}