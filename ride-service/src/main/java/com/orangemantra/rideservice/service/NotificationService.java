package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.messaging.NotificationProducer;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.model.Notification;
import com.orangemantra.rideservice.repository.RideRepository;
import com.orangemantra.rideservice.repository.NotificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    private final RideRepository rideRepository;
    private final NotificationProducer notificationProducer;
    private final NotificationRepository notificationRepository;

    @Async
    public void notifyRideOwnerOnJoin(Long rideId, String empId) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
            HttpServletRequest currentRequest = attrs.getRequest();
            String authHeader = currentRequest.getHeader("Authorization");

            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null && authHeader.startsWith("Bearer ")) headers.set("Authorization", authHeader);
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

            String message = "Employee " + employeeName + " joined your ride.";
            notificationProducer.send(ownerEmpId, message);
            persist(ownerEmpId, message, "JOIN");
        } catch (Exception e) {
            log.warn("Notification dispatch failed rideId={}, empId={}, err={}", rideId, empId, e.getMessage());
        }
    }

    public void persist(String empId, String message, String type) {
        if (notificationRepository == null) return; // defensive
        try {
            Notification n = new Notification();
            n.setEmpId(empId);
            n.setMessage(message);
            n.setType(type);
            n.setCreatedAt(java.time.Instant.now());
            n.setReadFlag(false);
            notificationRepository.save(n);
        } catch (Exception ex) {
            log.warn("Persist notification failed empId={}, msg={}", empId, ex.getMessage());
        }
    }
}