// src/main/java/com/guvi/busapp/service/BookingServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.*;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;
import com.guvi.busapp.model.*;
import com.guvi.busapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final BusService busService;
    private final RouteService routeService;

    @Autowired
    public BookingServiceImpl(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              ScheduledTripRepository scheduledTripRepository,
                              BusService busService,
                              RouteService routeService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.busService = busService;
        this.routeService = routeService;
    }

    private PassengerDto mapPassengerToDto(Passenger passenger) {
        if (passenger == null) return null;
        PassengerDto dto = new PassengerDto();
        dto.setName(passenger.getName());
        dto.setAge(passenger.getAge());
        dto.setGender(passenger.getGender());
        dto.setSeatNumber(passenger.getSeatNumber());
        return dto;
    }

    private BookingResponseDto mapBookingToResponseDto(Booking booking) {
        if (booking == null) return null;

        BookingResponseDto responseDto = new BookingResponseDto();
        responseDto.setBookingId(booking.getId());
        responseDto.setStatus(booking.getStatus());
        responseDto.setTotalFare(booking.getTotalFare());
        responseDto.setBookingTime(booking.getBookingTime());

        User user = booking.getUser();
        if (user != null) {
            responseDto.setUserEmail(user.getEmail());
            responseDto.setUserFullName(user.getFirstName() + " " + user.getLastName());
        } else {
            logger.warn("Booking ID {} is missing User information.", booking.getId());
        }

        ScheduledTrip trip = booking.getScheduledTrip();
        if (trip != null) {
            ScheduledTripResponseDto tripDto = new ScheduledTripResponseDto();
            tripDto.setId(trip.getId());
            try {
                Bus bus = trip.getBus();
                Route route = trip.getRoute();
                if (bus != null) tripDto.setBus(busService.getBusById(bus.getId()));
                if (route!= null) tripDto.setRoute(routeService.getRouteById(route.getId()));
            } catch (ResourceNotFoundException e) {
                logger.error("Error mapping nested Bus/Route for Booking ID {}: {}", booking.getId(), e.getMessage());
            }
            tripDto.setDepartureDate(trip.getDepartureDate());
            tripDto.setDepartureTime(trip.getDepartureTime());
            tripDto.setArrivalTime(trip.getArrivalTime());
            tripDto.setFare(trip.getFare());
            tripDto.setAvailableSeats(trip.getAvailableSeats());
            responseDto.setTripDetails(tripDto);
        } else {
            logger.warn("Booking ID {} is missing ScheduledTrip information.", booking.getId());
        }

        responseDto.setPassengers(
                booking.getPassengers() != null ?
                        booking.getPassengers().stream()
                                .map(this::mapPassengerToDto)
                                .collect(Collectors.toList())
                        : new ArrayList<>()
        );

        return responseDto;
    }


    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto bookingRequest, String userEmail)
            throws ResourceNotFoundException, SeatUnavailableException, IllegalArgumentException {
        logger.info("Attempting to create PENDING booking for user {} on trip {}", userEmail, bookingRequest.getTripId());

        if (bookingRequest.getPassengers() == null || bookingRequest.getSelectedSeats() == null ||
                bookingRequest.getPassengers().size() != bookingRequest.getSelectedSeats().size()) {
            throw new IllegalArgumentException("Number of passengers (" + (bookingRequest.getPassengers() != null ? bookingRequest.getPassengers().size() : 0) +
                    ") must match the number of selected seats (" + (bookingRequest.getSelectedSeats() != null ? bookingRequest.getSelectedSeats().size() : 0) + ").");
        }
        if (bookingRequest.getSelectedSeats().isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected for booking.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ScheduledTrip trip = scheduledTripRepository.findByIdForUpdate(bookingRequest.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", bookingRequest.getTripId()));

        Map<String, ScheduledTrip.SeatStatus> seatStatusMap = trip.getSeatStatus();
        if (seatStatusMap == null) {
            logger.error("CRITICAL: Seat status map not initialized for trip ID: {}.", trip.getId());
            if (trip.getBus() != null) {
                throw new IllegalStateException("Seat status map not initialized for this trip. Trip ID: " + trip.getId());
            } else {
                throw new IllegalStateException("Seat status map not initialized and Bus info missing for trip ID: " + trip.getId());
            }
        }


        List<String> unavailableSeats = new ArrayList<>();
        for (String seatNum : bookingRequest.getSelectedSeats()) {
            ScheduledTrip.SeatStatus status = seatStatusMap.get(seatNum);
            if (status == null || (status != ScheduledTrip.SeatStatus.AVAILABLE && status != ScheduledTrip.SeatStatus.LOCKED)) {
                unavailableSeats.add(seatNum + (status != null ? " (" + status + ")" : " (Invalid)"));
            }
        }

        if (!unavailableSeats.isEmpty()) {
            String message = "Booking failed. Seats unavailable: " + String.join(", ", unavailableSeats);
            logger.warn(message);
            throw new SeatUnavailableException(message);
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setScheduledTrip(trip);
        booking.setNumberOfSeats(bookingRequest.getSelectedSeats().size());
        BigDecimal totalFare = trip.getFare().multiply(BigDecimal.valueOf(booking.getNumberOfSeats()));
        booking.setTotalFare(totalFare);

        List<PassengerDto> passengerDtos = bookingRequest.getPassengers();
        List<String> selectedSeats = bookingRequest.getSelectedSeats();
        List<Passenger> passengersToSave = new ArrayList<>();
        for (int i = 0; i < selectedSeats.size(); i++) {
            PassengerDto pDto = passengerDtos.get(i);
            String seatNum = selectedSeats.get(i);
            Passenger passenger = new Passenger();
            passenger.setName(pDto.getName()); passenger.setAge(pDto.getAge()); passenger.setGender(pDto.getGender());
            passenger.setSeatNumber(seatNum);
            passenger.setBooking(booking);
            passengersToSave.add(passenger);
        }
        booking.setPassengers(new HashSet<>(passengersToSave));

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("PENDING Booking created successfully with ID {} for user {} on trip {}", savedBooking.getId(), userEmail, trip.getId());

        return mapBookingToResponseDto(savedBooking);
    }


    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByUser(String userEmail) throws ResourceNotFoundException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        List<Booking> bookings = bookingRepository.findByUserOrderByBookingTimeDesc(user);
        return bookings.stream()
                .map(this::mapBookingToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAllBookings() {
        logger.info("Admin request: Fetching all bookings.");
        List<Booking> allBookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingTime"));
        logger.info("Mapping {} bookings to DTOs.", allBookings.size());
        return allBookings.stream()
                .map(this::mapBookingToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingEntityById(Long bookingId) throws ResourceNotFoundException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "ID", bookingId));
        if (booking.getUser() != null) booking.getUser().getEmail();
        if (booking.getScheduledTrip() != null) {
            booking.getScheduledTrip().getId();
            if (booking.getScheduledTrip().getBus() != null) booking.getScheduledTrip().getBus().getId();
            if (booking.getScheduledTrip().getRoute() != null) booking.getScheduledTrip().getRoute().getId();
        }
        booking.getPassengers().size();

        return booking;
    }
}