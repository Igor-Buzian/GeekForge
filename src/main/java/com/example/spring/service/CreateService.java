package com.example.spring.service;

import com.example.spring.dto.RegisterDtoValues;
import com.example.spring.entity.Role;
import com.example.spring.entity.User;
import com.example.spring.exeption.InfoExeption;
import com.example.spring.repository.RoleRepository;
import com.example.spring.repository.UserRepository;
import com.example.spring.utils.JwtTokenUtils;
import io.jsonwebtoken.Header;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;

/**
 * Service class responsible for handling the creation of new user accounts.
 * This includes validation, password encoding, role assignment, and JWT token generation.
 */
@Service
@RequiredArgsConstructor
public class CreateService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final ValidationService validationService;
    private  final JwtTokenUtils jwtTokenUtils;
    private  final LoginAttemptService loginAttemptService;

    /**
     * Creates a new user account based on the provided registration data.
     * Performs checks for existing email, validates password confirmation, assigns a default role,
     * generates a JWT token, and sets it as an HttpOnly cookie.
     *
     * @param registerDtoValues The DTO containing the new user's registration details.
     * @param response The HttpServletResponse to add the JWT cookie to.
     * @param request The HttpServletRequest to get client IP.
     * @param captchaResponse The reCAPTCHA response from the client.
     * @return A ResponseEntity indicating the result of the user creation, typically a redirect on success or an error.
     */
    public ResponseEntity<?> createNewUser(RegisterDtoValues registerDtoValues, HttpServletResponse response, HttpServletRequest request, @RequestParam(name = "g-recaptcha-response", required = false) String captchaResponse){

        if(userRepository.existsByEmail(registerDtoValues.getEmail())){
            String ip = request.getRemoteAddr();
            if(loginAttemptService.isBloked(ip)) return ResponseEntity.status(HttpStatus.FORBIDDEN).header("Location", "/register?error=ip_banned").build();
            if(!loginAttemptService.validateCaptcha(ip, captchaResponse)) return ResponseEntity.status(HttpStatus.FORBIDDEN).header("Location", "/register?error=ip_banned").build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).header("Location","/register?error=exist_mail").build();
        }

        User user = new User();
        user.setEmail(registerDtoValues.getEmail());
        user.setPassword(passwordEncoder.encode(registerDtoValues.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        user.setUsername(registerDtoValues.getUsername());

        if(!validationService.validationPassword(registerDtoValues.getPassword(), registerDtoValues.getConfirmPassword())){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).header("Location","/register?error=different_password").build();
        }
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default role 'ROLE_USER' not found in database! Please ensure it's inserted."));


        user.setRoles(new HashSet<>(Collections.singletonList(defaultRole)));
        String token = jwtTokenUtils.generateToken(user);
        userRepository.save(user);

        Cookie cookie = new Cookie("Auth_cookie", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/success");
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}