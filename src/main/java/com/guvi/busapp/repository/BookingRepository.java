// src/main/java/com/guvi/busapp/repository/BookingRepository.java
package com.guvi.busapp.repository;

import com.guvi.busapp.model.Booking;
import com.guvi.busapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // Import LocalDateTime
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Find bookings by user, ordered by booking time descending
    List<Booking> findByUserOrderByBookingTimeDesc(User user);

    // **** ADDED: Find PENDING bookings created before a certain time ****
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingTime < :expiryTime")
    List<Booking> findExpiredPendingBookings(@Param("expiryTime") LocalDateTime expiryTime, @Param("status") Booking.BookingStatus status);
}