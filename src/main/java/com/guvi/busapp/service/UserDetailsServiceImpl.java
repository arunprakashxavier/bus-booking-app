package com.guvi.busapp.service;

import com.guvi.busapp.model.CustomUserDetails;
import com.guvi.busapp.model.User; // **** Ensure this is your User entity: com.guvi.busapp.model.User ****
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

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // **** Explicitly type 'user' as your User entity ****
        com.guvi.busapp.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        // Convert our User Role to Spring Security's GrantedAuthority
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name()) // Now 'user.getRole()' should work
        );

        // Return our CustomUserDetails object
        return new CustomUserDetails(
                user.getId(),          // user.getId()
                user.getEmail(),       // user.getEmail()
                user.getPassword(),    // user.getPassword()
                user.getFirstName(),   // user.getFirstName()
                user.getLastName(),    // user.getLastName()
                authorities);
    }
}
