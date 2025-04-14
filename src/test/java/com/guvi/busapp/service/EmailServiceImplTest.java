package com.guvi.busapp.service;

import com.guvi.busapp.model.*;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor; // Import Captor
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException; // Import for testing exception
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Properties; // Import Properties for dummy MimeMessage
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    // Argument Captors
    @Captor
    private ArgumentCaptor<Context> contextCaptor;
    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

    // Test Data
    private Booking testBooking;
    private User testUser;
    private ScheduledTrip testTrip;
    private Bus testBus;
    private Route testRoute;

    @BeforeEach
    void setUp() {
        // --- User ---
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("recipient@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("Recipient");

        // --- Bus ---
        testBus = new Bus();
        testBus.setId(10L);
        testBus.setBusNumber("TN-MAIL-TEST");
        testBus.setOperatorName("Email Bus Lines");
        testBus.setBusType("AC Sleeper");

        // --- Route ---
        testRoute = new Route();
        testRoute.setId(20L);
        testRoute.setOrigin("EmailOrigin");
        testRoute.setDestination("EmailDest");

        // --- Scheduled Trip ---
        testTrip = new ScheduledTrip();
        testTrip.setId(30L);
        testTrip.setBus(testBus);
        testTrip.setRoute(testRoute);
        testTrip.setDepartureDate(LocalDate.now().plusDays(3));
        testTrip.setDepartureTime(LocalTime.of(11, 30));
        testTrip.setArrivalTime(LocalTime.of(19, 45));
        testTrip.setFare(new BigDecimal("650.00"));

        // --- Passengers ---
        Passenger p1 = new Passenger(); p1.setName("Pass A"); p1.setAge(30); p1.setSeatNumber("A1");
        Passenger p2 = new Passenger(); p2.setName("Pass B"); p2.setAge(45); p2.setSeatNumber("A2");
        Set<Passenger> passengers = new HashSet<>();
        passengers.add(p1); passengers.add(p2);

        // --- Booking ---
        testBooking = new Booking();
        testBooking.setId(101L);
        testBooking.setUser(testUser);
        testBooking.setScheduledTrip(testTrip);
        testBooking.setBookingTime(LocalDateTime.now().minusMinutes(5));
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED); // Test assumes confirmed state
        testBooking.setNumberOfSeats(passengers.size());
        testBooking.setTotalFare(testTrip.getFare().multiply(BigDecimal.valueOf(passengers.size())));
        // Link passengers back to booking (important for context generation)
        p1.setBooking(testBooking);
        p2.setBooking(testBooking);
        testBooking.setPassengers(passengers);

    }

    @Test
    void sendBookingConfirmation_Success() throws MessagingException { // Add MessagingException to signature
        // Arrange
        String expectedSubject = "Your Bus Ticket Confirmation - Booking ID: " + testBooking.getId();
        String dummyHtmlContent = "<html><body>Booking Confirmed!</body></html>";
        // Create a dummy MimeMessage to be returned by the mock mailSender
        // Note: Using a real MimeMessage requires a Session, which is tricky to mock easily.
        // A simpler approach for unit tests is to use a mock MimeMessage directly if possible,
        // but ArgumentCaptor needs a concrete type. So we create a basic one.
        MimeMessage dummyMimeMessage = new MimeMessage((Session) null); // Basic message

        when(mailSender.createMimeMessage()).thenReturn(dummyMimeMessage);
        when(templateEngine.process(eq("email/ticket-email"), contextCaptor.capture())).thenReturn(dummyHtmlContent);
        // Mock the void send method
        doNothing().when(mailSender).send(mimeMessageCaptor.capture());

        // Act
        emailService.sendBookingConfirmation(testBooking);

        // Assert
        // Verify template processing
        verify(templateEngine, times(1)).process(eq("email/ticket-email"), any(Context.class));
        Context capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext.getVariable("booking"));
        assertEquals(testBooking, capturedContext.getVariable("booking"));
        assertTrue(((String)capturedContext.getVariable("passengerDetails")).contains("Seat: A1"));
        assertTrue(((String)capturedContext.getVariable("passengerDetails")).contains("Seat: A2"));

        // Verify email sending
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        // Assertions on the MimeMessage (need try-catch for MessagingException)
        assertEquals(expectedSubject, capturedMessage.getSubject());
        assertNotNull(capturedMessage.getRecipients(MimeMessage.RecipientType.TO));
        assertEquals(1, capturedMessage.getRecipients(MimeMessage.RecipientType.TO).length);
        assertEquals(testUser.getEmail(), capturedMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        // Note: Verifying HTML content is more complex and usually requires MimeMessageHelper or inspecting raw content.
        // Checking recipient and subject is often sufficient for unit tests.
    }

    @Test
    void sendBookingConfirmation_NullBooking() {
        // Arrange (no mocking needed)

        // Act
        emailService.sendBookingConfirmation(null);

        // Assert (Verify no interaction with mailSender or templateEngine)
        verify(mailSender, never()).createMimeMessage();
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendBookingConfirmation_NullUser() {
        // Arrange
        testBooking.setUser(null);

        // Act
        emailService.sendBookingConfirmation(testBooking);

        // Assert (Verify no interaction)
        verify(mailSender, never()).createMimeMessage();
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendBookingConfirmation_MessagingExceptionOnSend() {
        // Arrange
        MimeMessage dummyMimeMessage = new MimeMessage((Session) null);
        String dummyHtmlContent = "<html>Test</html>";
        when(mailSender.createMimeMessage()).thenReturn(dummyMimeMessage);
        when(templateEngine.process(eq("email/ticket-email"), any(Context.class))).thenReturn(dummyHtmlContent);
        // Mock mailSender.send() to throw an exception
        doThrow(new MailSendException("Test Mail Send Failure")).when(mailSender).send(any(MimeMessage.class));

        // Act
        // Since the service method catches the exception and logs it, we don't expect it to bubble up
        assertDoesNotThrow(() -> {
            emailService.sendBookingConfirmation(testBooking);
        });


        // Assert / Verify
        // We verify that send was indeed called, even though it threw an exception internally
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        // We can't easily verify the logger output without specific log testing frameworks,
        // but we ensured no exception escaped the method.
    }
}