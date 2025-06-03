package com.guvi.busapp.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User; // Spring's User

import java.util.Collection;

public class CustomUserDetails extends User { // Extend Spring's User
    private final Long id;
    private final String firstName;
    private final String lastName;
    // You can add email here too if you prefer, though getUsername() will return it.
    // private final String email;

    public CustomUserDetails(Long id, String email, String password, String firstName, String lastName,
                             Collection<? extends GrantedAuthority> authorities) {
        super(email, password, authorities); // Call super constructor
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        // this.email = email;
    }

    // Getters for the custom fields
    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    // public String getEmail() { return email; }
}
