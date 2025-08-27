package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.RatingDtos;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.RatingRepository;
import com.orangemantra.rideservice.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class RatingServiceTest {
    private RatingRepository ratingRepo = Mockito.mock(RatingRepository.class);
    private RideRepository rideRepo = Mockito.mock(RideRepository.class);
    private RatingService service;

    @BeforeEach
    void setup() {
        service = new RatingService(ratingRepo, rideRepo);
    }

    @Test
    void createRejectsFutureRide() {
        Ride ride = Ride.builder().id(1L).date(LocalDate.now().plusDays(1)).ownerEmpId("d1").status("Active").build();
        when(rideRepo.findWithJoinedEmpIdsById(1L)).thenReturn(Optional.of(ride));
        RatingDtos.CreateRequest req = RatingDtos.CreateRequest.builder().rideId(1L).targetEmpId("d1").stars(5).build();
        assertThrows(IllegalStateException.class, () -> service.create("p1", req));
    }
}
