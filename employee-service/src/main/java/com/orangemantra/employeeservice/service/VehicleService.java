package com.orangemantra.employeeservice.service;

import com.orangemantra.employeeservice.dto.VehicleRequest;
import com.orangemantra.employeeservice.dto.VehicleResponse;
import com.orangemantra.employeeservice.dto.VehicleVerifyRequest;
import com.orangemantra.employeeservice.model.Vehicle;
import com.orangemantra.employeeservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleResponse submitOrUpdate(VehicleRequest request, String empId) {
        Vehicle vehicle = vehicleRepository.findByEmpId(empId).orElseGet(() -> Vehicle.builder()
                .empId(empId)
                .createdAt(LocalDateTime.now())
                .build());
        if (vehicle.getId() != null && "APPROVED".equalsIgnoreCase(vehicle.getStatus())) {
            vehicle.setStatus("PENDING");
            vehicle.setVerifiedAt(null);
        }
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setCapacity(request.getCapacity());
        vehicle.setProofImageName(request.getProofImageName());
        vehicle.setProofImageUrl(request.getProofImageUrl());
        if (vehicle.getStatus() == null || vehicle.getStatus().isBlank()) {
            vehicle.setStatus("PENDING");
        }
        vehicle.setRejectionReason(null);
        vehicle.setUpdatedAt(LocalDateTime.now());
        if (vehicle.getProofImageUrl() != null && vehicle.getProofImageUrl().length() > 2_000_000) { // ~2MB characters (approx 1.5MB binary)
            vehicle.setProofImageUrl(vehicle.getProofImageUrl().substring(0, 2_000_000));
        }
        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    // Keep backward compatibility
    public VehicleResponse submitOrUpdate(VehicleRequest request) {
        return submitOrUpdate(request, "default");
    }

    public VehicleResponse myVehicle(String empId) {
        Vehicle vehicle = vehicleRepository.findByEmpId(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No vehicle submitted"));
        return toResponse(vehicle);
    }

    // Keep backward compatibility
    public VehicleResponse myVehicle() {
        return myVehicle("default");
    }

    public VehicleResponse getByEmpId(String empId) {
        Vehicle vehicle = vehicleRepository.findByEmpId(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No vehicle for employee"));
        return toResponse(vehicle);
    }

    public VehicleResponse verify(Long id, VehicleVerifyRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!"APPROVED".equalsIgnoreCase(request.getStatus()) && !"REJECTED".equalsIgnoreCase(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        vehicle.setStatus(request.getStatus().toUpperCase());
        if ("REJECTED".equalsIgnoreCase(request.getStatus())) {
            vehicle.setRejectionReason(request.getRejectionReason());
            vehicle.setVerifiedAt(null);
        } else {
            vehicle.setRejectionReason(null);
            vehicle.setVerifiedAt(LocalDateTime.now());
        }
        vehicle.setUpdatedAt(LocalDateTime.now());
        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    public List<VehicleResponse> allVehicles() {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    private VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .empId(v.getEmpId())
                .make(v.getMake())
                .model(v.getModel())
                .color(v.getColor())
                .registrationNumber(v.getRegistrationNumber())
                .capacity(v.getCapacity())
                .proofImageName(v.getProofImageName())
                .proofImageUrl(v.getProofImageUrl())
                .status(v.getStatus())
                .rejectionReason(v.getRejectionReason())
                .build();
    }
}
