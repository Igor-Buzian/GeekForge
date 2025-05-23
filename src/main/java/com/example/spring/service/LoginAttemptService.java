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

    private final  CaptchaService captchaService;

    Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    ScheduledExecutorService cleaner;

    //init method where we will delete in ConcurrentHashMap all old entities
    @PostConstruct
    public void init() {
        cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleWithFixedDelay(this::removeOldEntries, clear_old_entities, clear_old_entities, TimeUnit.HOURS);
    }

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
                info.attempts = capthca_count;
                info.lastAttempt = LocalDateTime.now().plusMinutes(block_time_in_minutes);
                return false;
            }
        }
    }

    public boolean isBloked(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null) return false;

        if (!LocalDateTime.now().isBefore(info.lastAttempt)) {
            attempts.remove(ip);
            return false;
        }

        if (info.attempts >= count_attempts) {
            return true;
        }
        return false;
    }

    public void loginSuccess(String ip) {
        attempts.remove(ip);
    }

    public void removeOldEntries() {
        LocalDateTime now = LocalDateTime.now();
        attempts.entrySet().removeIf(entry ->

        {
            return entry.getValue().lastAttempt.isBefore(LocalDateTime.now());
        });

    }

    public int AttemptsCount(String ip){
        AttemptInfo attemptInfo =attempts.get(ip);
        return attemptInfo ==null ? 0 : attemptInfo.attempts;
    }

    @PreDestroy
    public void destroy() {
        cleaner.shutdown();
    }

    private static class AttemptInfo {
        int attempts;
        LocalDateTime lastAttempt;

        AttemptInfo(int attempts, LocalDateTime lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }
    }
}
