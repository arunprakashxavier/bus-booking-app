// src/main/java/com/guvi/busapp/model/ScheduledTrip.java
package com.guvi.busapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; // Use individual annotations
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "scheduled_trips")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"bus", "route", "seatStatus"}) // Exclude relationships and large collections
public class ScheduledTrip {

    public enum SeatStatus { AVAILABLE, BOOKED, LOCKED, UNAVAILABLE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @NotNull
    @FutureOrPresent
    private LocalDate departureDate;

    @NotNull
    private LocalTime departureTime;

    @NotNull
    private LocalTime arrivalTime;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal fare;

    @NotNull
    @Min(0)
    private Integer availableSeats;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "trip_seat_status", joinColumns = @JoinColumn(name = "trip_id"))
    @MapKeyColumn(name = "seat_number")
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Map<String, SeatStatus> seatStatus = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledTrip that = (ScheduledTrip) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
}