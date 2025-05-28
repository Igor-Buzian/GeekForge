package com.example.spring.service;

import com.example.spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service class for performing various validation checks.
 * This includes validation for email, password, and roles.
 */
@Service
@RequiredArgsConstructor
public class ValidationService {

    /**
     * Validates if the current email matches an email retrieved from the database.
     *
     * @param currentEmail The email string provided for validation.
     * @param emailInBd The email string retrieved from the database.
     * @return true if the emails match, false otherwise.
     */
    public Boolean validationEmail(String currentEmail, String emailInBd){
        return currentEmail.equals(emailInBd);
    }

    /**
     * Validates if the current password matches a confirmPassword.
     * Note: For security, passwords should ideally be validated after hashing using a password encoder,
     * not by direct string comparison in a production environment.
     *
     * @param currentPassword The password string provided for validation.
     * @param confirmPassword The confirm password string.
     * @return true if the passwords match, false otherwise.
     */
    public Boolean validationPassword(String currentPassword, String confirmPassword){
        return currentPassword.equals(confirmPassword);
    }

    /**
     * Validates if the current role name matches a role name retrieved from the database.
     *
     * @param currentRole The role name string provided for validation.
     * @param roleInBd The role name string retrieved from the database.
     * @return true if the role names match, false otherwise.
     */
    public Boolean validationRole(String currentRole, String roleInBd){
        return currentRole.equals(roleInBd);
    }
}