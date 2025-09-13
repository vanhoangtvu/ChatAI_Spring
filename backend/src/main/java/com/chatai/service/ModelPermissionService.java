package com.chatai.service;

import com.chatai.entity.User;
import com.chatai.entity.UserModelPermission;
import com.chatai.repository.UserModelPermissionRepository;
import com.chatai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelPermissionService {
    
    private final UserModelPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    
    public boolean hasModelAccess(Long userId, String modelId) {
        // Admin has access to all models
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getDailyRequestLimit() == -1) {
            return true;
        }
        
        // Check specific model permission
        Optional<UserModelPermission> permission = permissionRepository.findByUserIdAndModelId(userId, modelId);
        return permission.map(UserModelPermission::getIsAllowed).orElse(true); // Default allow
    }
    
    @Transactional
    public UserModelPermission grantModelAccess(Long userId, String modelId, String modelName, Integer dailyLimit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserModelPermission permission = permissionRepository.findByUserIdAndModelId(userId, modelId)
                .orElse(UserModelPermission.builder()
                        .user(user)
                        .modelId(modelId)
                        .modelName(modelName)
                        .build());
        
        permission.setIsAllowed(true);
        permission.setDailyRequestLimit(dailyLimit);
        permission.setRequestsUsedToday(0);
        permission.setLastRequestReset(LocalDateTime.now());
        
        return permissionRepository.save(permission);
    }
    
    @Transactional
    public void revokeModelAccess(Long userId, String modelId) {
        Optional<UserModelPermission> permission = permissionRepository.findByUserIdAndModelId(userId, modelId);
        if (permission.isPresent()) {
            permission.get().setIsAllowed(false);
            permissionRepository.save(permission.get());
        }
    }
    
    public List<UserModelPermission> getUserModelPermissions(Long userId) {
        return permissionRepository.findByUserId(userId);
    }
    
    public List<UserModelPermission> getModelUsers(String modelId) {
        return permissionRepository.findByModelId(modelId);
    }
}
