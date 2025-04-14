// src/main/java/com/guvi/busapp/repository/UserRepository.java
package com.guvi.busapp.repository;

import com.guvi.busapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // findByUsername removed
    Optional<User> findByEmail(String email); // Find by email for login/checks

    // existsByUsername removed
    Boolean existsByEmail(String email); // Check if email exists
}