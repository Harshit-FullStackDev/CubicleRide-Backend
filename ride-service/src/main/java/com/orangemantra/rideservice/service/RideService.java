package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.JoinedEmployeeDTO;
import com.orangemantra.rideservice.dto.RideResponseDTO;
import com.orangemantra.rideservice.util.JwtUtil;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RideService {
    private final RideRepository rideRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final ChatService chatService;
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
            VehicleInfo v = fetchVehicleInfoCached(ride.getOwnerEmpId(), authHeader);
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
                    if (!sb.isEmpty()) sb.append('(').append(v.getRegistrationNumber()).append(')');
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
        // Conflict check: stop early on first active future ride (reduces iterations)
        List<Ride> activeOwned = rideRepository.findByOwnerEmpIdAndStatus(ride.getOwnerEmpId(), "Active");
        LocalDateTime now = LocalDateTime.now();
        for (Ride r : activeOwned) {
            if (r.getDate() == null || r.getArrivalTime() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
            try {
                LocalTime at = LocalTime.parse(r.getArrivalTime());
                if (now.isBefore(LocalDateTime.of(r.getDate(), at))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_RIDE_CONFLICT_MSG);
                }
            } catch (Exception e) {
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
            assert ride.getPendingEmpIds() != null;
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
        // notify chat closure for this ride
        try { chatService.notifyRideClosed(existing); } catch (Exception ignored) {}
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
        if (activeRides.isEmpty()) return;
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        List<Ride> toExpire = new ArrayList<>();
        for (Ride r : activeRides) {
            if (r.getDate() == null) continue;
            boolean expire = r.getDate().isBefore(today);
            if (!expire && r.getDate().isEqual(today) && r.getArrivalTime() != null) {
                try {
                    expire = !LocalTime.parse(r.getArrivalTime()).isAfter(nowTime);
                } catch (Exception ignored) {}
            }
            if (expire) {
                r.setStatus("Expired");
                r.setUpdatedAt(LocalDateTime.now());
                toExpire.add(r);
            }
        }
        if (!toExpire.isEmpty()) {
            rideRepository.saveAll(toExpire);
            // broadcast closure events
            for (Ride r : toExpire) { try { chatService.notifyRideClosed(r); } catch (Exception ignored) {} }
        }
    }

    public List<RideResponseDTO> getPublishedRideHistory(String ownerEmpId) {
        expirePastRidesInternal();
        List<Ride> allOwner = rideRepository.findByOwnerEmpId(ownerEmpId);
        List<Ride> history = allOwner.stream()
                .filter(r -> r.getStatus() != null && ("Expired".equalsIgnoreCase(r.getStatus()) || "Cancelled".equalsIgnoreCase(r.getStatus())))
                .toList();
        return mapRidesToDtoWithEmployees(history, "Expired");
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
    if (rides.isEmpty()) return List.of();
    return buildDtosBatch(rides, defaultStatus, jwt);
    }

    private RideResponseDTO buildDto(Ride ride, String defaultStatus, String jwt) {
        String ownerName = "Unknown";
        String ownerPhone = null;
        String viewerEmpId = null;
        if (jwt != null) {
            try {
                String token = jwt.replace("Bearer ", "").trim();
                viewerEmpId = jwtUtil.extractEmpId(token);
            } catch (Exception ignored) {}
        }
        try {
            String url = "http://localhost:8082/employee/" + ride.getOwnerEmpId();
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            ResponseEntity<EmployeeProfile> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), EmployeeProfile.class);
            EmployeeProfile owner = response.getBody();
            if (owner != null) {
                ownerName = owner.getName();
                ownerPhone = owner.getPhone();
            }
        } catch (Exception e) { log.error("Failed to fetch owner {}: {}", ride.getOwnerEmpId(), e.getMessage()); }

        boolean viewerIsOwner = viewerEmpId != null && viewerEmpId.equals(ride.getOwnerEmpId());
        boolean viewerJoined = viewerEmpId != null && ride.getJoinedEmpIds().contains(viewerEmpId);
        boolean canSeeOwnerPhone = viewerIsOwner || viewerJoined; // joined implies approved already

    List<JoinedEmployeeDTO> joinedEmployees = ride.getJoinedEmpIds().stream().map(empId -> mapEmployeeCached(empId, jwt)).toList();
    List<JoinedEmployeeDTO> pendingEmployees = mapPending(ride.getPendingEmpIds(), jwt);

        // Apply phone visibility rules
        if (!canSeeOwnerPhone) ownerPhone = null; // hide owner's phone if not joined/owner

        for (JoinedEmployeeDTO je : joinedEmployees) {
            if (viewerIsOwner) {
                // owner sees all joined employees' phones
                continue;
            }
            // Non-owner viewers: only show their own phone (optional) and hide others
            if (viewerEmpId == null || !viewerEmpId.equals(je.getEmpId())) {
                je.setPhone(null);
            }
        }
        // Pending employees' phones never shown until approved
        for (JoinedEmployeeDTO pe : pendingEmployees) { pe.setPhone(null); }

        return RideResponseDTO.builder()
                .id(ride.getId())
                .ownerEmpId(ride.getOwnerEmpId())
                .ownerName(ownerName)
                .ownerPhone(ownerPhone)
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

    private final Map<String, JoinedEmployeeDTO> employeeCache = new ConcurrentHashMap<>();
    private final Map<String, VehicleInfo> vehicleCache = new ConcurrentHashMap<>();

    private JoinedEmployeeDTO mapEmployeeCached(String empId, String jwt) {
        return employeeCache.computeIfAbsent(empId, id -> fetchEmployee(id, jwt));
    }

    private JoinedEmployeeDTO fetchEmployee(String empId, String jwt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null) headers.set("Authorization", jwt);
            ResponseEntity<EmployeeProfile> response = restTemplate.exchange("http://localhost:8082/employee/" + empId, HttpMethod.GET, new HttpEntity<>(headers), EmployeeProfile.class);
            EmployeeProfile emp = response.getBody();
            if (emp != null) return new JoinedEmployeeDTO(emp.getEmpId(), emp.getName(), emp.getEmail(), emp.getPhone());
        } catch (Exception e) { log.error("Failed to fetch employee {}: {}", empId, e.getMessage()); }
        return new JoinedEmployeeDTO(empId, "Unknown", "", null);
    }

    private List<JoinedEmployeeDTO> mapPending(List<String> pendingIds, String jwt) {
        if (pendingIds == null || pendingIds.isEmpty()) return List.of();
        return pendingIds.stream().map(id -> mapEmployeeCached(id, jwt)).toList();
    }

    private VehicleInfo fetchVehicleInfoCached(String empId, String jwt) {
        return vehicleCache.computeIfAbsent(empId, id -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                if (jwt != null) headers.set("Authorization", jwt);
                ResponseEntity<VehicleInfo> resp = restTemplate.exchange("http://localhost:8082/vehicle/" + empId, HttpMethod.GET, new HttpEntity<>(headers), VehicleInfo.class);
                return resp.getBody();
            } catch (Exception e) {
                log.error("Failed vehicle fetch {}: {}", empId, e.getMessage());
                return null;
            }
        });
    }

    private List<RideResponseDTO> buildDtosBatch(List<Ride> rides, String defaultStatus, String jwt) {
        // Pre-fetch owner profiles distinct to minimize remote calls
        Set<String> ownerIds = rides.stream().map(Ride::getOwnerEmpId).collect(Collectors.toSet());
        for (String ownerId : ownerIds) { employeeCache.computeIfAbsent(ownerId, id -> fetchEmployee(id, jwt)); }
        List<RideResponseDTO> result = new ArrayList<>(rides.size());
        for (Ride ride : rides) { result.add(buildDto(ride, defaultStatus, jwt)); }
        return result;
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
    @Data static class EmployeeProfile { private String empId; private String name; private String email; private String phone; }
    @Data static class VehicleInfo { private Long id; private String status; private Integer capacity; private String registrationNumber; private String make; private String model; private String color; }
}
