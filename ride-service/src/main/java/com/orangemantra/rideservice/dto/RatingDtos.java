package com.orangemantra.rideservice.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RatingDtos {
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        private Long rideId;
        private String targetEmpId; // person being rated
        private int stars; // 1..5
        private String label; // Optional predefined label
        private String comment; // Optional free text
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RatingDTO {
        private Long id; private Long rideId; private String raterEmpId; private String targetEmpId;
        private String direction; private int stars; private String label; private String comment; private Instant createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SummaryDTO {
        private String empId;
        private double average; // rounded to 1 decimal
        private long total;
        private long five; private long four; private long three; private long two; private long one;
        private java.util.Map<String, Long> labelBreakdown;
        private List<RatingDTO> recent; // latest few
    }
}
