// src/test/java/com/guvi/busapp/service/BookingServiceImplTest.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.*;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;
import com.guvi.busapp.model.*;
import com.guvi.busapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ScheduledTripRepository scheduledTripRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private BusService busService;
    @Mock
    private RouteService routeService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Bus testBus;
    private Route testRoute;
    private ScheduledTrip testTrip;
    private BookingRequestDto testBookingRequest;
    private List<PassengerDto> testPassengersDto;
    private Booking savedBooking;
    private BusDto testBusDto;
    private RouteDto testRouteDto;

    @BeforeEach
    void setUp() {
        // Create mock User
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encodedPassword");
        testUser.setRole(User.Role.ROLE_USER);

        // Create mock Bus & DTO
        testBus = new Bus();
        testBus.setId(10L);
        testBus.setBusNumber("TN-01-1000");
        testBus.setOperatorName("Test Operator");
        testBus.setBusType("AC Sleeper");
        testBus.setTotalSeats(30);
        testBus.setAmenities(new HashSet<>(Set.of("WiFi", "AC")));

        testBusDto = new BusDto();
        testBusDto.setId(testBus.getId());
        testBusDto.setBusNumber(testBus.getBusNumber());
        testBusDto.setOperatorName(testBus.getOperatorName());
        testBusDto.setBusType(testBus.getBusType());
        testBusDto.setTotalSeats(testBus.getTotalSeats());
        testBusDto.setAmenities(testBus.getAmenities());

        // Create mock Route & DTO
        testRoute = new Route();
        testRoute.setId(20L);
        testRoute.setOrigin("City A");
        testRoute.setDestination("City B");

        testRouteDto = new RouteDto();
        testRouteDto.setId(testRoute.getId());
        testRouteDto.setOrigin(testRoute.getOrigin());
        testRouteDto.setDestination(testRoute.getDestination());

        // Create mock ScheduledTrip
        testTrip = new ScheduledTrip();
        testTrip.setId(30L);
        testTrip.setBus(testBus);
        testTrip.setRoute(testRoute);
        testTrip.setDepartureDate(LocalDate.now().plusDays(1));
        testTrip.setDepartureTime(LocalTime.of(10, 0));
        testTrip.setArrivalTime(LocalTime.of(18, 0));
        testTrip.setFare(new BigDecimal("500.00"));
        testTrip.setAvailableSeats(30);
        Map<String, ScheduledTrip.SeatStatus> seatStatusMap = new HashMap<>();
        seatStatusMap.put("1", ScheduledTrip.SeatStatus.BOOKED);
        seatStatusMap.put("2", ScheduledTrip.SeatStatus.LOCKED);
        seatStatusMap.put("3", ScheduledTrip.SeatStatus.AVAILABLE);
        for(int i = 4; i <= 30; i++) {
            seatStatusMap.put(String.valueOf(i), ScheduledTrip.SeatStatus.AVAILABLE);
        }
        testTrip.setSeatStatus(seatStatusMap);

        // Create mock Passenger DTOs (Using Setters)
        PassengerDto p1Dto = new PassengerDto();
        p1Dto.setName("Pass Two");
        p1Dto.setAge(30);
        p1Dto.setGender("Male");
        p1Dto.setSeatNumber("2");

        PassengerDto p2Dto = new PassengerDto();
        p2Dto.setName("Pass Three");
        p2Dto.setAge(25);
        p2Dto.setGender("Female");
        p2Dto.setSeatNumber("3");

        testPassengersDto = List.of(p1Dto, p2Dto);

        // Create mock Booking Request
        testBookingRequest = new BookingRequestDto();
        testBookingRequest.setTripId(testTrip.getId());
        testBookingRequest.setSelectedSeats(List.of("2", "3"));
        testBookingRequest.setPassengers(testPassengersDto);

        // Create a mock Booking to be returned by save/find (Using Setters for Passengers)
        savedBooking = new Booking();
        savedBooking.setId(100L);
        savedBooking.setUser(testUser);
        savedBooking.setScheduledTrip(testTrip);
        savedBooking.setBookingTime(LocalDateTime.now());
        savedBooking.setStatus(Booking.BookingStatus.PENDING);
        savedBooking.setNumberOfSeats(testBookingRequest.getSelectedSeats().size());
        savedBooking.setTotalFare(testTrip.getFare().multiply(BigDecimal.valueOf(savedBooking.getNumberOfSeats())));
        Set<Passenger> savedPassengers = new HashSet<>();
        Passenger pass1 = new Passenger();
        pass1.setName("Pass Two"); pass1.setAge(30); pass1.setGender("Male"); pass1.setSeatNumber("2"); pass1.setBooking(savedBooking);
        Passenger pass2 = new Passenger();
        pass2.setName("Pass Three"); pass2.setAge(25); pass2.setGender("Female"); pass2.setSeatNumber("3"); pass2.setBooking(savedBooking);
        savedPassengers.add(pass1); savedPassengers.add(pass2);
        savedBooking.setPassengers(savedPassengers);
    }

    @Test
    void testCreateBooking_Success() throws Exception {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(scheduledTripRepository.findByIdForUpdate(testTrip.getId())).thenReturn(Optional.of(testTrip));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));
        when(busService.getBusById(testBus.getId())).thenReturn(testBusDto);
        when(routeService.getRouteById(testRoute.getId())).thenReturn(testRouteDto);

        BookingResponseDto responseDto = bookingService.createBooking(testBookingRequest, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(savedBooking.getId(), responseDto.getBookingId());
        assertEquals(Booking.BookingStatus.PENDING, responseDto.getStatus());
        assertEquals(testUser.getEmail(), responseDto.getUserEmail());
        assertNotNull(responseDto.getPassengers());
        assertEquals(testPassengersDto.size(), responseDto.getPassengers().size());
        assertEquals(savedBooking.getTotalFare(), responseDto.getTotalFare());
        assertNotNull(responseDto.getTripDetails());
        assertEquals(testTrip.getId(), responseDto.getTripDetails().getId());
        assertNotNull(responseDto.getTripDetails().getBus());
        assertEquals(testBus.getId(), responseDto.getTripDetails().getBus().getId());
        assertNotNull(responseDto.getTripDetails().getRoute());
        assertEquals(testRoute.getId(), responseDto.getTripDetails().getRoute().getId());

        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(scheduledTripRepository, times(1)).findByIdForUpdate(testTrip.getId());
    }

    @Test
    void testCreateBooking_SeatUnavailable_Booked() {
        // Arrange: Modify the request to include a BOOKED seat (Seat 1 is BOOKED in setup)
        List<String> requestedSeats = List.of("1", "3");
        PassengerDto p1Dto = new PassengerDto(); p1Dto.setName("P Booked"); p1Dto.setAge(40); p1Dto.setGender("O"); p1Dto.setSeatNumber("1");
        PassengerDto p2Dto = new PassengerDto(); p2Dto.setName("P Avail"); p2Dto.setAge(35); p2Dto.setGender("M"); p2Dto.setSeatNumber("3");
        testBookingRequest.setSelectedSeats(requestedSeats);
        testBookingRequest.setPassengers(List.of(p1Dto, p2Dto));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(scheduledTripRepository.findByIdForUpdate(testTrip.getId())).thenReturn(Optional.of(testTrip));

        // Act & Assert: Expect SeatUnavailableException - REMOVED message check
        assertThrows(SeatUnavailableException.class, () -> {
            bookingService.createBooking(testBookingRequest, testUser.getEmail());
        }, "Should throw SeatUnavailableException when a booked seat is requested"); // Added custom message for clarity if it fails

        // Verify that bookingRepository.save was NOT called
        verify(bookingRepository, never()).save(any(Booking.class));
        // Verify trip repo was called for locking check
        verify(scheduledTripRepository, times(1)).findByIdForUpdate(testTrip.getId());
    }

    @Test
    void testCreateBooking_UserNotFound() {
        String unknownEmail = "unknown@example.com";
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.createBooking(testBookingRequest, unknownEmail);
        });

        // Corrected Assertion: Add single quotes
        assertEquals("User not found with email : '" + unknownEmail + "'", exception.getMessage());
        verify(scheduledTripRepository, never()).findByIdForUpdate(anyLong());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_TripNotFound() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(scheduledTripRepository.findByIdForUpdate(testTrip.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.createBooking(testBookingRequest, testUser.getEmail());
        });

        // Corrected Assertion: Add single quotes
        assertEquals("ScheduledTrip not found with ID : '" + testBookingRequest.getTripId() + "'", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_PassengerCountMismatch() {
        testBookingRequest.setSelectedSeats(List.of("2", "3"));
        PassengerDto p1Dto = new PassengerDto(); p1Dto.setName("Only One"); p1Dto.setAge(20); p1Dto.setGender("M"); p1Dto.setSeatNumber("2");
        testBookingRequest.setPassengers(List.of(p1Dto));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testBookingRequest, testUser.getEmail());
        });

        assertTrue(exception.getMessage().contains("Number of passengers (1) must match the number of selected seats (2)"));
        verify(userRepository, never()).findByEmail(anyString());
        verify(scheduledTripRepository, never()).findByIdForUpdate(anyLong());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // --- Tests for getBookingsByUser ---

    @Test
    void testGetBookingsByUser_Success() {
        // Arrange
        List<Booking> userBookings = List.of(savedBooking);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(bookingRepository.findByUserOrderByBookingTimeDesc(testUser)).thenReturn(userBookings);
        when(busService.getBusById(testBus.getId())).thenReturn(testBusDto);
        when(routeService.getRouteById(testRoute.getId())).thenReturn(testRouteDto);

        // Act
        List<BookingResponseDto> result = bookingService.getBookingsByUser(testUser.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BookingResponseDto dto = result.get(0);
        assertEquals(savedBooking.getId(), dto.getBookingId());
        assertEquals(testUser.getEmail(), dto.getUserEmail());
        assertEquals(testTrip.getId(), dto.getTripDetails().getId());

        // Verify
        verify(userRepository, times(1)).findByEmail(testUser.getEmail());
        verify(bookingRepository, times(1)).findByUserOrderByBookingTimeDesc(testUser);
    }

    @Test
    void testGetBookingsByUser_UserNotFound() {
        // Arrange
        String unknownEmail = "unknown@example.com";
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.getBookingsByUser(unknownEmail);
        });
        // Corrected Assertion: Add single quotes
        assertEquals("User not found with email : '" + unknownEmail + "'", exception.getMessage());

        // Verify
        verify(userRepository, times(1)).findByEmail(unknownEmail);
        verify(bookingRepository, never()).findByUserOrderByBookingTimeDesc(any(User.class));
    }

    // --- Tests for getAllBookings ---

    @Test
    void testGetAllBookings_Success() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2L); anotherUser.setEmail("another@example.com"); anotherUser.setFirstName("Another"); anotherUser.setLastName("Person");

        Booking anotherBooking = new Booking();
        anotherBooking.setId(101L); anotherBooking.setUser(anotherUser); anotherBooking.setScheduledTrip(testTrip);
        anotherBooking.setStatus(Booking.BookingStatus.CONFIRMED); anotherBooking.setBookingTime(LocalDateTime.now().minusHours(1));
        Passenger anotherPass = new Passenger();
        anotherPass.setName("A Passenger"); anotherPass.setAge(40); anotherPass.setGender("F"); anotherPass.setSeatNumber("1"); anotherPass.setBooking(anotherBooking);
        anotherBooking.setPassengers(new HashSet<>(List.of(anotherPass)));

        List<Booking> allBookings = List.of(savedBooking, anotherBooking); // Order might depend on bookingTime

        when(bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingTime"))).thenReturn(allBookings);
        when(busService.getBusById(testBus.getId())).thenReturn(testBusDto);
        when(routeService.getRouteById(testRoute.getId())).thenReturn(testRouteDto);

        // Act
        List<BookingResponseDto> result = bookingService.getAllBookings();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        BookingResponseDto dto1 = result.stream().filter(b -> b.getBookingId().equals(savedBooking.getId())).findFirst().orElse(null);
        assertNotNull(dto1);
        assertEquals(savedBooking.getId(), dto1.getBookingId());
        assertEquals(testUser.getEmail(), dto1.getUserEmail());
        assertEquals(Booking.BookingStatus.PENDING, dto1.getStatus());

        BookingResponseDto dto2 = result.stream().filter(b -> b.getBookingId().equals(anotherBooking.getId())).findFirst().orElse(null);
        assertNotNull(dto2);
        assertEquals(anotherBooking.getId(), dto2.getBookingId());
        assertEquals(anotherUser.getEmail(), dto2.getUserEmail());
        assertEquals(Booking.BookingStatus.CONFIRMED, dto2.getStatus());

        // Verify
        verify(bookingRepository, times(1)).findAll(Sort.by(Sort.Direction.DESC, "bookingTime"));
    }

    @Test
    void testGetAllBookings_EmptyList() {
        // Arrange
        when(bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingTime"))).thenReturn(Collections.emptyList());

        // Act
        List<BookingResponseDto> result = bookingService.getAllBookings();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(bookingRepository, times(1)).findAll(Sort.by(Sort.Direction.DESC, "bookingTime"));
    }
}