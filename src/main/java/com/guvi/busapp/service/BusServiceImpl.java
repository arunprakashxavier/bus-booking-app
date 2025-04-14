// src/main/java/com/guvi/busapp/service/BusServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.BusDto;
import com.guvi.busapp.exception.ResourceNotFoundException; // Import custom exception
import com.guvi.busapp.model.Bus;
import com.guvi.busapp.repository.BusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;

    @Autowired
    public BusServiceImpl(BusRepository busRepository) {
        this.busRepository = busRepository;
    }

    // --- Helper Methods for Mapping ---

    private BusDto mapToDto(Bus bus) {
        if (bus == null) return null; // Added null check
        BusDto dto = new BusDto();
        dto.setId(bus.getId());
        dto.setBusNumber(bus.getBusNumber());
        dto.setOperatorName(bus.getOperatorName());
        dto.setBusType(bus.getBusType());
        dto.setTotalSeats(bus.getTotalSeats());
        dto.setAmenities(bus.getAmenities() != null ? new HashSet<>(bus.getAmenities()) : null);
        dto.setSeatLayout(bus.getSeatLayout()); // **** ADDED seatLayout mapping ****
        return dto;
    }

    private Bus mapToEntity(BusDto dto) {
        if (dto == null) return null;
        Bus bus = new Bus();
        // ID not set during creation
        bus.setBusNumber(dto.getBusNumber());
        bus.setOperatorName(dto.getOperatorName());
        bus.setBusType(dto.getBusType());
        bus.setTotalSeats(dto.getTotalSeats());
        bus.setAmenities(dto.getAmenities() != null ? new HashSet<>(dto.getAmenities()) : new HashSet<>());
        bus.setSeatLayout(dto.getSeatLayout());
        return bus;
    }

    // --- Service Method Implementations ---

    @Override
    @Transactional
    public BusDto createBus(BusDto busDto) {
        if (busRepository.existsByBusNumber(busDto.getBusNumber())) {
            throw new IllegalArgumentException("Bus with number '" + busDto.getBusNumber() + "' already exists.");
        }
        Bus bus = mapToEntity(busDto);
        Bus savedBus = busRepository.save(bus);
        return mapToDto(savedBus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusDto> getAllBuses() {
        List<Bus> buses = busRepository.findAll();
        return buses.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BusDto getBusById(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "ID", id));
        return mapToDto(bus);
    }

    @Override
    @Transactional
    public BusDto updateBus(Long id, BusDto busDto) {
        Bus existingBus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "ID", id));

        if (!existingBus.getBusNumber().equals(busDto.getBusNumber())) {
            Optional<Bus> conflictingBus = busRepository.findByBusNumber(busDto.getBusNumber());
            if (conflictingBus.isPresent() && !conflictingBus.get().getId().equals(id)) {
                throw new IllegalArgumentException("Another bus with number '" + busDto.getBusNumber() + "' already exists.");
            }
            existingBus.setBusNumber(busDto.getBusNumber());
        }

        // Update other fields
        existingBus.setOperatorName(busDto.getOperatorName());
        existingBus.setBusType(busDto.getBusType());
        existingBus.setTotalSeats(busDto.getTotalSeats());
        existingBus.setAmenities(busDto.getAmenities() != null ? new HashSet<>(busDto.getAmenities()) : new HashSet<>());
        existingBus.setSeatLayout(busDto.getSeatLayout());

        Bus updatedBus = busRepository.save(existingBus);
        return mapToDto(updatedBus);
    }

    @Override
    @Transactional
    public void deleteBus(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "ID", id));
        busRepository.delete(bus);
    }
}