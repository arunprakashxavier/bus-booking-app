// src/main/java/com/guvi/busapp/repository/PassengerRepository.java
package com.guvi.busapp.repository;

import com.guvi.busapp.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
}