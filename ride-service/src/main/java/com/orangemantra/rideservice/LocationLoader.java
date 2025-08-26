package com.orangemantra.rideservice;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.orangemantra.rideservice.model.Location;
import com.orangemantra.rideservice.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocationLoader implements CommandLineRunner {
    private final LocationRepository repo;
    @Override
    public void run(String... args) {
        record Preset(String name,double lat,double lng){}
        List<Preset> presets = List.of(
                new Preset("{OM}",28.4132796,77.0421934), // Gurgaon (company HQ placeholder)
                new Preset("Rajiv Chowk",28.4452149,77.0334382),
                new Preset("Subhash Chowk",28.4126,77.0421),
                new Preset("Ambience Mall",28.5045,77.0956),
                new Preset("MG Road",28.4796,77.0720)
        );
        presets.forEach(p -> repo.findByName(p.name())
                .or(() -> Optional.of(repo.save(Location.builder()
                        .name(p.name())
                        .latitude(p.lat())
                        .longitude(p.lng())
                        .build()))));
    }
}
