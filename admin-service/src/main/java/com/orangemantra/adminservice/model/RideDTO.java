package com.orangemantra.adminservice.model;

import lombok.Data;

import java.util.List;

@Data
public class RideDTO {
    private Long id;
    private String ownerEmpId;
    private String route;
    private int totalSeats;
    private int availableSeats;
    private List<String> joinedEmpIds;
}
