// src/main/java/com/guvi/busapp/repository/RouteRepository.java
package com.guvi.busapp.repository;

import com.guvi.busapp.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    // Find routes by origin and destination
    List<Route> findByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);
    Boolean existsByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);
}