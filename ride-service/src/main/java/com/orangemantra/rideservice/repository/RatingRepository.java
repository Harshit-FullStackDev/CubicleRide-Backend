package com.orangemantra.rideservice.repository;

import com.orangemantra.rideservice.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByRideIdAndRaterEmpIdAndTargetEmpId(Long rideId, String raterEmpId, String targetEmpId);
    List<Rating> findByRaterEmpId(String raterEmpId);
    List<Rating> findByTargetEmpId(String targetEmpId);
    List<Rating> findByRideId(Long rideId);

    @Query("SELECT r.label AS label, COUNT(r) AS cnt FROM Rating r WHERE r.targetEmpId = :emp GROUP BY r.label")
    List<LabelCount> countByLabelForTarget(@Param("emp") String empId);

    interface LabelCount { String getLabel(); long getCnt(); }
}
