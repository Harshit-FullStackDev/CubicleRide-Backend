package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.model.Ride;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.orangemantra.rideservice.repository.RideRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Service
@AllArgsConstructor
public class NotificationService {
    private RestTemplate restTemplate;
    private RideRepository rideRepository;

    public void notifyRideOwnerOnJoin(Long rideId, String empId) {
        HttpServletRequest currentRequest =
                ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String authHeader = currentRequest.getHeader("Authorization");

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set("Authorization", authHeader);
        }
        headers.set("Content-Type", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8082/employee/" + empId + "/name",
                HttpMethod.GET,
                entity,
                String.class
        );
        String employeeName = response.getBody();
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        String ownerEmpId = ride.getOwnerEmpId();

        NotificationRequest notification = new NotificationRequest(
                ownerEmpId,
                "Employee " + employeeName + " joined your ride."
        );
        HttpEntity<NotificationRequest> notificationEntity = new HttpEntity<>(notification, headers);

        restTemplate.postForEntity(
                "http://localhost:8082/notifications",
                notificationEntity,
                Void.class
        );
    }

    public static class NotificationRequest {
        public String userId;
        public String message;
        public NotificationRequest(String userId, String message) {
            this.userId = userId;
            this.message = message;
        }
    }
}