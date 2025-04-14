// src/main/java/com/guvi/busapp/model/Passenger.java
package com.guvi.busapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; // Use individual annotations
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"booking"}) // Exclude back-reference
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull // Ensure booking is set
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking; // Link back to the booking

    @NotBlank
    private String name;

    @NotNull
    @Min(1)
    private Integer age;

    @NotBlank
    private String gender;

    @NotBlank // Seat number should be assigned
    private String seatNumber;

    // Keep constructor if needed, ensure booking is passed
    public Passenger(Booking booking, String name, Integer age, String gender, String seatNumber) {
        this.booking = booking;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNumber = seatNumber; // Assign seat number
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passenger passenger = (Passenger) o;
        return id != null && Objects.equals(id, passenger.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
}