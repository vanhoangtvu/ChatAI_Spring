package com.chatai.service;

import com.chatai.dto.admin.SystemStatsResponse;
import com.chatai.dto.admin.UpdateUserRequest;
import com.chatai.dto.admin.UserManagementResponse;
import com.chatai.entity.Role;
import com.chatai.entity.User;
import com.chatai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserModelPermissionRepository userModelPermissionRepository;
    
    public List<UserManagementResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(this::mapToUserManagementResponse)
                .collect(Collectors.toList());
    }
    
    public List<UserManagementResponse> searchUsers(String search) {
        List<User> users = userRepository.searchUsers(search);
        
        return users.stream()
                .map(this::mapToUserManagementResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public UserManagementResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        if (request.getDailyRequestLimit() != null) {
            user.setDailyRequestLimit(request.getDailyRequestLimit());
        }
        
        // Update roles if provided
        if (request.getRoles() != null) {
            Set<Role> newRoles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(Role.RoleName.valueOf(roleName.toUpperCase()))
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                newRoles.add(role);
            }
            user.setRoles(newRoles);
        }
        
        User savedUser = userRepository.save(user);
        log.info("Admin updated user: {}", savedUser.getUsername());
        
        return mapToUserManagementResponse(savedUser);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Don't allow deleting admin user
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleName.ADMIN);
        
        if (isAdmin && user.getId() == 1) {
            throw new RuntimeException("Cannot delete default admin user");
        }
        
        userRepository.delete(user);
        log.info("Admin deleted user: {}", user.getUsername());
    }
    
    public SystemStatsResponse getSystemStats() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekAgo = today.minusDays(7);
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long totalSessions = chatSessionRepository.count();
        long totalMessages = chatMessageRepository.count();
        long todaySessions = chatSessionRepository.countSessionsCreatedAfter(today);
        long todayMessages = chatMessageRepository.countAssistantMessagesAfter(today);
        
        // Model usage stats - simplified
        Map<String, Integer> modelStats = new HashMap<>();
        modelStats.put("deepseek-r1-distill-llama-70b", 45);
        modelStats.put("llama-3.1-8b-instant", 32);
        modelStats.put("gemma2-9b-it", 18);
        modelStats.put("llama-3.3-70b-versatile", 25);
        
        // Daily activity - simplified  
        Map<String, Integer> dailyActivity = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = today.minusDays(i);
            String dateKey = date.toLocalDate().toString();
            // Simplified random data - in production, calculate real stats
            dailyActivity.put(dateKey, (int)(Math.random() * 50) + 10);
        }
        
        return SystemStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalSessions(totalSessions)
                .totalMessages(totalMessages)
                .todaySessions(todaySessions)
                .todayMessages(todayMessages)
                .modelUsageStats(modelStats)
                .dailyUserActivity(dailyActivity)
                .build();
    }
    
    @Transactional
    public void resetUserDailyLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRequestsUsedToday(0);
        user.setLastRequestReset(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Admin reset daily limit for user: {}", user.getUsername());
    }
    
    private UserManagementResponse mapToUserManagementResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        
        // Get user statistics
        int totalSessions = (int) chatSessionRepository.countByUserId(user.getId());
        int totalMessages = (int) chatMessageRepository.countUserMessagesAfter(user.getId(), 
                LocalDateTime.now().minusYears(10)); // All time
        
        return UserManagementResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .isActive(user.getIsActive())
                .dailyRequestLimit(user.getDailyRequestLimit())
                .requestsUsedToday(user.getRequestsUsedToday())
                .lastRequestReset(user.getLastRequestReset())
                .createdAt(user.getCreatedAt())
                .roles(roles)
                .totalSessions(totalSessions)
                .totalMessages(totalMessages)
                .build();
    }
}
