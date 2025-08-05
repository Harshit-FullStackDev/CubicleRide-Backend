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
}

