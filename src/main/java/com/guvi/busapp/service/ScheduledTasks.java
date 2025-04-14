package com.guvi.busapp.service;

import com.guvi.busapp.model.Booking;
import com.guvi.busapp.model.Passenger;
import com.guvi.busapp.model.ScheduledTrip;
import com.guvi.busapp.repository.BookingRepository;
import com.guvi.busapp.repository.ScheduledTripRepository;
import org.slf4j.Logger;
import com.guvi.busapp.exception.ResourceNotFoundException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling; // Import EnableScheduling
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@EnableScheduling // Enable scheduling for methods in this component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    // Define timeout in minutes (can be externalized to application.properties)
    private static final int LOCK_EXPIRY_MINUTES = 15; // e.g., 15 minutes

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ScheduledTripRepository scheduledTripRepository;

    // Scheduled task to run periodically (e.g., every minute)
    // cron = "0 * * * * ?" means run at the start of every minute
    @Scheduled(cron = "0 * * * * ?")
    @Transactional // Make the entire cleanup process atomic
    public void releaseExpiredLockedSeats() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(LOCK_EXPIRY_MINUTES);
        logger.debug("Running expired seat lock cleanup task. Checking for bookings older than {}", expiryTime);

        // Find PENDING bookings created before the expiry time
        // NOTE: This requires a custom query in BookingRepository
        List<Booking> expiredPendingBookings = bookingRepository.findExpiredPendingBookings(expiryTime, Booking.BookingStatus.PENDING);

        if (expiredPendingBookings.isEmpty()) {
            logger.debug("No expired PENDING bookings found.");
            return;
        }

        logger.info("Found {} expired PENDING bookings to process.", expiredPendingBookings.size());

        for (Booking booking : expiredPendingBookings) {
            logger.warn("Processing expired PENDING booking ID: {}", booking.getId());
            try {
                // Fetch the associated trip with a lock
                ScheduledTrip trip = scheduledTripRepository.findByIdForUpdate(booking.getScheduledTrip().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", booking.getScheduledTrip().getId()));

                Map<String, ScheduledTrip.SeatStatus> seatStatusMap = trip.getSeatStatus();
                if (seatStatusMap == null) {
                    logger.error("Seat status map is null for trip ID {} during cleanup! Skipping booking ID {}.", trip.getId(), booking.getId());
                    // Mark booking as failed anyway?
                    booking.setStatus(Booking.BookingStatus.FAILED);
                    bookingRepository.save(booking);
                    logger.error("Marked booking ID {} as FAILED due to missing seat map on trip.", booking.getId());
                    continue; // Skip to next booking
                }

                // Get seats associated with this expired booking
                Set<String> seatsToRelease = booking.getPassengers().stream()
                        .map(Passenger::getSeatNumber)
                        .collect(Collectors.toSet());

                int releasedCount = 0;
                for (String seatNum : seatsToRelease) {
                    // Only release if the seat is currently LOCKED
                    if (seatStatusMap.get(seatNum) == ScheduledTrip.SeatStatus.LOCKED) {
                        seatStatusMap.put(seatNum, ScheduledTrip.SeatStatus.AVAILABLE);
                        releasedCount++;
                    } else {
                        // If it's already BOOKED or AVAILABLE, something else happened (e.g., webhook processed, another cleanup ran?)
                        // Or maybe it failed payment and was already reverted. Log this inconsistency.
                        logger.warn("Seat {} for expired booking ID {} on trip {} was not in LOCKED state (Actual: {}). Still marking booking as FAILED.",
                                seatNum, booking.getId(), trip.getId(), seatStatusMap.get(seatNum));
                    }
                }

                // Update available seats count only if seats were actually released
                if (releasedCount > 0) {
                    int currentAvailable = trip.getAvailableSeats() != null ? trip.getAvailableSeats() : 0;
                    trip.setAvailableSeats(currentAvailable + releasedCount);
                    scheduledTripRepository.save(trip);
                    logger.info("Released {} seats and updated available count for trip ID {} due to expired booking ID {}.", releasedCount, trip.getId(), booking.getId());
                } else {
                    logger.warn("No seats found in LOCKED state to release for expired booking ID {} on trip {}.", booking.getId(), trip.getId());
                }

                // Update the booking status to FAILED (or CANCELLED_BY_SYSTEM)
                booking.setStatus(Booking.BookingStatus.FAILED); // Or a more specific status
                bookingRepository.save(booking);
                logger.info("Marked expired booking ID {} as {}.", booking.getId(), booking.getStatus());

            } catch (ResourceNotFoundException e) {
                logger.error("ScheduledTrip not found for expired booking ID {} during cleanup. Marking booking as FAILED.", booking.getId(), e);
                booking.setStatus(Booking.BookingStatus.FAILED);
                bookingRepository.save(booking);
            } catch (Exception e) {
                // Catch unexpected errors during processing of a single booking
                logger.error("Error processing expired booking ID {}: {}", booking.getId(), e.getMessage(), e);
                // Continue to the next booking
            }
        }
        logger.debug("Finished expired seat lock cleanup task.");
    }
}