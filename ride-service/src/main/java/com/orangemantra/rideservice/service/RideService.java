package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.JoinedEmployeeDTO;
import com.orangemantra.rideservice.dto.RideResponseDTO;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.RideRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    public Ride offerRide(Ride ride) {
        if (ride.getTotalSeats() > 8) {
            throw new RuntimeException("Total seats cannot exceed 8");
        }
        ride.setAvailableSeats(ride.getTotalSeats());
        return rideRepository.save(ride);
    }

    public List<Ride> getRidesByOwner(String ownerEmpId) {
        return rideRepository.findByOwnerEmpId(ownerEmpId);
    }

    public List<Ride> getRidesByOriginAndDestination(String origin, String destination) {
        return rideRepository.findByOriginAndDestination(origin, destination);
    }

    public void joinRide(Long rideId, String empId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getAvailableSeats() <= 0) {
            throw new RuntimeException("No seats available");
        }

        ride.getJoinedEmpIds().add(empId);
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        rideRepository.save(ride);

    }

    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }
    public Ride updateRide(Long rideId, Ride updatedRide) {
        Ride existingRide = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        existingRide.setOrigin(updatedRide.getOrigin());
        existingRide.setDestination(updatedRide.getDestination());
        existingRide.setDate(updatedRide.getDate());
        existingRide.setArrivalTime(updatedRide.getArrivalTime());
        existingRide.setCarDetails(updatedRide.getCarDetails());
        existingRide.setTotalSeats(updatedRide.getTotalSeats());
        existingRide.setAvailableSeats(updatedRide.getAvailableSeats());

        return rideRepository.save(existingRide);
    }
    public void deleteRide(Long rideId) {
        Ride existingRide = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        rideRepository.delete(existingRide);
    }
    public Ride getRideById(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    public List<Ride> getJoinedRides(String empId) {
        return rideRepository.findByJoinedEmpIdsContaining(empId);
    }

    public void leaveRide(Long rideId, String empId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        if (ride.getJoinedEmpIds().remove(empId)) {
            ride.setAvailableSeats(ride.getAvailableSeats() + 1);
            rideRepository.save(ride);
        } else {
            throw new RuntimeException("Employee not joined in this ride");
        }
    }

    @Data
    static
    class EmployeeProfile {
        private String empId;
        private String name;
        private String email;
    }

    public List<RideResponseDTO> getRidesWithEmployeeDetailsByOwner(String ownerEmpId) {
        List<Ride> rides = getRidesByOwner(ownerEmpId);
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        final String jwt;
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader;
            } else {
                jwt = null;
            }
        } else {
            jwt = null;
        }
        return rides.stream().map(ride -> {
            List<JoinedEmployeeDTO> joinedEmployees = ride.getJoinedEmpIds().stream().map(empId -> {
                try {
                    String url = "http://localhost:8082/employee/" + empId;
                    HttpHeaders headers = new HttpHeaders();
                    if (jwt != null) headers.set("Authorization", jwt);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                    EmployeeProfile emp = response.getBody();
                    if (emp != null) {
                        return new JoinedEmployeeDTO(emp.getEmpId(), emp.getName(), emp.getEmail());
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch employee {} from employee-service: {}", empId, e.getMessage());
                }
                return new JoinedEmployeeDTO(empId, "Unknown", "");
            }).toList();
            return RideResponseDTO.builder()
                    .id(ride.getId())
                    .ownerEmpId(ride.getOwnerEmpId())
                    .origin(ride.getOrigin())
                    .destination(ride.getDestination())
                    .date(ride.getDate() != null ? ride.getDate().toString() : null)
                    .arrivalTime(ride.getArrivalTime())
                    .carDetails(ride.getCarDetails())
                    .totalSeats(ride.getTotalSeats())
                    .availableSeats(ride.getAvailableSeats())
                    .status("Active")
                    .joinedEmployees(joinedEmployees)
                    .build();
        }).toList();
    }
}
