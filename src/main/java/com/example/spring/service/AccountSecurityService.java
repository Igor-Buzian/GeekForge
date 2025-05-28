package com.example.spring.service;

import com.example.spring.entity.User;
import com.example.spring.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service class for managing user account security, particularly focusing on
 * handling failed login attempts and account locking mechanisms.
 */
@Service
@RequiredArgsConstructor
public class AccountSecurityService {
    @Value("${security.account.max_failed_attempts}")
    private int max_failed_attempts;
    @Value("${security.account.lock_duration_hours}")
    private byte lock_duration_hours;

    /**
     * Returns the maximum number of failed login attempts allowed before an account is locked.
     *
     * @return The maximum number of failed attempts.
     */
    public int getMax_failed_attempts() {
        return max_failed_attempts;
    }

    private final UserRepository userRepository;

    /**
     * Checks if a user's account is currently locked. An account is considered locked
     * if its lock time is set and the current time is before the calculated unlock time.
     * If the lock time has passed, the failed attempts are reset.
     *
     * @param user The user object to check.
     * @return true if the account is locked, false otherwise.
     */
    public boolean isAccountLocked(User user) {
        if (user.getLockTime() == null) {
            return false;
        }
        LocalDateTime banLocalDateTime = user.getLockTime().plusHours(lock_duration_hours);
        boolean isLocked = LocalDateTime.now().isAfter(banLocalDateTime);
        if (isLocked) {
            resetFailedAttempts(user);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Increments the failed login attempts for a given user. If the failed attempts
     * reach the maximum allowed and the CAPTCHA is invalid, the account will be locked
     * for a predefined duration. If CAPTCHA is valid, it prevents immediate lock.
     *
     * @param user The user whose failed attempts need to be incremented.
     * @param isCaptchaValid A boolean indicating if the CAPTCHA validation was successful.
     */
    @Transactional
    public void IncrementFailedAttempts(User user, boolean isCaptchaValid) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        if (user.getFailedAttempts() >= max_failed_attempts && !isCaptchaValid) {
            user.setLockTime(LocalDateTime.now().plusHours(lock_duration_hours));
        } else if (isCaptchaValid) {
            user.setFailedAttempts(max_failed_attempts -1); // Allow one more attempt if CAPTCHA was valid
        }
        userRepository.save(user);
    }

    /**
     * Resets the failed login attempts and unlocks the user's account.
     *
     * @param user The user whose failed attempts and lock time need to be reset.
     */
    @Transactional
    public void resetFailedAttempts(User user) {
        user.setLockTime(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
    }
}