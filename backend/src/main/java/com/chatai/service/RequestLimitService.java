package com.chatai.service;

import com.chatai.entity.User;
import com.chatai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestLimitService {
    
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public boolean canMakeRequest(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Admin has unlimited requests
        if (user.getDailyRequestLimit() == -1) {
            return true;
        }
        
        // Check if it's a new day - if so, assume reset will happen
        LocalDate today = LocalDate.now();
        LocalDateTime lastReset = user.getLastRequestReset();
        boolean isNewDay = lastReset == null || lastReset.toLocalDate().isBefore(today);
        
        int currentUsage = isNewDay ? 0 : user.getRequestsUsedToday();
        return currentUsage < user.getDailyRequestLimit();
    }
    
    @Transactional
    public void incrementRequestCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Don't count requests for admin
        if (user.getDailyRequestLimit() == -1) {
            return;
        }
        
        // Check if reset is needed and do it in one operation
        LocalDate today = LocalDate.now();
        LocalDateTime lastReset = user.getLastRequestReset();
        boolean needsReset = lastReset == null || lastReset.toLocalDate().isBefore(today);
        
        if (needsReset) {
            user.setRequestsUsedToday(1); // Reset to 1 (this request)
            user.setLastRequestReset(LocalDateTime.now());
            log.debug("Reset daily request counter for user: {} and incremented to 1", user.getUsername());
        } else {
            user.setRequestsUsedToday(user.getRequestsUsedToday() + 1);
        }
        
        userRepository.save(user);
        
        log.debug("User {} request count: {}/{}", 
                user.getUsername(), user.getRequestsUsedToday(), user.getDailyRequestLimit());
    }
    
    @Transactional(readOnly = true)
    public int getRemainingRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Admin has unlimited requests
        if (user.getDailyRequestLimit() == -1) {
            return -1;
        }
        
        // Check if it's a new day without modifying data
        LocalDate today = LocalDate.now();
        LocalDateTime lastReset = user.getLastRequestReset();
        boolean isNewDay = lastReset == null || lastReset.toLocalDate().isBefore(today);
        
        int currentUsage = isNewDay ? 0 : user.getRequestsUsedToday();
        return Math.max(0, user.getDailyRequestLimit() - currentUsage);
    }
}
