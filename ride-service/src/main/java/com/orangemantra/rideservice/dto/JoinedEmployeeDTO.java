package com.orangemantra.rideservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinedEmployeeDTO {
    private String empId;
    private String name;
    private String email;
    private String phone; // Conditionally exposed
    private Integer seats;

    public JoinedEmployeeDTO(String empId, String name, String email, String phone) {
        this.empId = empId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.seats = null;
    }
}


