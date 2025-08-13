package com.orangemantra.employeeservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleResponse {
    private Long id;
    private String empId;
    private String make;
    private String model;
    private String color;
    private String registrationNumber;
    private Integer capacity;
    private String proofImageName;
    private String proofImageUrl;
    private String status;
    private String rejectionReason;
}
