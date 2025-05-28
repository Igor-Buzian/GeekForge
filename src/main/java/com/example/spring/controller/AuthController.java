package com.example.spring.controller;

import com.example.spring.dto.JwtRequest;
import com.example.spring.dto.RegisterDtoValues;
import com.example.spring.service.AuthService;
import com.example.spring.service.CreateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for authentication-related operations.
 * Handles user registration, authentication, login attempts, and logout.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/v1")
public class AuthController {
    private  final AuthService authService;
    private  final CreateService createNewUser;

    /**
     * Handles user registration requests.
     * Validates the provided registration data to create a new user.
     *
     * @param registerDtoValues The DTO containing user registration details.
     * @param bindingResult     Holds the result of validation on the DTO.
     * @param response          The HttpServletResponse.
     * @param request           The HttpServletRequest.
     * @param captchaResponse   The reCAPTCHA response from the client.
     * @return A ResponseEntity indicating the success or failure of the registration.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@ModelAttribute() @Valid RegisterDtoValues registerDtoValues, BindingResult bindingResult, HttpServletResponse response, HttpServletRequest request, @RequestParam(name = "g-recaptcha-response", required = false) String captchaResponse){
        if(bindingResult.hasErrors()) return  ResponseEntity.status(HttpStatus.FORBIDDEN).header("Location","/register").build();
        return createNewUser.createNewUser(registerDtoValues, response, request,captchaResponse);
    }

    /**
     * Handles user authentication requests.
     * Attempts to authenticate the user based on the provided credentials.
     *
     * @param jwtRequest        The DTO containing user login credentials.
     * @param response          The HttpServletResponse.
     * @param request           The HttpServletRequest.
     * @param captchaResponse   The reCAPTCHA response from the client.
     * @return A ResponseEntity containing the authentication result (e.g., JWT token) or an error.
     */
    @PostMapping("/authentication")
    public ResponseEntity<?> auth(JwtRequest jwtRequest, HttpServletResponse response, HttpServletRequest request , @RequestParam(name = "g-recaptcha-response", required = false) String captchaResponse)
    {
        return authService.authUser(jwtRequest, request,response,captchaResponse);
    }

    /**
     * Retrieves the login attempts for the authentication.
     *
     * @return A ResponseEntity containing information about login attempts.
     */
    @GetMapping("/login-attempts")
    public ResponseEntity<?> getLoginAttempts() {
        return authService.getLoginAttempts();
    }

    /**
     * Handles user logout requests.
     * Invalidates the user's token.
     *
     * @param response The HttpServletResponse.
     * @return A ResponseEntity indicating the success or failure of the logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response)
    {
        return authService.logout(response);
    }

}