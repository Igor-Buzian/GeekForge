package com.example.spring.service;

import com.example.spring.dto.JwtRequest;
import com.example.spring.entity.User;
import com.example.spring.exeption.InfoExeption;
import com.example.spring.repository.UserRepository;
import com.example.spring.utils.JwtTokenUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final PasswordEncoder passwordEncoder;
    private final  AccountSecurityService accountSecurityService;
    private  final  LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;

    public ResponseEntity<?>logout(HttpServletResponse response)
    {
        Cookie cookie = new Cookie("New_User", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/login");
        return  new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    public ResponseEntity<?> authUser(@RequestBody JwtRequest jwtRequest, HttpServletRequest request, HttpServletResponse response, @RequestParam(name = "g-recaptcha-response", required = false) String captchaResponse) {

        //if login not exist
        if(!userRepository.existsByEmail(jwtRequest.getEmail())){

            String ip = request.getRemoteAddr();
            loginAttemptService.loginFailed(ip);
            if (loginAttemptService.isBloked(ip)) {
                //  return new ResponseEntity<>(new InfoExeption(HttpStatus.FORBIDDEN.value(), "You have now ban for few time. Try some later"),HttpStatus.FORBIDDEN);
                return  ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/login?errpr=ip_banned").build();
            }

           // return new ResponseEntity<>(new InfoExeption(HttpStatus.FORBIDDEN.value(), "Password or login is Incorrect!"),HttpStatus.FORBIDDEN);
            return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","/login?error=incorrect").build();
        }

        User user = userService.loadLogin(jwtRequest.getEmail());

        if(!user.isEnabled()){
            //return new ResponseEntity<>(new InfoExeption(HttpStatus.FORBIDDEN.value(), "This account was Baned"),HttpStatus.FORBIDDEN);
            return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","/login?error=account_baned").build();
        }

        String ip = request.getRemoteAddr();

        if(loginAttemptService.isBloked(ip)) return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","/login?error=ip_banned").build();

        if(!loginAttemptService.validateCaptcha(ip, captchaResponse))   return  ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","/login?error=ip_banned").build();

        if(!passwordEncoder.matches(jwtRequest.getPassword(), user.getPassword())){
            accountSecurityService.IncrementFailedAttempts(user);
            if(accountSecurityService.isAccountLocked(user)){
                return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","login?error=account_banned").build();

            }
            else {
                int attemptsLeft = loginAttemptService.getCount_attempts() - user.getFailedAttempts();
                return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","/login?error=incorrect&attempt="+attemptsLeft).build();
            }
        }

        accountSecurityService.resetFailedAttempts(user);
        loginAttemptService.loginSuccess(ip);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                jwtRequest.getEmail(), jwtRequest.getPassword()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        String token = jwtTokenUtils.generateToken(user);

        Cookie cookie = new Cookie("Auth_cookie",token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        HttpHeaders headers =new HttpHeaders();
        headers.add("Location", "/success");
        return  new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);

    }

    public ResponseEntity<?> getLoginAttempts( HttpServletRequest request)
    {
        String ip = request.getRemoteAddr();
        return ResponseEntity.ok(Map.of("attempts", loginAttemptService.AttemptsCount(ip)));
    }
}
