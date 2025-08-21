package com.orangemantra.rideservice.model;

import com.orangemantra.rideservice.util.StringCryptoConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(indexes = {
    @Index(name = "idx_ride_owner_status", columnList = "ownerEmpId,status"),
    @Index(name = "idx_ride_status", columnList = "status"),
    @Index(name = "idx_ride_date_status", columnList = "date,status")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmpId;

    private String origin;
    private String destination;

    private LocalDate date;
    private String arrivalTime;

    @Convert(converter = StringCryptoConverter.class)
    private String carDetails;

    private int totalSeats;
    private int availableSeats;

    // Fare per seat (currency). Nullable => free ride. Non-negative.
    private BigDecimal fare;

    @ElementCollection
    @Builder.Default
    private List<String> joinedEmpIds = new ArrayList<>();

    // If true passengers are auto-added. If false owner must approve each request.
    @Builder.Default
    private boolean instantBookingEnabled = true;

    // Pending employee IDs waiting for owner approval (used when instantBookingEnabled = false)
    @ElementCollection
    @Builder.Default
    private List<String> pendingEmpIds = new ArrayList<>();

    private String status; // Active, Expired, Cancelled
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Ride ride = (Ride) o;
        return getId() != null && Objects.equals(getId(), ride.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
