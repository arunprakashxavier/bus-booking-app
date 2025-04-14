// src/main/java/com/guvi/busapp/service/PaymentServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.model.Booking;
import com.guvi.busapp.model.Passenger;
import com.guvi.busapp.model.ScheduledTrip;
import com.guvi.busapp.repository.BookingRepository;
import com.guvi.busapp.repository.ScheduledTripRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent; // Keep for createPaymentIntent return type
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    // Use @Value now that property loading should be fixed
    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ScheduledTripRepository scheduledTripRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        logger.info("Stripe API Key initialized.");
    }

    @Override
    public PaymentIntent createPaymentIntent(Long bookingId, Long amount, String currency, String userEmail) throws StripeException {
        // ... (no changes needed here) ...
        logger.info("Creating Stripe Payment Intent for booking ID: {}, Amount: {}, Currency: {}", bookingId, amount, currency);
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .putMetadata("booking_id", String.valueOf(bookingId))
                .putMetadata("user_email", userEmail)
                .setDescription("Bus Ticket Booking #" + bookingId)
                .build();
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        logger.info("Stripe Payment Intent created successfully: {}", paymentIntent.getId());
        return paymentIntent;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String sigHeader) {
        // This verification happens in the controller now before calling handlers
        // We could potentially remove this method or keep it for utility
        // For now, let's keep it but note it's duplicated logic with controller
        try {
            Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            return true;
        } catch (SignatureVerificationException e) {
            return false;
        } catch (Exception e){
            logger.error("Error during signature construction in service (should not be primary verification): {}", e.getMessage());
            return false;
        }
    }

    // --- UPDATED Webhook Handlers ---

    @Override
    @Transactional // Ensure atomicity
    public void handlePaymentSuccess(String paymentIntentId, Long bookingId, Long amount, String currency) {
        logger.info("Handling PaymentIntent Succeeded: PI_ID={}, Booking_ID={}", paymentIntentId, bookingId);
        // bookingId is already extracted and validated in the controller

        try {
            // Retrieve the booking using the validated bookingId
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", "ID", bookingId));

            // Idempotency check
            if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
                logger.warn("Webhook Warning: Booking ID {} is already confirmed. Ignoring duplicate event for PI ID: {}", bookingId, paymentIntentId);
                return;
            }
            if (booking.getStatus() != Booking.BookingStatus.PENDING) {
                logger.warn("Webhook Warning: Received success event for booking ID {} with status {}, expected PENDING. Ignoring. PI ID: {}", bookingId, booking.getStatus(), paymentIntentId);
                return;
            }

            // Optional: Verify amount (using amount passed from controller)
            long expectedAmount = booking.getTotalFare().multiply(new BigDecimal("100")).longValueExact();
            if (amount != null && !amount.equals(expectedAmount)) {
                logger.warn("Webhook Warning: Amount mismatch for Booking ID {}. Expected: {}, Received: {}. PI ID: {}. Processing anyway...",
                        bookingId, expectedAmount, amount, paymentIntentId);
            }

            // Update Booking Status
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            logger.info("Booking ID {} status updated to CONFIRMED.", bookingId);

            // Update Scheduled Trip Seats
            ScheduledTrip trip = scheduledTripRepository.findByIdForUpdate(booking.getScheduledTrip().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", booking.getScheduledTrip().getId()));

            Map<String, ScheduledTrip.SeatStatus> seatStatusMap = trip.getSeatStatus();
            if (seatStatusMap == null) {
                logger.error("Webhook Error: Seat status map is null for trip ID {}!", trip.getId());
            } else {
                Set<String> bookedSeatNumbers = booking.getPassengers().stream()
                        .map(Passenger::getSeatNumber)
                        .collect(Collectors.toSet());
                int updatedCount = 0;
                for (String seatNum : bookedSeatNumbers) {
                    if (seatStatusMap.containsKey(seatNum)) {
                        seatStatusMap.put(seatNum, ScheduledTrip.SeatStatus.BOOKED);
                        updatedCount++;
                    } else {
                        logger.warn("Webhook Warning: Seat {} for booking ID {} not found in trip {} seat map during confirmation.", seatNum, bookingId, trip.getId());
                    }
                }
                scheduledTripRepository.save(trip);
                logger.info("Updated status to BOOKED for {} seats on trip ID {}.", updatedCount, trip.getId());
            }

            // Trigger Email Notification
            Booking confirmedBookingForEmail = bookingRepository.findById(bookingId).orElse(null); // Re-fetch fresh data if needed
            if (emailService != null && confirmedBookingForEmail != null) {
                try {
                    if (confirmedBookingForEmail.getUser() != null) confirmedBookingForEmail.getUser().getEmail();
                    if (confirmedBookingForEmail.getScheduledTrip() != null) {
                        if (confirmedBookingForEmail.getScheduledTrip().getBus() != null) confirmedBookingForEmail.getScheduledTrip().getBus().getId();
                        if (confirmedBookingForEmail.getScheduledTrip().getRoute() != null) confirmedBookingForEmail.getScheduledTrip().getRoute().getId();
                    }
                    confirmedBookingForEmail.getPassengers().size();

                    logger.info("Attempting to send confirmation email for booking ID: {}", confirmedBookingForEmail.getId());
                    emailService.sendBookingConfirmation(confirmedBookingForEmail);
                } catch (Exception emailEx) {
                    logger.error("Error triggering confirmation email for booking ID {}: {}", bookingId, emailEx.getMessage(), emailEx);
                }
            } else if (emailService == null) {
                logger.warn("EmailService not available. Skipping confirmation email for booking ID: {}", bookingId);
            }

        } catch (ResourceNotFoundException e) {
            logger.error("Webhook Error: Resource not found while processing success for PI ID: {}. Message: {}", paymentIntentId, e.getMessage());
        } catch (Exception e) {
            logger.error("Webhook Error: Unexpected error handling payment success for PI ID: {}. Error: {}", paymentIntentId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional // Ensure atomicity
    public void handlePaymentFailure(String paymentIntentId, Long bookingId) {
        logger.warn("Handling PaymentIntent Failed: PI_ID={}, Booking_ID={}", paymentIntentId, bookingId);
        // bookingId is already extracted and validated in the controller

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", "ID", bookingId));

            if (booking.getStatus() != Booking.BookingStatus.PENDING) {
                logger.warn("Webhook Warning: Received failure event for booking ID {} with status {}, expected PENDING. Ignoring. PI ID: {}", bookingId, booking.getStatus(), paymentIntentId);
                return;
            }

            // Update Booking Status
            booking.setStatus(Booking.BookingStatus.FAILED);
            bookingRepository.save(booking);
            logger.info("Booking ID {} status updated to FAILED.", bookingId);

            // Update Scheduled Trip Seats - Revert Lock to Available
            ScheduledTrip trip = scheduledTripRepository.findByIdForUpdate(booking.getScheduledTrip().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", booking.getScheduledTrip().getId()));

            Map<String, ScheduledTrip.SeatStatus> seatStatusMap = trip.getSeatStatus();
            if (seatStatusMap == null) {
                logger.error("Webhook Error: Seat status map is null for trip ID {} during failure handling!", trip.getId());
            } else {
                Set<String> seatsToRelease = booking.getPassengers().stream()
                        .map(Passenger::getSeatNumber)
                        .collect(Collectors.toSet());
                int releasedCount = 0;
                for (String seatNum : seatsToRelease) {
                    if (seatStatusMap.get(seatNum) == ScheduledTrip.SeatStatus.LOCKED) {
                        seatStatusMap.put(seatNum, ScheduledTrip.SeatStatus.AVAILABLE);
                        releasedCount++;
                    } else {
                        logger.warn("Webhook Warning: Seat {} for failed booking ID {} on trip {} was not in LOCKED state (Actual: {}). Not changing status or available count.",
                                seatNum, bookingId, trip.getId(), seatStatusMap.get(seatNum));
                    }
                }
                if(releasedCount > 0) {
                    int currentAvailable = trip.getAvailableSeats() != null ? trip.getAvailableSeats() : 0;
                    trip.setAvailableSeats(currentAvailable + releasedCount);
                    scheduledTripRepository.save(trip);
                    logger.info("Reverted status to AVAILABLE for {} seats and updated available count for trip ID {}.", releasedCount, trip.getId());
                } else {
                    logger.warn("No seats found in LOCKED state to release for failed booking ID {} on trip {}.", bookingId, trip.getId());
                }
            }
            // TODO: Notify user of payment failure?
        } catch (ResourceNotFoundException e) {
            logger.error("Webhook Error: Resource not found while processing failure for PI ID: {}. Message: {}", paymentIntentId, e.getMessage());
        } catch (Exception e) {
            logger.error("Webhook Error: Unexpected error handling payment failure for PI ID: {}. Error: {}", paymentIntentId, e.getMessage(), e);
        }
    }
}