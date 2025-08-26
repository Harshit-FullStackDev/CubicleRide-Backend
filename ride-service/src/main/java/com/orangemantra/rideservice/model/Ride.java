package com.orangemantra.rideservice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.proxy.HibernateProxy;

import com.orangemantra.rideservice.util.StringCryptoConverter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

    // Precise coordinates (WGS84) for origin & destination if provided via wizard.
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;

    // Selected route polyline (encoded polyline or geojson simplified). For now store as text (encoded polyline or JSON string)
    @Lob
    private String routeGeometry;
    // Distance (meters) and duration (seconds) from routing engine
    private Integer routeDistanceMeters;
    private Integer routeDurationSeconds;

    private LocalDate date;
    private String arrivalTime;

    @Convert(converter = StringCryptoConverter.class)
    private String carDetails;

    private int totalSeats;
    private int availableSeats;

    // Fare per seat (currency). Nullable => free ride. Non-negative.
    private BigDecimal fare;

    // Optional public comment/note shown to passengers when publishing (e.g. meeting instructions)
    @Column(length = 500)
    private String driverNote;

    @ElementCollection
    @Builder.Default
    private List<String> joinedEmpIds = new ArrayList<>();

    // Seats booked per joined employee (empId -> seats). Aligns with joinedEmpIds list.
    @ElementCollection
    @CollectionTable(name = "ride_joined_seats", joinColumns = @JoinColumn(name = "ride_id"))
    @MapKeyColumn(name = "emp_id")
    @Column(name = "seats")
    @Builder.Default
    private java.util.Map<String, Integer> joinedSeats = new java.util.HashMap<>();

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
