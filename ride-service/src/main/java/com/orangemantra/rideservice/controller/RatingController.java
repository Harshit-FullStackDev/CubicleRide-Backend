package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.dto.RatingDtos;
import com.orangemantra.rideservice.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ride/ratings")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @PostMapping
    public RatingDtos.RatingDTO create(@RequestBody RatingDtos.CreateRequest req) {
        String me = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return ratingService.create(me, req);
    }

    @GetMapping("/given")
    public List<RatingDtos.RatingDTO> given() {
        String me = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return ratingService.listGiven(me);
    }

    @GetMapping("/received/{empId}")
    public List<RatingDtos.RatingDTO> received(@PathVariable("empId") String empId) {
        return ratingService.listReceived(empId);
    }

    @GetMapping("/summary/{empId}")
    public RatingDtos.SummaryDTO summary(@PathVariable("empId") String empId) {
        return ratingService.summary(empId);
    }

    @GetMapping("/exists")
    public java.util.Map<String,Object> exists(@RequestParam Long rideId, @RequestParam String targetEmpId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        boolean exists = ratingService.hasRating(rideId, me, targetEmpId);
        return java.util.Map.of("exists", exists);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() { return ResponseEntity.ok(java.util.Map.of("status","UP","component","ratings")); }
}
