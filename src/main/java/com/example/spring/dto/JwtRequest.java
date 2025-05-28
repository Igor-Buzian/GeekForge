package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for JWT authentication requests.
 * This class encapsulates the email and password provided by a user
 * during the login process, along with validation constraints.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtRequest {
    @NotBlank(message = "This field is empty ")
    @Email(message="Incorrect Email")
    private String email;

    @NotBlank(message = "This field is empty")
    @Size(min = 2, max = 100, message = "Username is bigger then 100 or smaller then 2")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "Password must be at least 8 characters long and include uppercase, lowercase, and a digit.")
    private String password;

}