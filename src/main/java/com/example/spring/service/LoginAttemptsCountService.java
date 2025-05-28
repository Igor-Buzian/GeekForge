package com.example.spring.service;

import org.springframework.stereotype.Service;

/**
 * Service class to hold a simple counter for login attempts.
 */
@Service
public class LoginAttemptsCountService {
   /**
    * A public integer variable to store the count of login attempts.
    * This variable is not thread-safe and will reset if the application restarts.
    */
   public int  loginAttemptsCount;
}