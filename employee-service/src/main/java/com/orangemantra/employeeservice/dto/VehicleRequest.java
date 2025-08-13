package com.orangemantra.employeeservice.dto;

import lombok.Data;

@Data
public class VehicleRequest {
    private String make;
    private String model;
    private String color;
    private String registrationNumber;
    private Integer capacity;
    private String proofImageName;
    private String proofImageUrl;
}
