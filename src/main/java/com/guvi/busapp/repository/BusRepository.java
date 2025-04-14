// src/main/java/com/guvi/busapp/repository/BusRepository.java
package com.guvi.busapp.repository;

import com.guvi.busapp.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByBusNumber(String busNumber); // Find by unique number
    Boolean existsByBusNumber(String busNumber);
}