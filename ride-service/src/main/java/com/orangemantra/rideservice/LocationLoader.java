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
                new Preset("Bus Stand",28.4656413,77.0334509), // Gurgaon (company HQ placeholder)
                new Preset("Rajiv Chowk",28.4452149,77.0334382),
                new Preset("Subhash Chowk",28.4286913,77.0370213),
                new Preset("Ambience Mall",28.5045,77.0956),
                new Preset("Iffco Chwok",28.4772267,77.0681124),
                new Preset("Sikandarpur",28.4809816,77.0946874),
                new Preset("Artemis Hospital",28.4321631,77.0731265),
                new Preset("Dlf CyberHub",28.4941311,77.0918051),
                new Preset("Gurgaon Railway Station",28.4884127,77.0110367)
        );
        presets.forEach(p -> repo.findByName(p.name())
                .or(() -> Optional.of(repo.save(Location.builder()
                        .name(p.name())
                        .latitude(p.lat())
                        .longitude(p.lng())
                        .build()))));
    }
}
