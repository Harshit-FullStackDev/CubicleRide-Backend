package com.orangemantra.employeeservice.dto;

import lombok.Data;

@Data
public class VehicleVerifyRequest {
    private String status; // APPROVED or REJECTED
    private String rejectionReason;
}
