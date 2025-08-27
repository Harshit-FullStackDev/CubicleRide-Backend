package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.RatingDtos;
import com.orangemantra.rideservice.model.Rating;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.RatingRepository;
import com.orangemantra.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepo;
    private final RideRepository rideRepo;

    private static final Set<String> ALLOWED_LABELS = Set.of(
            "Outstanding", "Good", "Okay", "Poor", "Very disappointing"
    );

    @Transactional
    public RatingDtos.RatingDTO create(String raterEmpId, RatingDtos.CreateRequest req) {
        if (req.getRideId() == null || req.getTargetEmpId() == null) throw new IllegalArgumentException("rideId & targetEmpId required");
        if (raterEmpId.equalsIgnoreCase(req.getTargetEmpId())) throw new IllegalArgumentException("Cannot rate yourself");
        if (req.getStars() < 1 || req.getStars() > 5) throw new IllegalArgumentException("stars must be 1..5");
        if (req.getLabel() != null && !req.getLabel().isBlank() && !ALLOWED_LABELS.contains(req.getLabel()))
            throw new IllegalArgumentException("Invalid label");

        Ride ride = rideRepo.findWithJoinedEmpIdsById(req.getRideId())
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        // allow rating once ride date is in past OR status != Active
        if (ride.getDate() != null && ride.getDate().isAfter(LocalDate.now()))
            throw new IllegalStateException("Ride not completed yet");

        boolean raterInRide = raterEmpId.equalsIgnoreCase(ride.getOwnerEmpId()) ||
                (ride.getJoinedEmpIds() != null && ride.getJoinedEmpIds().stream().anyMatch(id -> id != null && id.equalsIgnoreCase(raterEmpId)));
        boolean targetInRide = req.getTargetEmpId().equalsIgnoreCase(ride.getOwnerEmpId()) ||
                (ride.getJoinedEmpIds() != null && ride.getJoinedEmpIds().stream().anyMatch(id -> id != null && id.equalsIgnoreCase(req.getTargetEmpId())));
        if (!raterInRide || !targetInRide) throw new IllegalArgumentException("Both users must belong to ride");

        String direction = ride.getOwnerEmpId().equalsIgnoreCase(req.getTargetEmpId()) ? "DRIVER" : "PASSENGER";
        ratingRepo.findByRideIdAndRaterEmpIdAndTargetEmpId(req.getRideId(), raterEmpId, req.getTargetEmpId())
                .ifPresent(r -> { throw new IllegalStateException("Already rated this participant for this ride"); });

        Rating saved = ratingRepo.save(Rating.builder()
                .rideId(req.getRideId())
                .raterEmpId(raterEmpId)
                .targetEmpId(req.getTargetEmpId())
                .direction(direction)
                .stars(req.getStars())
                .label(req.getLabel())
                .comment(req.getComment())
                .createdAt(Instant.now())
                .build());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RatingDtos.RatingDTO> listGiven(String rater) {
        return ratingRepo.findByRaterEmpId(rater).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RatingDtos.RatingDTO> listReceived(String target) {
        return ratingRepo.findByTargetEmpId(target).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RatingDtos.SummaryDTO summary(String target) {
        List<Rating> all = ratingRepo.findByTargetEmpId(target);
        long total = all.size();
        long five=0,four=0,three=0,two=0,one=0; double sum=0;
        for (Rating r : all) {
            sum += r.getStars();
            switch (r.getStars()) {case 5 -> five++; case 4 -> four++; case 3 -> three++; case 2 -> two++; default -> one++;}
        }
        double avg = total==0?0: Math.round((sum/total)*10.0)/10.0;
        Map<String, Long> labelBreakdown = new LinkedHashMap<>();
        for (String lbl : ALLOWED_LABELS) labelBreakdown.put(lbl, 0L);
        for (RatingRepository.LabelCount lc : ratingRepo.countByLabelForTarget(target)) {
            labelBreakdown.put(lc.getLabel(), lc.getCnt());
        }
        List<RatingDtos.RatingDTO> recent = all.stream()
                .sorted(Comparator.comparing(Rating::getCreatedAt).reversed())
                .limit(10)
                .map(this::toDto).toList();
        return RatingDtos.SummaryDTO.builder()
                .empId(target).average(avg).total(total)
                .five(five).four(four).three(three).two(two).one(one)
                .labelBreakdown(labelBreakdown)
                .recent(recent)
                .build();
    }

    private RatingDtos.RatingDTO toDto(Rating r) {
        return RatingDtos.RatingDTO.builder()
                .id(r.getId()).rideId(r.getRideId()).raterEmpId(r.getRaterEmpId())
                .targetEmpId(r.getTargetEmpId()).direction(r.getDirection())
                .stars(r.getStars()).label(r.getLabel()).comment(r.getComment())
                .createdAt(r.getCreatedAt()).build();
    }

    @Transactional(readOnly = true)
    public boolean hasRating(Long rideId, String raterEmpId, String targetEmpId) {
        return ratingRepo.findByRideIdAndRaterEmpIdAndTargetEmpId(rideId, raterEmpId, targetEmpId).isPresent();
    }
}
