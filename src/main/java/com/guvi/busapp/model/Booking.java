// src/main/java/com/guvi/busapp/model/Booking.java
package com.guvi.busapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; // Use individual annotations
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects; // Import for manual equals/hashCode if needed later
import java.util.Set;

@Entity
@Table(name = "bookings")
@Getter // Add Getter
@Setter // Add Setter
@NoArgsConstructor // Add NoArgsConstructor
@ToString(exclude = {"user", "scheduledTrip", "passengers"}) // Exclude relationships from toString
public class Booking {

    // Enum for Seat Status (moved from ScheduledTrip for clarity if needed here, or keep there)
    // Let's assume it stays in ScheduledTrip for now.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The user who made the booking

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private ScheduledTrip scheduledTrip; // The specific trip being booked

    @NotNull
    private LocalDateTime bookingTime; // When the booking was made

    @NotNull
    @Min(1)
    private Integer numberOfSeats; // How many seats were booked in this transaction

    @NotNull
    private BigDecimal totalFare;

    @Enumerated(EnumType.STRING)
    private BookingStatus status; // e.g., CONFIRMED, CANCELLED

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Passenger> passengers = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        bookingTime = LocalDateTime.now();
        // Set default status - PENDING if payment integration is next
        // status = BookingStatus.CONFIRMED; // Keep CONFIRMED if skipping payment for now
        status = BookingStatus.PENDING; // Change to PENDING anticipating payment step
    }

    public enum BookingStatus {
        PENDING, // Waiting for payment
        CONFIRMED, // Payment successful
        CANCELLED,
        FAILED    // Payment failed
    }

    // --- IMPORTANT: DO NOT use Lombok @EqualsAndHashCode on Entities with relationships ---
    // Implement manually based on ID if absolutely necessary AFTER entity is persisted
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        // Use ID for equality check, ensure ID is not null for persisted entities
        return id != null && Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        // Use ID for hashcode, or a constant for transient entities
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
}