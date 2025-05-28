package com.example.spring.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Service for managing and tracking login attempts by IP address.
 * It provides mechanisms for incrementing failed attempts, blocking IPs,
 * validating CAPTCHA responses, and clearing old attempt records.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    @Getter
    @Value("${security.login.max-attempts}")
    private byte count_attempts;
    @Value("${security.login.block_time_in_minutes}")
    private byte block_time_in_minutes;
    @Value("${security.login.clear_old_entities}")
    private byte clear_old_entities;
    @Value("${security.login.time_for_save_ip}")
    private byte time_for_save_ip;
    @Value("${security.login.capthca_count}")
    private byte capthca_count;

    private final CaptchaService captchaService;

    // Stores IP addresses and their corresponding attempt information
    Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    ScheduledExecutorService cleaner;


    @PostConstruct
    public void init() {
        cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleWithFixedDelay(this::removeOldEntries, clear_old_entities, clear_old_entities, TimeUnit.HOURS);
    }

    /**
     * Records a failed login attempt for a given IP address.
     * Increments the attempt count and sets a lock time if the maximum attempts are reached.
     *
     * @param ip The IP address from which the failed login attempt originated.
     */
    public void loginFailed(String ip) {
        AttemptInfo info = attempts.computeIfAbsent(ip, k -> new AttemptInfo(0, null));
        info.attempts++;
        if (info.attempts >= count_attempts) {
            info.lastAttempt = LocalDateTime.now().plusMinutes(block_time_in_minutes);
        } else {
            info.lastAttempt = LocalDateTime.now().plusHours(time_for_save_ip);
        }

        attempts.put(ip, info);
    }

    /**
     * Validates the CAPTCHA response for a given IP address.
     * If the number of attempts is below the CAPTCHA threshold, it bypasses CAPTCHA validation.
     * Otherwise, it calls the {@link CaptchaService} to verify the response.
     * If CAPTCHA validation fails after the threshold, the IP is blocked.
     *
     * @param ip The IP address for which CAPTCHA is being validated.
     * @param captchaResponse The CAPTCHA response token from the client.
     * @return true if the CAPTCHA is valid or not required, false otherwise.
     */
    public boolean validateCaptcha(String ip, String captchaResponse) {
        AttemptInfo info = attempts.get(ip);
        if( info == null){
            return true;
        }
        else if(info.attempts < capthca_count){
            return true;
        }
        else
        {
            if( captchaService.isCaptchaValid(captchaResponse)){
                return  true;
            }
            else {
                info.attempts = capthca_count; // Set attempts to trigger block if CAPTCHA failed
                info.lastAttempt = LocalDateTime.now().plusMinutes(block_time_in_minutes);
                return false;
            }
        }
    }

    /**
     * Checks if a given IP address is currently blocked due to excessive failed login attempts.
     *
     * @param ip The IP address to check.
     * @return true if the IP address is blocked, false otherwise.
     */
    public boolean isBloked(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null) return false;

        // If the block time has passed, remove the entry and unblock
        if (!LocalDateTime.now().isBefore(info.lastAttempt)) {
            attempts.remove(ip);
            return false;
        }

        // Check if current attempts exceed the maximum allowed
        if (info.attempts >= count_attempts) {
            return true;
        }
        return false;
    }

    /**
     * Records a successful login for a given IP address by removing its entry from the attempts map.
     *
     * @param ip The IP address from which the successful login originated.
     */
    public void loginSuccess(String ip) {
        attempts.remove(ip);
    }

    /**
     * Removes old entries from the {@code attempts} map.
     * An entry is considered old if its {@code lastAttempt} timestamp is in the past.
     */
    public void removeOldEntries() {
        LocalDateTime now = LocalDateTime.now();
        attempts.entrySet().removeIf(entry ->
                entry.getValue().lastAttempt.isBefore(now)
        );
    }

    /**
     * Returns the number of failed login attempts for a given IP address.
     *
     * @param ip The IP address to query.
     * @return The number of failed attempts, or 0 if no attempts are recorded for the IP.
     */
    public int AttemptsCount(String ip){
        AttemptInfo attemptInfo =attempts.get(ip);
        return attemptInfo ==null ? 0 : attemptInfo.attempts;
    }

    /**
     * Shuts down the scheduled executor service when the application context is destroyed.
     * This prevents resource leaks.
     */
    @PreDestroy
    public void destroy() {
        if (cleaner != null) {
            cleaner.shutdown();
        }
    }

    /**
     * Private helper class to store information about login attempts for an IP address.
     */
    private static class AttemptInfo {
        int attempts;
        LocalDateTime lastAttempt;

        AttemptInfo(int attempts, LocalDateTime lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }
    }
}