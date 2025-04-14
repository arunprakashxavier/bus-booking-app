// src/main/java/com/guvi/busapp/dto/JwtResponseDto.java
package com.guvi.busapp.dto;

import lombok.Data;
import java.util.List;

@Data
public class JwtResponseDto {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private List<String> roles;
    private String firstName;
    private String lastName;


    public JwtResponseDto(String accessToken, Long id, String email, String firstName, String lastName, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
    }
}