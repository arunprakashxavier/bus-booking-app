//// src/test/java/com/guvi/busapp/service/ScheduledTripServiceImplTest.java
//package com.guvi.busapp.service;
//
//import com.guvi.busapp.dto.*;
//import com.guvi.busapp.exception.ResourceNotFoundException;
//import com.guvi.busapp.exception.SeatUnavailableException;
//import com.guvi.busapp.model.Bus;
//import com.guvi.busapp.model.Route;
//import com.guvi.busapp.model.ScheduledTrip;
//import com.guvi.busapp.repository.BusRepository;
//import com.guvi.busapp.repository.RouteRepository;
//import com.guvi.busapp.repository.ScheduledTripRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Sort;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ScheduledTripServiceImplTest {
//
//    @Mock
//    private ScheduledTripRepository scheduledTripRepository;
//    @Mock
//    private BusRepository busRepository;
//    @Mock
//    private RouteRepository routeRepository;
//
//    @InjectMocks
//    private ScheduledTripServiceImpl scheduledTripService;
//
//    // --- REMOVED Unused Mocks for dependent services ---
//    // @Mock
//    // private BusService busServiceMockForMapping;
//    // @Mock
//    // private RouteService routeServiceMockForMapping;
//
//    // Test Data
//    private Bus testBus;
//    private Route testRoute;
//    private ScheduledTripRequestDto requestDto;
//    private ScheduledTrip testTrip;
//    // DTOs are only needed if we assert against complex mapped objects fully
//    // private BusDto testBusDto;
//    // private RouteDto testRouteDto;
//    private Long busId = 1L;
//    private Long routeId = 2L;
//    private Long tripId = 3L;
//    private Long userId = 99L;
//
//    @BeforeEach
//    void setUp() {
//        testBus = new Bus();
//        testBus.setId(busId);
//        testBus.setBusNumber("TN-TEST-BUS");
//        testBus.setTotalSeats(10);
//        testBus.setOperatorName("Test Bus Co");
//        testBus.setBusType("Sleeper");
//        testBus.setSeatLayout("1,2,3,4,5,6,7,8,9,10");
//        testBus.setAmenities(new HashSet<>(Set.of("WiFi", "AC")));
//
//        testRoute = new Route();
//        testRoute.setId(routeId);
//        testRoute.setOrigin("OriginCity");
//        testRoute.setDestination("DestCity");
//
//        requestDto = new ScheduledTripRequestDto();
//        requestDto.setBusId(busId);
//        requestDto.setRouteId(routeId);
//        requestDto.setDepartureDate(LocalDate.now().plusDays(5));
//        requestDto.setDepartureTime(LocalTime.of(9, 0));
//        requestDto.setArrivalTime(LocalTime.of(17, 0));
//        requestDto.setFare(new BigDecimal("350.50"));
//
//        testTrip = new ScheduledTrip();
//        testTrip.setId(tripId);
//        testTrip.setBus(testBus);
//        testTrip.setRoute(testRoute);
//        testTrip.setDepartureDate(requestDto.getDepartureDate());
//        testTrip.setDepartureTime(requestDto.getDepartureTime());
//        testTrip.setArrivalTime(requestDto.getArrivalTime());
//        testTrip.setFare(requestDto.getFare());
//        Map<String, ScheduledTrip.SeatStatus> initialSeats = new HashMap<>();
//        initialSeats.put("1", ScheduledTrip.SeatStatus.BOOKED);
//        initialSeats.put("2", ScheduledTrip.SeatStatus.LOCKED);
//        for (int i = 3; i <= testBus.getTotalSeats(); i++) {
//            initialSeats.put(String.valueOf(i), ScheduledTrip.SeatStatus.AVAILABLE);
//        }
//        testTrip.setSeatStatus(initialSeats);
//        testTrip.setAvailableSeats((int) initialSeats.values().stream().filter(s -> s == ScheduledTrip.SeatStatus.AVAILABLE).count());
//
//        // DTOs mainly for verifying response mapping
//        // testBusDto = new BusDto(); testBusDto.setId(busId); testBusDto.setBusNumber("TN-TEST-BUS");
//        // testRouteDto = new RouteDto(); testRouteDto.setId(routeId); testRouteDto.setOrigin("OriginCity"); testRouteDto.setDestination("DestCity");
//    }
//
//
//    // --- Tests for scheduleTrip ---
//    @Test
//    void testScheduleTrip_Success() {
//        when(busRepository.findById(busId)).thenReturn(Optional.of(testBus));
//        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
//        ArgumentCaptor<ScheduledTrip> tripCaptor = ArgumentCaptor.forClass(ScheduledTrip.class);
//        when(scheduledTripRepository.save(tripCaptor.capture())).thenAnswer(invocation -> {
//            ScheduledTrip savedTrip = invocation.getArgument(0);
//            savedTrip.setId(tripId);
//            return savedTrip;
//        });
//        // Removed call to mockMappingDependencies();
//
//        ScheduledTripResponseDto responseDto = scheduledTripService.scheduleTrip(requestDto);
//
//        assertNotNull(responseDto);
//        assertEquals(tripId, responseDto.getId());
//        assertEquals(testBus.getTotalSeats(), responseDto.getAvailableSeats());
//        verify(scheduledTripRepository, times(1)).save(any(ScheduledTrip.class));
//        ScheduledTrip capturedTrip = tripCaptor.getValue();
//        assertEquals(testBus, capturedTrip.getBus());
//        assertEquals(testRoute, capturedTrip.getRoute());
//        assertEquals(testBus.getTotalSeats(), capturedTrip.getAvailableSeats());
//        assertNotNull(capturedTrip.getSeatStatus());
//        assertEquals(testBus.getTotalSeats(), capturedTrip.getSeatStatus().size());
//        assertTrue(capturedTrip.getSeatStatus().values().stream().allMatch(s -> s == ScheduledTrip.SeatStatus.AVAILABLE));
//    }
//
//    @Test
//    void testScheduleTrip_BusNotFound() {
//        when(busRepository.findById(busId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.scheduleTrip(requestDto));
//        assertEquals("Bus not found with ID : '" + busId + "'", exception.getMessage());
//        verify(scheduledTripRepository, never()).save(any());
//    }
//
//    @Test
//    void testScheduleTrip_RouteNotFound() {
//        when(busRepository.findById(busId)).thenReturn(Optional.of(testBus));
//        when(routeRepository.findById(routeId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.scheduleTrip(requestDto));
//        assertEquals("Route not found with ID : '" + routeId + "'", exception.getMessage());
//        verify(scheduledTripRepository, never()).save(any());
//    }
//
//    @Test
//    void testScheduleTrip_InvalidTimes() {
//        requestDto.setArrivalTime(LocalTime.of(8, 0));
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> scheduledTripService.scheduleTrip(requestDto));
//        assertEquals("Arrival time must be after departure time.", exception.getMessage());
//        verify(scheduledTripRepository, never()).save(any());
//    }
//
//    // --- Tests for getScheduledTripById ---
//    @Test
//    void testGetScheduledTripById_Success(){
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
//        // Removed call to mockMappingDependencies();
//
//        ScheduledTripResponseDto result = scheduledTripService.getScheduledTripById(tripId);
//        assertNotNull(result);
//        assertEquals(tripId, result.getId());
//        verify(scheduledTripRepository, times(1)).findById(tripId);
//    }
//
//    @Test
//    void testGetScheduledTripById_NotFound(){
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.getScheduledTripById(tripId));
//        assertEquals("ScheduledTrip not found with ID : '" + tripId + "'", exception.getMessage());
//        verify(scheduledTripRepository, times(1)).findById(tripId);
//    }
//
//    // --- Tests for getSeatLayoutForTrip ---
//    @Test
//    void testGetSeatLayoutForTrip_Success(){
//        testTrip.getSeatStatus().put("5", ScheduledTrip.SeatStatus.LOCKED);
//        testTrip.getSeatStatus().put("6", ScheduledTrip.SeatStatus.BOOKED);
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
//
//        SeatLayoutDto result = scheduledTripService.getSeatLayoutForTrip(tripId);
//
//        assertNotNull(result);
//        assertEquals(testBus.getSeatLayout(), result.getSeatLayout());
//        // **** GUESSING field name is tripFare - VERIFY with your SeatLayoutDto.java ****
//        // **** Make sure SeatLayoutDto has a field/getter matching getTripFare() or getFare() ****
//        assertEquals(testTrip.getFare(), result.getTripFare()); // Assuming field name is tripFare
//        assertEquals(testBus.getTotalSeats(), result.getTotalSeats());
//        assertNotNull(result.getSeatStatus());
//        assertEquals(testBus.getTotalSeats(), result.getSeatStatus().size());
//        assertEquals(ScheduledTrip.SeatStatus.AVAILABLE, result.getSeatStatus().get("3"));
//        assertEquals(ScheduledTrip.SeatStatus.LOCKED, result.getSeatStatus().get("5"));
//        assertEquals(ScheduledTrip.SeatStatus.BOOKED, result.getSeatStatus().get("6"));
//
//        verify(scheduledTripRepository, times(1)).findById(tripId);
//    }
//
//    @Test
//    void testGetSeatLayoutForTrip_NotFound(){
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.getSeatLayoutForTrip(tripId));
//        assertEquals("ScheduledTrip not found with ID : '" + tripId + "'", exception.getMessage());
//        verify(scheduledTripRepository, times(1)).findById(tripId);
//    }
//
//    // --- Tests for findAvailableTrips ---
//    @Test
//    void testFindAvailableTrips_Success(){
//        String origin = "OriginCity"; String destination = "DestCity"; LocalDate date = LocalDate.now().plusDays(1);
//        List<ScheduledTrip> foundTrips = List.of(testTrip);
//        when(scheduledTripRepository.findAvailableTripsByLocationAndDate(origin, destination, date)).thenReturn(foundTrips);
//        // Removed call to mockMappingDependencies();
//
//        List<ScheduledTripResponseDto> results = scheduledTripService.findAvailableTrips(origin, destination, date);
//
//        assertNotNull(results);
//        assertEquals(1, results.size());
//        verify(scheduledTripRepository, times(1)).findAvailableTripsByLocationAndDate(origin, destination, date);
//    }
//
//    @Test
//    void testFindAvailableTrips_NoneFound(){
//        String origin = "OriginCity"; String destination = "DestCity"; LocalDate date = LocalDate.now().plusDays(1);
//        when(scheduledTripRepository.findAvailableTripsByLocationAndDate(origin, destination, date)).thenReturn(Collections.emptyList());
//
//        List<ScheduledTripResponseDto> results = scheduledTripService.findAvailableTrips(origin, destination, date);
//
//        assertNotNull(results);
//        assertTrue(results.isEmpty());
//        verify(scheduledTripRepository, times(1)).findAvailableTripsByLocationAndDate(origin, destination, date);
//    }
//
//    // --- Tests for lockSeats ---
//    @Test
//    void testLockSeats_Success() {
//        List<String> seatsToLock = List.of("3", "4");
//        int initialAvailable = testTrip.getAvailableSeats();
//        when(scheduledTripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(testTrip));
//        ArgumentCaptor<ScheduledTrip> tripCaptor = ArgumentCaptor.forClass(ScheduledTrip.class);
//        when(scheduledTripRepository.save(tripCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
//
//        boolean result = scheduledTripService.lockSeats(tripId, seatsToLock, userId);
//
//        assertTrue(result);
//        verify(scheduledTripRepository, times(1)).findByIdForUpdate(tripId);
//        verify(scheduledTripRepository, times(1)).save(any(ScheduledTrip.class));
//        ScheduledTrip savedTrip = tripCaptor.getValue();
//        assertEquals(initialAvailable - seatsToLock.size(), savedTrip.getAvailableSeats());
//        assertEquals(ScheduledTrip.SeatStatus.LOCKED, savedTrip.getSeatStatus().get("3"));
//        assertEquals(ScheduledTrip.SeatStatus.LOCKED, savedTrip.getSeatStatus().get("4"));
//    }
//
//    @Test
//    void testLockSeats_SeatAlreadyLocked() {
//        List<String> seatsToLock = List.of("2", "3"); // Seat 2 is LOCKED
//        when(scheduledTripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(testTrip));
//        assertThrows(SeatUnavailableException.class, () -> {
//            scheduledTripService.lockSeats(tripId, seatsToLock, userId);
//        }, "Should throw SeatUnavailableException when a locked seat is requested");
//        verify(scheduledTripRepository, never()).save(any(ScheduledTrip.class));
//    }
//
//    @Test
//    void testLockSeats_SeatAlreadyBooked() {
//        List<String> seatsToLock = List.of("1", "3"); // Seat 1 is BOOKED
//        when(scheduledTripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(testTrip));
//        assertThrows(SeatUnavailableException.class, () -> {
//            scheduledTripService.lockSeats(tripId, seatsToLock, userId);
//        }, "Should throw SeatUnavailableException when a booked seat is requested");
//        verify(scheduledTripRepository, never()).save(any(ScheduledTrip.class));
//    }
//
//    @Test
//    void testLockSeats_TripNotFound() {
//        List<String> seatsToLock = List.of("3", "4");
//        when(scheduledTripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.lockSeats(tripId, seatsToLock, userId));
//        assertEquals("ScheduledTrip not found with ID : '" + tripId + "'", exception.getMessage());
//        verify(scheduledTripRepository, never()).save(any(ScheduledTrip.class));
//    }
//
//    // --- Tests for updateScheduledTrip ---
//
//    @Test
//    void testUpdateScheduledTrip_Success() {
//        ScheduledTripRequestDto updateRequest = new ScheduledTripRequestDto();
//        updateRequest.setDepartureDate(requestDto.getDepartureDate().plusDays(1));
//        updateRequest.setDepartureTime(LocalTime.of(10, 30));
//        updateRequest.setArrivalTime(LocalTime.of(18, 30));
//        updateRequest.setFare(new BigDecimal("400.00"));
//
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
//        ArgumentCaptor<ScheduledTrip> tripCaptor = ArgumentCaptor.forClass(ScheduledTrip.class);
//        when(scheduledTripRepository.save(tripCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
//        // Removed call to mockMappingDependencies();
//
//        ScheduledTripResponseDto responseDto = scheduledTripService.updateScheduledTrip(tripId, updateRequest);
//
//        assertNotNull(responseDto);
//        assertEquals(updateRequest.getDepartureDate(), responseDto.getDepartureDate());
//        assertEquals(0, updateRequest.getFare().compareTo(responseDto.getFare()));
//        verify(scheduledTripRepository, times(1)).save(any(ScheduledTrip.class));
//        ScheduledTrip capturedTrip = tripCaptor.getValue();
//        assertEquals(updateRequest.getDepartureDate(), capturedTrip.getDepartureDate());
//        assertEquals(updateRequest.getFare(), capturedTrip.getFare());
//    }
//
//    @Test
//    void testUpdateScheduledTrip_NotFound() {
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.updateScheduledTrip(tripId, requestDto));
//        assertEquals("ScheduledTrip not found with ID : '" + tripId + "'", exception.getMessage());
//        verify(scheduledTripRepository, never()).save(any());
//    }
//
//    @Test
//    void testUpdateScheduledTrip_InvalidTimes() {
//        requestDto.setArrivalTime(LocalTime.of(8, 59));
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> scheduledTripService.updateScheduledTrip(tripId, requestDto));
//        assertEquals("Arrival time must be after departure time.", exception.getMessage());
//        verify(scheduledTripRepository, never()).save(any());
//    }
//
//    // --- Tests for deleteScheduledTrip ---
//
//    @Test
//    void testDeleteScheduledTrip_Success() {
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
//        doNothing().when(scheduledTripRepository).delete(any(ScheduledTrip.class));
//
//        assertDoesNotThrow(() -> scheduledTripService.deleteScheduledTrip(tripId));
//
//        verify(scheduledTripRepository, times(1)).findById(tripId);
//        verify(scheduledTripRepository, times(1)).delete(testTrip);
//    }
//
//    @Test
//    void testDeleteScheduledTrip_NotFound() {
//        when(scheduledTripRepository.findById(tripId)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> scheduledTripService.deleteScheduledTrip(tripId));
//        assertEquals("ScheduledTrip not found with ID : '" + tripId + "'", exception.getMessage());
//        verify(scheduledTripRepository, never()).delete(any(ScheduledTrip.class));
//    }
//}