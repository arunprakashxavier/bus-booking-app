// src/main/java/com/guvi/busapp/service/EmailServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.model.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Override
    @Async
    public void sendBookingConfirmation(Booking booking) {
        if (booking == null || booking.getUser() == null || booking.getScheduledTrip() == null) {
            logger.error("Cannot send confirmation email. Booking data is incomplete: {}", booking);
            return;
        }

        // Ensure lazy-loaded fields are accessible in this async thread
        // Trigger loading before creating the context
        String recipientEmail = booking.getUser().getEmail(); // Fetch email early
        Long bookingId = booking.getId(); // Fetch ID early
        logger.debug("Preparing to send confirmation for booking ID {} to {}", bookingId, recipientEmail);
        try {
            // It's safer to trigger related fields needed by the template here
            booking.getScheduledTrip().getDepartureDate();
            booking.getScheduledTrip().getDepartureTime();
            booking.getScheduledTrip().getArrivalTime();
            booking.getScheduledTrip().getFare();
            if(booking.getScheduledTrip().getRoute() != null) booking.getScheduledTrip().getRoute().getOrigin();
            if(booking.getScheduledTrip().getBus() != null) booking.getScheduledTrip().getBus().getBusNumber();
            booking.getPassengers().size(); // Trigger passenger loading
            logger.debug("Lazy-loaded fields accessed for booking ID {}", bookingId);
        } catch (Exception e) {
            logger.error("Error accessing lazy-loaded fields for email context for booking ID {}: {}", bookingId, e.getMessage());
            // Decide if you should still attempt sending a partial email or just return
            return;
        }


        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariable("booking", booking);
            String passengerDetails = booking.getPassengers().stream()
                    .map(p -> p.getName() + " (Age: " + p.getAge() + ", Seat: " + p.getSeatNumber() + ")")
                    .collect(Collectors.joining("<br>"));
            context.setVariable("passengerDetails", passengerDetails);

            // **** CORRECTED TEMPLATE PATH ****
            String htmlContent = templateEngine.process("ticket-email", context); // Removed "email/" prefix

            helper.setTo(recipientEmail); // Use pre-fetched email
            helper.setSubject("Your Bus Ticket Confirmation - Booking ID: " + bookingId); // Use pre-fetched ID
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Booking confirmation email sent successfully to {} for booking ID {}", recipientEmail, bookingId);

        } catch (MessagingException e) {
            logger.error("Failed to send confirmation email for booking ID {}: {}", bookingId, e.getMessage(), e);
        } catch (Exception e) {
            // Catch potential TemplateProcessingException here too
            logger.error("Unexpected error during email sending or template processing for booking ID {}: {}", bookingId, e.getMessage(), e);
        }
    }
}