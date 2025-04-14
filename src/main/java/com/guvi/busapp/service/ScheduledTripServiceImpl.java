// src/main/java/com/guvi/busapp/service/ScheduledTripServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.*;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;
import com.guvi.busapp.model.Bus;
import com.guvi.busapp.model.Route;
import com.guvi.busapp.model.ScheduledTrip;
import com.guvi.busapp.repository.BusRepository;
import com.guvi.busapp.repository.RouteRepository;
import com.guvi.busapp.repository.ScheduledTripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
public class ScheduledTripServiceImpl implements ScheduledTripService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTripServiceImpl.class);

    private final ScheduledTripRepository scheduledTripRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;

    @Autowired
    public ScheduledTripServiceImpl(ScheduledTripRepository scheduledTripRepository,
                                    BusRepository busRepository,
                                    RouteRepository routeRepository) {
        this.scheduledTripRepository = scheduledTripRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
    }

    // --- Helper Mapping Methods ---
    // (Keep existing mapping methods: mapBusToDto, mapRouteToDto, mapEntityToResponseDto)
    private BusDto mapBusToDto(Bus bus) {
        if (bus == null) return null;
        BusDto dto = new BusDto();
        dto.setId(bus.getId());
        dto.setBusNumber(bus.getBusNumber());
        dto.setOperatorName(bus.getOperatorName());
        dto.setBusType(bus.getBusType());
        dto.setTotalSeats(bus.getTotalSeats());
        dto.setAmenities(bus.getAmenities() != null ? new java.util.HashSet<>(bus.getAmenities()) : null);
        dto.setSeatLayout(bus.getSeatLayout());
        return dto;
    }

    private RouteDto mapRouteToDto(Route route) {
        if (route == null) return null;
        RouteDto dto = new RouteDto();
        dto.setId(route.getId());
        dto.setOrigin(route.getOrigin());
        dto.setDestination(route.getDestination());
        return dto;
    }

    private ScheduledTripResponseDto mapEntityToResponseDto(ScheduledTrip trip) {
        if (trip == null) return null;
        ScheduledTripResponseDto dto = new ScheduledTripResponseDto();
        dto.setId(trip.getId());
        Bus bus = trip.getBus();
        Route route = trip.getRoute();
        dto.setBus(mapBusToDto(bus));
        dto.setRoute(mapRouteToDto(route));
        dto.setDepartureDate(trip.getDepartureDate());
        dto.setDepartureTime(trip.getDepartureTime());
        dto.setArrivalTime(trip.getArrivalTime());
        dto.setFare(trip.getFare());
        dto.setAvailableSeats(trip.getAvailableSeats());
        return dto;
    }

    private Map<String, ScheduledTrip.SeatStatus> initializeSeats(Bus bus) {
        Map<String, ScheduledTrip.SeatStatus> seatStatusMap = new HashMap<>();
        String layout = bus.getSeatLayout();
        int totalSeats = bus.getTotalSeats() != null ? bus.getTotalSeats() : 0;

        if (StringUtils.hasText(layout) && layout.contains(",")) {
            String[] seatNumbers = layout.split(",");
            for (String seatNum : seatNumbers) {
                seatNum = seatNum.trim();
                if (!seatNum.isEmpty()) {
                    seatStatusMap.put(seatNum, ScheduledTrip.SeatStatus.AVAILABLE);
                }
            }
            logger.debug("Initialized seats from layout string: {}", layout);
        } else {
            logger.warn("Bus ID {} missing detailed seat layout. Generating simple seat numbers 1 to {}.", bus.getId(), totalSeats);
            if (totalSeats > 0) {
                for (int i = 1; i <= totalSeats; i++) {
                    seatStatusMap.put(String.valueOf(i), ScheduledTrip.SeatStatus.AVAILABLE);
                }
            }
        }

        if (seatStatusMap.isEmpty() && totalSeats > 0) {
            logger.error("Failed to initialize seats correctly for bus ID {}. Layout: '{}', TotalSeats: {}. Defaulting to 1-N.", bus.getId(), layout, totalSeats);
            for (int i = 1; i <= totalSeats; i++) {
                seatStatusMap.put(String.valueOf(i), ScheduledTrip.SeatStatus.AVAILABLE);
            }
        }
        return seatStatusMap;
    }

    // --- Service Method Implementations ---

    @Override
    @Transactional
    public ScheduledTripResponseDto scheduleTrip(ScheduledTripRequestDto requestDto) {
        if (requestDto.getArrivalTime().isBefore(requestDto.getDepartureTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time.");
        }
        Bus bus = busRepository.findById(requestDto.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "ID", requestDto.getBusId()));
        Route route = routeRepository.findById(requestDto.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", "ID", requestDto.getRouteId()));

        ScheduledTrip newTrip = new ScheduledTrip();
        newTrip.setBus(bus);
        newTrip.setRoute(route);
        newTrip.setDepartureDate(requestDto.getDepartureDate());
        newTrip.setDepartureTime(requestDto.getDepartureTime());
        newTrip.setArrivalTime(requestDto.getArrivalTime());
        newTrip.setFare(requestDto.getFare());

        Map<String, ScheduledTrip.SeatStatus> initialSeatStatus = initializeSeats(bus);
        newTrip.setSeatStatus(initialSeatStatus);
        newTrip.setAvailableSeats((int) initialSeatStatus.values().stream()
                .filter(s -> s == ScheduledTrip.SeatStatus.AVAILABLE).count());

        ScheduledTrip savedTrip = scheduledTripRepository.save(newTrip);
        logger.info("Scheduled new trip ID: {}, initialized {} seats, {} available.",
                savedTrip.getId(), savedTrip.getSeatStatus().size(), savedTrip.getAvailableSeats());

        savedTrip.getBus().getBusNumber();
        savedTrip.getRoute().getOrigin();

        return mapEntityToResponseDto(savedTrip);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTripResponseDto> getAllScheduledTrips() {
        List<ScheduledTrip> trips = scheduledTripRepository.findAll();
        trips.forEach(trip -> { trip.getBus().getBusNumber(); trip.getRoute().getOrigin(); });
        return trips.stream().map(this::mapEntityToResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledTripResponseDto getScheduledTripById(Long id) {
        ScheduledTrip trip = scheduledTripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", id));
        trip.getBus().getBusNumber(); trip.getRoute().getOrigin();
        return mapEntityToResponseDto(trip);
    }

    @Override
    @Transactional
    public ScheduledTripResponseDto updateScheduledTrip(Long id, ScheduledTripRequestDto requestDto) {
        ScheduledTrip existingTrip = scheduledTripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", id));
        if (requestDto.getArrivalTime().isBefore(requestDto.getDepartureTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time.");
        }
        existingTrip.setDepartureDate(requestDto.getDepartureDate());
        existingTrip.setDepartureTime(requestDto.getDepartureTime());
        existingTrip.setArrivalTime(requestDto.getArrivalTime());
        existingTrip.setFare(requestDto.getFare());
        ScheduledTrip updatedTrip = scheduledTripRepository.save(existingTrip);
        updatedTrip.getBus().getBusNumber(); updatedTrip.getRoute().getOrigin();
        return mapEntityToResponseDto(updatedTrip);
    }

    @Override
    @Transactional
    public void deleteScheduledTrip(Long id) {
        ScheduledTrip trip = scheduledTripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", id));
        scheduledTripRepository.delete(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTripResponseDto> findAvailableTrips(String origin, String destination, LocalDate date) {
        List<ScheduledTrip> trips = scheduledTripRepository.findAvailableTripsByLocationAndDate(origin, destination, date);
        trips.forEach(trip -> { trip.getBus().getBusNumber(); trip.getRoute().getOrigin(); });
        return trips.stream().map(this::mapEntityToResponseDto).collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public SeatLayoutDto getSeatLayoutForTrip(Long tripId) {
        ScheduledTrip trip = scheduledTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", tripId));
        Bus bus = trip.getBus();
        if (bus == null) { throw new IllegalStateException("Trip missing Bus info."); }
        Map<String, ScheduledTrip.SeatStatus> statusMap = trip.getSeatStatus();
        if (statusMap == null) { statusMap = new HashMap<>(); }
        Integer totalSeats = bus.getTotalSeats();
        BigDecimal tripFare = trip.getFare();
        logger.info("Returning seat layout '{}', status map size {}, total seats {}, and fare {} for trip ID {}",
                bus.getSeatLayout(), statusMap.size(), totalSeats, tripFare, tripId);
        // Assuming SeatLayoutDto constructor matches: layout, map, totalSeats, fare
        return new SeatLayoutDto(bus.getSeatLayout(), new HashMap<>(statusMap), totalSeats, tripFare);
    }


    @Override
    @Transactional
    public boolean lockSeats(Long tripId, List<String> seatNumbers, Long userId) throws SeatUnavailableException {
        logger.info("Attempting to lock seats {} for trip ID {} by user ID {}", seatNumbers, tripId, userId);

        ScheduledTrip trip = scheduledTripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTrip", "ID", tripId));

        Map<String, ScheduledTrip.SeatStatus> currentStatusMap = trip.getSeatStatus();
        if (currentStatusMap == null) {
            logger.error("Seat status map not initialized for trip ID: {}", tripId);
            throw new IllegalStateException("Seat status map not initialized for this trip.");
        }

        List<String> unavailableSeats = new ArrayList<>();
        for (String seatNum : seatNumbers) {
            ScheduledTrip.SeatStatus status = currentStatusMap.get(seatNum);

            // **** ADDED TEMPORARY DEBUG LOG ****
            boolean isUnavailable = (status == null || status != ScheduledTrip.SeatStatus.AVAILABLE);
            logger.debug("LockSeats Check - Seat: {}, Status found: {}, IsUnavailable check result: {}", seatNum, status, isUnavailable);
            // **** END TEMPORARY DEBUG LOG ****

            if (isUnavailable) {
                unavailableSeats.add(seatNum + (status != null ? " (" + status + ")" : " (Invalid Seat)"));
            }
        }

        if (!unavailableSeats.isEmpty()) {
            String message = "Cannot lock seats. Following seats unavailable: " + String.join(", ", unavailableSeats);
            logger.warn("Seat locking failed for trip {}: {}", tripId, message);
            throw new SeatUnavailableException(message);
        }

        // If loop completes without finding unavailable seats, proceed to lock
        int lockedCount = 0;
        for (String seatNum : seatNumbers) {
            currentStatusMap.put(seatNum, ScheduledTrip.SeatStatus.LOCKED);
            lockedCount++;
        }

        int currentAvailable = trip.getAvailableSeats() != null ? trip.getAvailableSeats() : 0;
        trip.setAvailableSeats(Math.max(0, currentAvailable - lockedCount));

        scheduledTripRepository.save(trip);
        logger.info("Successfully locked {} seats for trip ID {} by user ID {}", lockedCount, tripId, userId);
        return true;
    }
}