// src/main/java/com/guvi/busapp/repository/ScheduledTripRepository.java
package com.guvi.busapp.repository;

import com.guvi.busapp.model.Route;
import com.guvi.busapp.model.ScheduledTrip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledTripRepository extends JpaRepository<ScheduledTrip, Long> {

    // Find trips based on route and date with available seats
    @Query("SELECT st FROM ScheduledTrip st WHERE st.route = :route AND st.departureDate = :date AND st.availableSeats > 0")
    List<ScheduledTrip> findAvailableTripsByRouteAndDate(@Param("route") Route route, @Param("date") LocalDate date);

    // Find trips between two locations on a specific date with available seats
    @Query("SELECT st FROM ScheduledTrip st JOIN st.route r WHERE r.origin = :origin AND r.destination = :destination AND st.departureDate = :date AND st.availableSeats > 0")
    List<ScheduledTrip> findAvailableTripsByLocationAndDate(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("date") LocalDate date);

    // Find by ID with Pessimistic Write Lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM ScheduledTrip st WHERE st.id = :id")
    Optional<ScheduledTrip> findByIdForUpdate(@Param("id") Long id);

}