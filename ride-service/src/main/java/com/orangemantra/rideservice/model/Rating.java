package com.orangemantra.rideservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ride_ratings", indexes = {
        @Index(name = "idx_rating_target", columnList = "targetEmpId"),
        @Index(name = "idx_rating_rater", columnList = "raterEmpId"),
        @Index(name = "idx_rating_ride", columnList = "rideId"),
        @Index(name = "uk_rating_once", columnList = "rideId,raterEmpId,targetEmpId", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rideId;
    private String raterEmpId;   // auth user giving rating
    private String targetEmpId;  // the person being rated (driver or passenger)

    // DRIVER when rating the driver (owner) / PASSENGER when rating a passenger
    @Column(length = 20)
    private String direction;

    private int stars; // 1..5
    @Column(length = 30)
    private String label; // Outstanding, Good, Okay, Poor, Very disappointing
    @Column(length = 600)
    private String comment;

    private Instant createdAt;
}
