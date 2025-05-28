package com.example.spring.confing;

import com.example.spring.entity.User;
import com.example.spring.service.UserService;
import com.example.spring.utils.JwtTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;

    /**
     * Extracts the JWT token from the "Auth_cookie" in the HTTP request's cookies.
     *
     * @param request The HttpServletRequest to extract the cookie from.
     * @return The JWT token string if found, otherwise null.
     */
    public String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> authTokenCookie = Arrays.stream(cookies)
                    .filter(cookie -> "Auth_cookie".equals(cookie.getName()))
                    .findFirst();
            return authTokenCookie.map(Cookie::getValue).orElse(null);
        }
        return null;
    }

    /**
     * Performs the internal filtering logic for JWT authentication.
     * @param request The HttpServletRequest being processed.
     * @param response The HttpServletResponse being processed.
     * @param filterChain The FilterChain to continue the request processing.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.debug("Processing JwtAuthFilter for protected URI: {}", requestURI);

        String token = getToken(request);
        if (token != null) {
            try {
                if (jwtTokenUtils.validateToken(token)) {
                    String currentUserEmail = jwtTokenUtils.getLogin(token);
                    User user = userService.loadLogin(currentUserEmail);
                    String roleName = jwtTokenUtils.getRoleFromToken(token).get(0);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user, null,
                            Collections.singleton(new SimpleGrantedAuthority(roleName))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    logger.debug("Authentication set for user: {}", currentUserEmail);
                } else {
                    logger.warn("Invalid JWT token for URI: {}", requestURI);
                    // Invalid token, but filter continues with Spring Security
                }
            } catch (Exception e) {
                // Log errors related to token processing (e.g., user not found, JWT key issues)
                logger.error("Error processing JWT token for URI {}: {}", requestURI, e.getMessage(), e);
            }
        } else {
            logger.debug("No JWT token found in cookies for URI: {}", requestURI);
        }

        filterChain.doFilter(request, response);
    }
}