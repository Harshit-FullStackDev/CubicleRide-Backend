package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.JoinedEmployeeDTO;
import com.orangemantra.rideservice.dto.RideResponseDTO;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.RideRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RideService {
    private final RideRepository rideRepository;
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);
    private static final String ACTIVE_RIDE_CONFLICT_MSG = "You already have a published ride. Please publish a new ride after the active ride ends.";

    // OFFER RIDE --------------------------------------------------
    public Ride offerRide(Ride ride) {
        if (ride.getTotalSeats() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total seats cannot exceed 8");
        }
        try {
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
            try { // derive car details from approved vehicle
                StringBuilder sb = new StringBuilder();
                if (v.getMake() != null && !v.getMake().isBlank()) sb.append(v.getMake()).append(' ');
                if (v.getModel() != null && !v.getModel().isBlank()) sb.append(v.getModel()).append(' ');
                if (v.getRegistrationNumber() != null && !v.getRegistrationNumber().isBlank()) {
                    if (sb.length() > 0) sb.append('(').append(v.getRegistrationNumber()).append(')');
                    else sb.append(v.getRegistrationNumber());
                }
                String details = sb.toString().trim().replaceAll(" +", " ");
                if (!details.isEmpty()) ride.setCarDetails(details);
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

        expirePastRidesInternal();
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
                    throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
                }
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
            }
        }
        ride.setAvailableSeats(ride.getTotalSeats());
        ride.setStatus("Active");
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);
        if (ride.getJoinedEmpIds() == null) ride.setJoinedEmpIds(new ArrayList<>());
        if (ride.getPendingEmpIds() == null) ride.setPendingEmpIds(new ArrayList<>());
        return rideRepository.save(ride);
    }

    // BASIC CRUD HELPERS ------------------------------------------
    public List<Ride> getRidesByOwner(String ownerEmpId) { return rideRepository.findByOwnerEmpId(ownerEmpId); }
    public List<Ride> getRidesByOriginAndDestination(String origin, String destination) { return rideRepository.findByOriginAndDestination(origin, destination); }
    public List<Ride> getAllRides() { return rideRepository.findAll(); }
    public Ride getRideById(Long id) { return rideRepository.findById(id).orElseThrow(() -> new RuntimeException("Ride not found")); }
    public List<Ride> getJoinedRides(String empId) { return rideRepository.findByJoinedEmpIdsContaining(empId); }

    // JOIN / APPROVAL FLOW ----------------------------------------
    public void joinRide(Long rideId, String empId) {
        Ride ride = getRideById(rideId);
        if (!"Active".equalsIgnoreCase(ride.getStatus())) throw new RuntimeException("Ride is not active");
        expirePastRidesInternal();
        if (ride.getAvailableSeats() <= 0) throw new RuntimeException("No seats available");
        if (ride.getJoinedEmpIds().contains(empId)) return;
        if (ride.getPendingEmpIds() != null && ride.getPendingEmpIds().contains(empId)) return;
        if (ride.isInstantBookingEnabled()) {
            ride.getJoinedEmpIds().add(empId);
            ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        } else {
            ride.getPendingEmpIds().add(empId);
        }
        ride.setUpdatedAt(LocalDateTime.now());
        rideRepository.save(ride);
    }

    public void approveJoin(Long rideId, String ownerEmpId, String empId) {
        Ride ride = getRideById(rideId);
        if (!ride.getOwnerEmpId().equals(ownerEmpId)) throw new RuntimeException("Not ride owner");
        if (ride.isInstantBookingEnabled()) return; // nothing to approve
        if (ride.getAvailableSeats() <= 0) throw new RuntimeException("No seats available");
        if (ride.getPendingEmpIds().remove(empId)) {
            if (!ride.getJoinedEmpIds().contains(empId)) {
                ride.getJoinedEmpIds().add(empId);
                ride.setAvailableSeats(ride.getAvailableSeats() - 1);
            }
            ride.setUpdatedAt(LocalDateTime.now());
            rideRepository.save(ride);
        }
    }

    public void declineJoin(Long rideId, String ownerEmpId, String empId) {
        Ride ride = getRideById(rideId);
        if (!ride.getOwnerEmpId().equals(ownerEmpId)) throw new RuntimeException("Not ride owner");
        if (ride.isInstantBookingEnabled()) return;
        if (ride.getPendingEmpIds().remove(empId)) {
            ride.setUpdatedAt(LocalDateTime.now());
            rideRepository.save(ride);
        }
    }

    public Ride updateRide(Long rideId, Ride updatedRide) {
        Ride existing = getRideById(rideId);
        existing.setOrigin(updatedRide.getOrigin());
        existing.setDestination(updatedRide.getDestination());
        existing.setDate(updatedRide.getDate());
        existing.setArrivalTime(updatedRide.getArrivalTime());
        existing.setCarDetails(updatedRide.getCarDetails());
        existing.setTotalSeats(updatedRide.getTotalSeats());
        existing.setAvailableSeats(updatedRide.getAvailableSeats());
        existing.setInstantBookingEnabled(updatedRide.isInstantBookingEnabled());
    existing.setFare(updatedRide.getFare());
        existing.setUpdatedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(existing);
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
        expirePastRidesInternal();
        Ride existing = getRideById(rideId);
        boolean beforeArrival = false;
        try {
            if (existing.getDate() != null && existing.getArrivalTime() != null) {
                LocalTime at = LocalTime.parse(existing.getArrivalTime());
                LocalDateTime scheduled = LocalDateTime.of(existing.getDate(), at);
                beforeArrival = LocalDateTime.now().isBefore(scheduled);
            }
        } catch (Exception ignored) {
            beforeArrival = existing.getDate() != null && existing.getDate().isAfter(LocalDate.now());
        }
        List<String> joined = new ArrayList<>(existing.getJoinedEmpIds());
        existing.setStatus("Cancelled");
        existing.setUpdatedAt(LocalDateTime.now());
        rideRepository.save(existing);
        if (beforeArrival && !joined.isEmpty()) notifyJoinedOnCancel(existing, joined);
    }

    public void leaveRide(Long rideId, String empId) {
        Ride ride = getRideById(rideId);
        if (ride.getJoinedEmpIds().remove(empId)) {
            ride.setAvailableSeats(ride.getAvailableSeats() + 1);
            ride.setUpdatedAt(LocalDateTime.now());
            rideRepository.save(ride);
        } else if (ride.getPendingEmpIds().remove(empId)) { // allow withdrawal of pending request
            ride.setUpdatedAt(LocalDateTime.now());
            rideRepository.save(ride);
        } else {
            throw new RuntimeException("Employee not in this ride");
        }
    }

    // EXPIRY & HISTORY --------------------------------------------
    public void expirePastRidesInternal() {
        List<Ride> activeRides = rideRepository.findByStatus("Active");
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        for (Ride r : activeRides) {
            boolean expire = false;
            if (r.getDate() != null) {
                if (r.getDate().isBefore(today)) expire = true;
                else if (r.getDate().isEqual(today)) {
                    try {
                        if (r.getArrivalTime() != null) {
                            LocalTime at = LocalTime.parse(r.getArrivalTime());
                            if (!at.isAfter(nowTime)) expire = true;
                        }
                    } catch (Exception ignored) {}
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
        return mapRidesToDtoWithEmployees(rideRepository.findByOwnerEmpIdAndStatus(ownerEmpId, "Expired"), "Expired");
    }
    public List<RideResponseDTO> getJoinedRideHistory(String empId) {
        expirePastRidesInternal();
        return mapRidesToDtoWithEmployees(rideRepository.findByJoinedEmpIdsContainingAndStatus(empId, "Expired"), "Expired");
    }

    // NOTIFICATIONS ----------------------------------------------
    private void notifyJoinedOnCancel(Ride ride, List<String> joinedEmpIds) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        final String jwt;
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String authHeader = req.getHeader("Authorization");
            jwt = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader : null;
        } else jwt = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            headers.set("Content-Type", "application/json");
            for (String je : joinedEmpIds) {
                NotificationService.NotificationRequest note = new NotificationService.NotificationRequest(
                        je,
                        "Your ride from " + ride.getOrigin() + " to " + ride.getDestination() + " has been cancelled by the owner."
                );
                restTemplate.postForEntity("http://localhost:8082/notifications", new HttpEntity<>(note, headers), Void.class);
            }
        } catch (Exception e) { log.error("Failed to send cancellation notifications: {}", e.getMessage()); }
    }
    private void notifyJoinedOnUpdate(Ride ride, List<String> joinedEmpIds) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        final String jwt;
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String authHeader = req.getHeader("Authorization");
            jwt = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader : null;
        } else jwt = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            headers.set("Content-Type", "application/json");
            for (String je : joinedEmpIds) {
                NotificationService.NotificationRequest note = new NotificationService.NotificationRequest(
                        je,
                        "A ride from " + ride.getOrigin() + " to " + ride.getDestination() + " has been updated by the owner."
                );
                restTemplate.postForEntity("http://localhost:8082/notifications", new HttpEntity<>(note, headers), Void.class);
            }
        } catch (Exception e) { log.error("Failed to send update notifications: {}", e.getMessage()); }
    }

    // DTO MAPPING -------------------------------------------------
    private List<RideResponseDTO> mapRidesToDtoWithEmployees(List<Ride> rides, String defaultStatus) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        final String jwt;
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String authHeader = req.getHeader("Authorization");
            jwt = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader : null;
        } else jwt = null;
        return rides.stream().map(ride -> buildDto(ride, defaultStatus, jwt)).toList();
    }

    private RideResponseDTO buildDto(Ride ride, String defaultStatus, String jwt) {
        String ownerName = "Unknown";
        try {
            String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), EmployeeProfile.class);
            EmployeeProfile owner = response.getBody();
            if (owner != null) ownerName = owner.getName();
        } catch (Exception e) { log.error("Failed to fetch owner {}: {}", ride.getOwnerEmpId(), e.getMessage()); }

        List<JoinedEmployeeDTO> joinedEmployees = ride.getJoinedEmpIds().stream().map(empId -> mapEmployee(empId, jwt)).toList();
        List<JoinedEmployeeDTO> pendingEmployees = mapPending(ride.getPendingEmpIds(), jwt);
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
                .fare(ride.getFare() != null ? ride.getFare().toPlainString() : null)
                .status(ride.getStatus() != null ? ride.getStatus() : defaultStatus)
                .joinedEmployees(joinedEmployees)
                .instantBookingEnabled(ride.isInstantBookingEnabled())
                .pendingEmployees(pendingEmployees)
                .build();
    }

    private JoinedEmployeeDTO mapEmployee(String empId, String jwt) {
        try {
            String url = "http://localhost:8082/employee/" + empId;
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), EmployeeProfile.class);
            EmployeeProfile emp = response.getBody();
            if (emp != null) return new JoinedEmployeeDTO(emp.getEmpId(), emp.getName(), emp.getEmail());
        } catch (Exception e) { log.error("Failed to fetch employee {}: {}", empId, e.getMessage()); }
        return new JoinedEmployeeDTO(empId, "Unknown", "");
    }

    private List<JoinedEmployeeDTO> mapPending(List<String> pendingIds, String jwt) {
        if (pendingIds == null || pendingIds.isEmpty()) return List.of();
        return pendingIds.stream().map(id -> mapEmployee(id, jwt)).toList();
    }

    // PUBLIC LIST METHODS (using unified mapper) ------------------
    public List<RideResponseDTO> getRidesWithEmployeeDetailsByOwner(String ownerEmpId) {
        List<Ride> rides = getRidesByOwner(ownerEmpId).stream().filter(r -> r.getStatus() == null || "Active".equalsIgnoreCase(r.getStatus())).toList();
        return mapRidesToDtoWithEmployees(rides, "Active");
    }
    public List<RideResponseDTO> getAllRidesWithEmployeeDetails() { return mapRidesToDtoWithEmployees(getAllRides(), "Active"); }
    public List<RideResponseDTO> getActiveRidesWithEmployeeDetails() { return mapRidesToDtoWithEmployees(rideRepository.findByStatus("Active"), "Active"); }
    public List<RideResponseDTO> getJoinedRidesWithEmployeeDetails(String empId) {
        List<Ride> rides = getJoinedRides(empId).stream().filter(r -> r.getStatus() == null || "Active".equalsIgnoreCase(r.getStatus())).toList();
        return mapRidesToDtoWithEmployees(rides, "Active");
    }

    // HELPER DTO CLASSES -----------------------------------------
    @Data static class EmployeeProfile { private String empId; private String name; private String email; }
    @Data static class VehicleInfo { private Long id; private String status; private Integer capacity; private String registrationNumber; private String make; private String model; private String color; }
}
