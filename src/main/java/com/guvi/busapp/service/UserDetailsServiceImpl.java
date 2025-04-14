// src/main/java/com/guvi/busapp/service/UserDetailsServiceImpl.java
package com.guvi.busapp.service;


import com.guvi.busapp.model.User;
import com.guvi.busapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional // Needed for LAZY fetching if accessing related entities
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find our custom User entity by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        // Convert our User Role to Spring Security's GrantedAuthority
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        // Return Spring Security's UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),       // Use email as the username for Spring Security context
                user.getPassword(),    // Provide the stored hashed password
                authorities);          // Provide the user's roles/authorities
    }
}