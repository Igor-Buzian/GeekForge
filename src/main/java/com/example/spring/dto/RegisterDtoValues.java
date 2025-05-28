package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for user registration.
 * This class encapsulates the data required to register a new user,
 * including validation constraints for each field.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDtoValues {
    @NotBlank(message = "Username is need to be")
    @Size(min = 2, max = 100, message = "Username is bigger then 100 or smaller then 2")
    private String username;

    @NotBlank(message = "Email is need to be")
    @Email(message = "it is incorrect email")
    private String email;

    @NotBlank(message = "Password is need to be")
    @Size(min = 7, max = 50, message = "password is bigger than 100 or less 7")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "Password must be at least 8 characters long and include uppercase, lowercase, and a digit.")
    private String password;

    @NotBlank(message = "Confirm password is need to be")
    private String ConfirmPassword;

}