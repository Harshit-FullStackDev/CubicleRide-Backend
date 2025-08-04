package com.orangemantra.rideservice;

import com.orangemantra.rideservice.model.Location;
import com.orangemantra.rideservice.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocationLoader implements CommandLineRunner {
    private final LocationRepository repo;
    @Override
    public void run(String... args) {
        List<String> presets = List.of("{OM}", "Rajiv Chowk", "Subhash Chowk", "Ambience Mall","MG Road");
        presets.forEach(name ->
                repo.findByName(name).or(() -> Optional.of(repo.save(new Location(null, name))))
        );
    }
}
