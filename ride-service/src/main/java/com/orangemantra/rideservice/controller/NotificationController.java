package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.model.Notification;
import com.orangemantra.rideservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repo;

    @GetMapping("/{empId}")
    public List<Notification> list(@PathVariable String empId, @RequestParam(name="limit", required=false) Integer limit) {
        if (limit != null && limit > 0) return repo.findByEmpIdOrderByCreatedAtDesc(empId, PageRequest.of(0, limit));
        return repo.findByEmpIdOrderByCreatedAtDesc(empId);
    }

    @GetMapping("/{empId}/count")
    public Map<String, Object> count(@PathVariable String empId) {
        long unread = repo.countByEmpIdAndReadFlagFalse(empId);
        return Map.of("unread", unread);
    }

    @PostMapping("/{empId}/mark-all-read")
    public Map<String,Object> markAllRead(@PathVariable String empId) {
        int updated = repo.markAllRead(empId);
        return Map.of("updated", updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Ad-hoc manual create (not for production) useful for testing UI
    @PostMapping("/test")
    public Notification create(@RequestParam String empId, @RequestParam String msg) {
        Notification n = Notification.builder().empId(empId).message(msg).type("TEST").createdAt(Instant.now()).build();
        return repo.save(n);
    }
}
