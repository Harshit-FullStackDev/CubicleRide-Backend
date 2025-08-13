package com.orangemantra.rideservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.orangemantra.rideservice.dto.JoinedEmployeeDTO;
import com.orangemantra.rideservice.dto.RideResponseDTO;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.RideRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);
    private static final String ACTIVE_RIDE_CONFLICT_MSG = "You already have a published ride. Please publish a new ride after the active ride ends.";

    public Ride offerRide(Ride ride) {
        if (ride.getTotalSeats() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total seats cannot exceed 8");
        }
        try {
            // Include caller Authorization header when invoking employee-service
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String authHeader = null;
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                authHeader = req.getHeader("Authorization");
            }
            String url = "http://localhost:8082/vehicle/" + ride.getOwnerEmpId();
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null) headers.set("Authorization", authHeader);
            ResponseEntity<VehicleInfo> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), VehicleInfo.class);
            VehicleInfo v = resp.getBody();
            if (v == null || v.getStatus() == null || !"APPROVED".equalsIgnoreCase(v.getStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vehicle not approved. Please submit vehicle details and wait for approval before offering rides.");
            }
            if (v.getCapacity() != null && ride.getTotalSeats() > v.getCapacity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total seats cannot exceed approved vehicle capacity (" + v.getCapacity() + ")");
            }
            // Always override carDetails with latest approved vehicle info so user cannot spoof it
            try {
                StringBuilder sb = new StringBuilder();
                if (v.getMake() != null && !v.getMake().isBlank()) sb.append(v.getMake()).append(' ');
                if (v.getModel() != null && !v.getModel().isBlank()) sb.append(v.getModel()).append(' ');
                if (v.getRegistrationNumber() != null && !v.getRegistrationNumber().isBlank()) {
                    if (sb.length() > 0) sb.append('(').append(v.getRegistrationNumber()).append(')');
                    else sb.append(v.getRegistrationNumber());
                }
                String details = sb.toString().trim().replaceAll(" +", " ");
                if (!details.isEmpty()) {
                    ride.setCarDetails(details);
                }
            } catch (Exception ignored) {}
        } catch (HttpClientErrorException.NotFound nf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No vehicle submitted. Please add your vehicle first.");
        } catch (HttpClientErrorException.Forbidden fb) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to verify vehicle (forbidden). Please re-login.");
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify vehicle status. Try again later.");
        }
        // Ensure past rides are expired first so they don't block offering
        expirePastRidesInternal();
        // Guard: owner cannot have more than one active (pre-arrival) ride
        List<Ride> activeOwned = rideRepository.findByOwnerEmpIdAndStatus(ride.getOwnerEmpId(), "Active");
        LocalDateTime now = LocalDateTime.now();
        for (Ride r : activeOwned) {
            if (r.getDate() != null && r.getArrivalTime() != null) {
                try {
                    LocalTime at = LocalTime.parse(r.getArrivalTime());
                    LocalDateTime scheduled = LocalDateTime.of(r.getDate(), at);
                    if (now.isBefore(scheduled)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
                    }
                } catch (Exception ignored) {
                    // If parsing fails treat it as blocking to stay safe
                    throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
                }
            } else {
                // Missing date/time but still active – block
                throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
            }
        }
        ride.setAvailableSeats(ride.getTotalSeats());
        ride.setStatus("Active");
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);
        if (ride.getJoinedEmpIds() == null) ride.setJoinedEmpIds(new ArrayList<>());
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
        if (!"Active".equalsIgnoreCase(ride.getStatus())) {
            throw new RuntimeException("Ride is not active");
        }
        // auto-expire check
        expirePastRidesInternal();
        if (ride.getAvailableSeats() <= 0) {
            throw new RuntimeException("No seats available");
        }
        if (ride.getJoinedEmpIds().contains(empId)) {
            return; // idempotent join
        }
        ride.getJoinedEmpIds().add(empId);
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        ride.setUpdatedAt(LocalDateTime.now());
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
        existingRide.setUpdatedAt(LocalDateTime.now());

        Ride saved = rideRepository.save(existingRide);

        // Notify joined employees if ride still active and before its arrival time
        boolean beforeArrival = false;
        try {
            if (saved.getStatus() != null && "Active".equalsIgnoreCase(saved.getStatus()) && saved.getDate() != null && saved.getArrivalTime() != null) {
                LocalTime at = LocalTime.parse(saved.getArrivalTime());
                LocalDateTime scheduled = LocalDateTime.of(saved.getDate(), at);
                beforeArrival = LocalDateTime.now().isBefore(scheduled);
            }
        } catch (Exception ignored) {}
        if (beforeArrival && saved.getJoinedEmpIds() != null && !saved.getJoinedEmpIds().isEmpty()) {
            notifyJoinedOnUpdate(saved, new ArrayList<>(saved.getJoinedEmpIds()));
        }
        return saved;
    }
    public void deleteRide(Long rideId) {
        // First ensure past rides are expired appropriately (date/time aware)
        expirePastRidesInternal();

        Ride existingRide = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        // Only notify joined employees if the ride has not yet reached its arrival time
        boolean beforeArrival = false;
        try {
            if (existingRide.getDate() != null && existingRide.getArrivalTime() != null) {
                LocalTime at = LocalTime.parse(existingRide.getArrivalTime());
                LocalDateTime scheduled = LocalDateTime.of(existingRide.getDate(), at);
                beforeArrival = LocalDateTime.now().isBefore(scheduled);
            }
        } catch (Exception ignored) {
            // If time parsing fails, fall back to date-only comparison
            beforeArrival = existingRide.getDate() != null && existingRide.getDate().isAfter(LocalDate.now());
        }

        // Mark as cancelled and persist (do not delete to preserve history semantics)
        List<String> joined = new ArrayList<>(existingRide.getJoinedEmpIds());
        existingRide.setStatus("Cancelled");
        existingRide.setUpdatedAt(LocalDateTime.now());
        rideRepository.save(existingRide);

        // Notify only when cancellation happens before scheduled arrival
        if (beforeArrival && !joined.isEmpty()) {
            notifyJoinedOnCancel(existingRide, joined);
        }
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
            ride.setUpdatedAt(LocalDateTime.now());
            rideRepository.save(ride);
        } else {
            throw new RuntimeException("Employee not joined in this ride");
        }
    }

    public void expirePastRidesInternal() {
        // Expire rides where either:
        // - date is before today, or
        // - date is today AND arrivalTime is at or before current time
        List<Ride> activeRides = rideRepository.findByStatus("Active");
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        for (Ride r : activeRides) {
            boolean expire = false;
            if (r.getDate() != null) {
                if (r.getDate().isBefore(today)) {
                    expire = true;
                } else if (r.getDate().isEqual(today)) {
                    try {
                        if (r.getArrivalTime() != null) {
                            LocalTime at = LocalTime.parse(r.getArrivalTime());
                            if (!at.isAfter(nowTime)) {
                                expire = true;
                            }
                        }
                    } catch (Exception ignored) {
                        // If parsing fails, leave as is for today
                    }
                }
            }
            if (expire) {
                r.setStatus("Expired");
                r.setUpdatedAt(LocalDateTime.now());
                rideRepository.save(r);
            }
        }
    }

    public List<RideResponseDTO> getPublishedRideHistory(String ownerEmpId) {
        expirePastRidesInternal();
        List<Ride> rides = rideRepository.findByOwnerEmpIdAndStatus(ownerEmpId, "Expired");
        return mapRidesToDtoWithEmployees(rides, "Expired");
    }

    public List<RideResponseDTO> getJoinedRideHistory(String empId) {
        expirePastRidesInternal();
        List<Ride> rides = rideRepository.findByJoinedEmpIdsContainingAndStatus(empId, "Expired");
        return mapRidesToDtoWithEmployees(rides, "Expired");
    }

    private void notifyJoinedOnCancel(Ride ride, List<String> joinedEmpIds) {
        // attempt to notify via employee-service notifications API
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
        try {
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            headers.set("Content-Type", "application/json");
            for (String je : joinedEmpIds) {
                NotificationService.NotificationRequest notification = new NotificationService.NotificationRequest(
                        je,
                        "Your ride from " + ride.getOrigin() + " to " + ride.getDestination() + " has been cancelled by the owner."
                );
                HttpEntity<NotificationService.NotificationRequest> entity = new HttpEntity<>(notification, headers);
                restTemplate.postForEntity("http://localhost:8082/notifications", entity, Void.class);
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation notifications: {}", e.getMessage());
        }
    }

    private void notifyJoinedOnUpdate(Ride ride, List<String> joinedEmpIds) {
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
        try {
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            headers.set("Content-Type", "application/json");
            for (String je : joinedEmpIds) {
                NotificationService.NotificationRequest notification = new NotificationService.NotificationRequest(
                        je,
                        "A ride from " + ride.getOrigin() + " to " + ride.getDestination() + " has been updated by the owner."
                );
                HttpEntity<NotificationService.NotificationRequest> entity = new HttpEntity<>(notification, headers);
                restTemplate.postForEntity("http://localhost:8082/notifications", entity, Void.class);
            }
        } catch (Exception e) {
            log.error("Failed to send update notifications: {}", e.getMessage());
        }
    }

    private List<RideResponseDTO> mapRidesToDtoWithEmployees(List<Ride> rides, String defaultStatus) {
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
            // Fetch owner details
            String ownerName = "Unknown";
            try {
                String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
                HttpHeaders headers = new HttpHeaders();
                if (jwt != null) headers.set("Authorization", jwt);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                EmployeeProfile owner = response.getBody();
                if (owner != null) {
                    ownerName = owner.getName();
                }
            } catch (Exception e) {
                log.error("Failed to fetch owner {} from employee-service: {}", ride.getOwnerEmpId(), e.getMessage());
            }
            // Fetch joined employees
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
                    .ownerName(ownerName)
                    .origin(ride.getOrigin())
                    .destination(ride.getDestination())
                    .date(ride.getDate() != null ? ride.getDate().toString() : null)
                    .arrivalTime(ride.getArrivalTime())
                    .carDetails(ride.getCarDetails())
                    .totalSeats(ride.getTotalSeats())
                    .availableSeats(ride.getAvailableSeats())
                    .status(ride.getStatus() != null ? ride.getStatus() : defaultStatus)
                    .joinedEmployees(joinedEmployees)
                    .build();
        }).toList();
    }

    @Data
    static
    class EmployeeProfile {
        private String empId;
        private String name;
        private String email;
    }
    @Data
    static class VehicleInfo {
        private Long id; private String status; private Integer capacity; private String registrationNumber; private String make; private String model; private String color; }
    public List<RideResponseDTO> getRidesWithEmployeeDetailsByOwner(String ownerEmpId) {
        List<Ride> rides = getRidesByOwner(ownerEmpId).stream()
                .filter(r -> r.getStatus() == null || "Active".equalsIgnoreCase(r.getStatus()))
                .toList();
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
            // Fetch owner details
            String ownerName = "Unknown";
            try {
                String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
                HttpHeaders headers = new HttpHeaders();
                if (jwt != null) headers.set("Authorization", jwt);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                EmployeeProfile owner = response.getBody();
                if (owner != null) {
                    ownerName = owner.getName();
                }
            } catch (Exception e) {
                log.error("Failed to fetch owner {} from employee-service: {}", ride.getOwnerEmpId(), e.getMessage());
            }
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
                    .ownerName(ownerName)
                    .origin(ride.getOrigin())
                    .destination(ride.getDestination())
                    .date(ride.getDate() != null ? ride.getDate().toString() : null)
                    .arrivalTime(ride.getArrivalTime())
                    .carDetails(ride.getCarDetails())
                    .totalSeats(ride.getTotalSeats())
                    .availableSeats(ride.getAvailableSeats())
            .status(ride.getStatus() != null ? ride.getStatus() : "Active")
                    .joinedEmployees(joinedEmployees)
                    .build();
        }).toList();
    }

    public List<RideResponseDTO> getAllRidesWithEmployeeDetails() {
        List<Ride> rides = getAllRides();
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
            // Fetch owner details
            String ownerName = "Unknown";
            try {
                String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
                HttpHeaders headers = new HttpHeaders();
                if (jwt != null) headers.set("Authorization", jwt);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                EmployeeProfile owner = response.getBody();
                if (owner != null) {
                    ownerName = owner.getName();
                }
            } catch (Exception e) {
                log.error("Failed to fetch owner {} from employee-service: {}", ride.getOwnerEmpId(), e.getMessage());
            }
            // Fetch joined employees
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
                    .ownerName(ownerName)
                    .origin(ride.getOrigin())
                    .destination(ride.getDestination())
                    .date(ride.getDate() != null ? ride.getDate().toString() : null)
                    .arrivalTime(ride.getArrivalTime())
                    .carDetails(ride.getCarDetails())
                    .totalSeats(ride.getTotalSeats())
                    .availableSeats(ride.getAvailableSeats())
            .status(ride.getStatus())
                    .joinedEmployees(joinedEmployees)
                    .build();
        }).toList();
    }

    public List<RideResponseDTO> getActiveRidesWithEmployeeDetails() {
        // Only active rides after performing expiry
        List<Ride> rides = rideRepository.findByStatus("Active");
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
            String ownerName = "Unknown";
            try {
                String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
                HttpHeaders headers = new HttpHeaders();
                if (jwt != null) headers.set("Authorization", jwt);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                EmployeeProfile owner = response.getBody();
                if (owner != null) {
                    ownerName = owner.getName();
                }
            } catch (Exception e) {
                log.error("Failed to fetch owner {} from employee-service: {}", ride.getOwnerEmpId(), e.getMessage());
            }
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
                    .ownerName(ownerName)
                    .origin(ride.getOrigin())
                    .destination(ride.getDestination())
                    .date(ride.getDate() != null ? ride.getDate().toString() : null)
                    .arrivalTime(ride.getArrivalTime())
                    .carDetails(ride.getCarDetails())
                    .totalSeats(ride.getTotalSeats())
                    .availableSeats(ride.getAvailableSeats())
                    .status(ride.getStatus())
                    .joinedEmployees(joinedEmployees)
                    .build();
        }).toList();
    }

    public List<RideResponseDTO> getJoinedRidesWithEmployeeDetails(String empId) {
        List<Ride> rides = getJoinedRides(empId).stream()
                .filter(r -> r.getStatus() == null || "Active".equalsIgnoreCase(r.getStatus()))
                .toList();
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
            // Fetch owner details
            String ownerName = "Unknown";
            try {
                String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
                HttpHeaders headers = new HttpHeaders();
                if (jwt != null) headers.set("Authorization", jwt);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                EmployeeProfile owner = response.getBody();
                if (owner != null) {
                    ownerName = owner.getName();
                }
            } catch (Exception e) {
                log.error("Failed to fetch owner {} from employee-service: {}", ride.getOwnerEmpId(), e.getMessage());
            }
            // Fetch joined employees
            List<JoinedEmployeeDTO> joinedEmployees = ride.getJoinedEmpIds().stream().map(jEmpId -> {
                try {
                    String url = "http://localhost:8082/employee/" + jEmpId;
                    HttpHeaders headers = new HttpHeaders();
                    if (jwt != null) headers.set("Authorization", jwt);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeProfile.class);
                    EmployeeProfile emp = response.getBody();
                    if (emp != null) {
                        return new JoinedEmployeeDTO(emp.getEmpId(), emp.getName(), emp.getEmail());
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch employee {} from employee-service: {}", jEmpId, e.getMessage());
                }
                return new JoinedEmployeeDTO(jEmpId, "Unknown", "");
            }).toList();
        return RideResponseDTO.builder()
                    .id(ride.getId())
                    .ownerEmpId(ride.getOwnerEmpId())
                    .ownerName(ownerName)
                    .origin(ride.getOrigin())
                    .destination(ride.getDestination())
                    .date(ride.getDate() != null ? ride.getDate().toString() : null)
                    .arrivalTime(ride.getArrivalTime())
                    .carDetails(ride.getCarDetails())
                    .totalSeats(ride.getTotalSeats())
                    .availableSeats(ride.getAvailableSeats())
            .status(ride.getStatus() != null ? ride.getStatus() : "Active")
                    .joinedEmployees(joinedEmployees)
                    .build();
        }).toList();
    }
}
