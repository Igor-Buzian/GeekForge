package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for JWT authentication responses.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
}