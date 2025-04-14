// src/main/java/com/guvi/busapp/model/Bus.java
package com.guvi.busapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; // Use individual annotations
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "buses")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"amenities"}) // Exclude collections if they can be large or cause issues
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String busNumber;

    @NotBlank
    private String operatorName;

    @NotBlank
    private String busType;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bus_amenities", joinColumns = @JoinColumn(name = "bus_id"))
    @Column(name = "amenity")
    private Set<String> amenities = new HashSet<>();

    @Column(name = "seat_layout", length = 500)
    private String seatLayout;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bus bus = (Bus) o;
        return id != null && Objects.equals(id, bus.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
}