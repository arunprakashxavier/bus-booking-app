
// src/main/java/com/guvi/busapp/service/PaymentServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.model.Booking;
import com.guvi.busapp.model.Passenger;
import com.guvi.busapp.model.ScheduledTrip;
import com.guvi.busapp.repository.BookingRepository;
import com.guvi.busapp.repository.ScheduledTripRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException; // **** ADDED THIS IMPORT ****
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
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

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final EmailService emailService;

    @Autowired
    public PaymentServiceImpl(BookingService bookingService,
                              BookingRepository bookingRepository,
                              ScheduledTripRepository scheduledTripRepository,
                              @Autowired(required = false) EmailService emailService) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.emailService = emailService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        logger.info("Stripe API Key initialized.");
    }

    @Override
    @Transactional
    public PaymentIntent createPaymentIntent(Long bookingId, String userEmailForVerification)
            throws StripeException, ResourceNotFoundException, AccessDeniedException, IllegalStateException { // Added IllegalStateException
        logger.info("Processing request to create payment intent for booking ID {} from user {}",
                bookingId, userEmailForVerification);

        Booking booking = bookingService.getBookingEntityById(bookingId);

        if (booking.getUser() == null || !booking.getUser().getEmail().equals(userEmailForVerification)) {
            logger.warn("User {} attempted to create payment intent for booking ID {} which does not belong to them or user is null.",
                    userEmailForVerification, booking.getId());
            throw new AccessDeniedException("Access denied to this booking.");
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            logger.warn("Attempted to create payment intent for booking ID {} with status {}, expected PENDING.",
                    booking.getId(), booking.getStatus());
            throw new IllegalStateException("Payment cannot be initiated for booking with status: " + booking.getStatus());
        }

        String currency = "inr";
        long amountInPaise = booking.getTotalFare().multiply(new BigDecimal("100")).longValueExact();

        logger.info("Creating Stripe Payment Intent for booking ID: {}, Amount: {}, Currency: {}",
                bookingId, amountInPaise, currency);
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInPaise)
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .putMetadata("booking_id", String.valueOf(bookingId))
                .putMetadata("user_email", userEmailForVerification)
                .setDescription("Bus Ticket Booking #" + bookingId)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        logger.info("Stripe Payment Intent {} created successfully for booking ID {}", paymentIntent.getId(), bookingId);
        return paymentIntent;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String sigHeader) {
        try {
            Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            return true;
        } catch (SignatureVerificationException e) {
            logger.warn("Webhook signature verification failed in service: {}", e.getMessage());
            return false;
        } catch (Exception e){
            logger.error("Error during signature construction in service: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String paymentIntentId, Long bookingId, Long amount, String currency) {
        logger.info("Handling PaymentIntent Succeeded: PI_ID={}, Booking_ID={}", paymentIntentId, bookingId);

        try {
            Booking booking = bookingService.getBookingEntityById(bookingId);

            if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
                logger.warn("Webhook Warning: Booking ID {} is already confirmed. Ignoring duplicate event for PI ID: {}", bookingId, paymentIntentId);
                return;
            }
            if (booking.getStatus() != Booking.BookingStatus.PENDING) {
                logger.warn("Webhook Warning: Received success event for booking ID {} with status {}, expected PENDING. Ignoring. PI ID: {}", bookingId, booking.getStatus(), paymentIntentId);
                return;
            }

            long expectedAmount = booking.getTotalFare().multiply(new BigDecimal("100")).longValueExact();
            if (amount != null && !amount.equals(expectedAmount)) {
                logger.warn("Webhook Warning: Amount mismatch for Booking ID {}. Expected: {}, Received: {}. PI ID: {}. Processing anyway...",
                        bookingId, expectedAmount, amount, paymentIntentId);
            }

            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            logger.info("Booking ID {} status updated to CONFIRMED.", bookingId);

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
                        if(seatStatusMap.get(seatNum) == ScheduledTrip.SeatStatus.LOCKED || seatStatusMap.get(seatNum) == ScheduledTrip.SeatStatus.AVAILABLE) {
                            seatStatusMap.put(seatNum, ScheduledTrip.SeatStatus.BOOKED);
                            updatedCount++;
                        } else {
                            logger.warn("Webhook Warning: Seat {} for booking ID {} on trip {} was not in LOCKED or AVAILABLE state (Actual: {}). Not changing to BOOKED.", seatNum, bookingId, trip.getId(), seatStatusMap.get(seatNum));
                        }
                    } else {
                        logger.warn("Webhook Warning: Seat {} for booking ID {} not found in trip {} seat map during confirmation.", seatNum, bookingId, trip.getId());
                    }
                }
                scheduledTripRepository.save(trip);
                logger.info("Updated status to BOOKED for {} seats on trip ID {}.", updatedCount, trip.getId());
            }

            if (emailService != null) {
                try {
                    Booking confirmedBookingForEmail = bookingService.getBookingEntityById(bookingId);
                    logger.info("Attempting to send confirmation email for booking ID: {}", confirmedBookingForEmail.getId());
                    emailService.sendBookingConfirmation(confirmedBookingForEmail);
                } catch (Exception emailEx) {
                    logger.error("Error triggering confirmation email for booking ID {}: {}", bookingId, emailEx.getMessage(), emailEx);
                }
            } else {
                logger.warn("EmailService not available. Skipping confirmation email for booking ID: {}", bookingId);
            }

        } catch (ResourceNotFoundException e) {
            logger.error("Webhook Error: Resource not found while processing success for PI ID: {}. Message: {}", paymentIntentId, e.getMessage());
        } catch (Exception e) {
            logger.error("Webhook Error: Unexpected error handling payment success for PI ID: {}. Error: {}", paymentIntentId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handlePaymentFailure(String paymentIntentId, Long bookingId) {
        logger.warn("Handling PaymentIntent Failed: PI_ID={}, Booking_ID={}", paymentIntentId, bookingId);
        try {
            Booking booking = bookingService.getBookingEntityById(bookingId);

            if (booking.getStatus() != Booking.BookingStatus.PENDING) {
                logger.warn("Webhook Warning: Received failure event for booking ID {} with status {}, expected PENDING. Ignoring. PI ID: {}", bookingId, booking.getStatus(), paymentIntentId);
                return;
            }

            booking.setStatus(Booking.BookingStatus.FAILED);
            bookingRepository.save(booking);
            logger.info("Booking ID {} status updated to FAILED.", bookingId);

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
        } catch (ResourceNotFoundException e) {
            logger.error("Webhook Error: Resource not found while processing failure for PI ID: {}. Message: {}", paymentIntentId, e.getMessage());
        } catch (Exception e) {
            logger.error("Webhook Error: Unexpected error handling payment failure for PI ID: {}. Error: {}", paymentIntentId, e.getMessage(), e);
        }
    }
}