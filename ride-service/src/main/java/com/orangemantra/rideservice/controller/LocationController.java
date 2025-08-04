package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.model.Location;
import com.orangemantra.rideservice.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController {
    private final LocationRepository locationRepository;

    @GetMapping("/locations")
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }
}
