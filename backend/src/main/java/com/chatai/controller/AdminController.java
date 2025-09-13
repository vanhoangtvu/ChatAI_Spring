package com.chatai.controller;

import com.chatai.dto.admin.SystemStatsResponse;
import com.chatai.dto.admin.UpdateUserRequest;
import com.chatai.dto.admin.UserManagementResponse;
import com.chatai.dto.admin.ModelManagementRequest;
import com.chatai.dto.admin.ModelManagementResponse;
import com.chatai.dto.auth.MessageResponse;
import com.chatai.service.AdminService;
import com.chatai.service.ModelManagementService;
import com.chatai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final ModelManagementService modelManagementService;
    
    @GetMapping("/users")
    public ResponseEntity<List<UserManagementResponse>> getAllUsers() {
        List<UserManagementResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/search")
    public ResponseEntity<List<UserManagementResponse>> searchUsers(@RequestParam String q) {
        List<UserManagementResponse> users = adminService.searchUsers(q);
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/users/{userId}")
    public ResponseEntity<UserManagementResponse> updateUser(@PathVariable Long userId, 
                                                           @RequestBody UpdateUserRequest request) {
        UserManagementResponse user = adminService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long userId) {
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok(MessageResponse.success("User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Cannot delete user: " + e.getMessage()));
        }
    }
    
    @PostMapping("/users/{userId}/reset-limit")
    public ResponseEntity<MessageResponse> resetUserDailyLimit(@PathVariable Long userId) {
        adminService.resetUserDailyLimit(userId);
        return ResponseEntity.ok(MessageResponse.success("Daily limit reset successfully"));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<SystemStatsResponse> getSystemStats() {
        SystemStatsResponse stats = adminService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> adminHealth() {
        return ResponseEntity.ok("Admin API is running!");
    }
    
    // Model Management Endpoints
    @GetMapping("/models")
    public ResponseEntity<List<ModelManagementResponse>> getAllModels() {
        List<ModelManagementResponse> models = modelManagementService.getAllModels();
        return ResponseEntity.ok(models);
    }
    
    @GetMapping("/models/enabled")
    public ResponseEntity<List<ModelManagementResponse>> getEnabledModels() {
        List<ModelManagementResponse> models = modelManagementService.getEnabledModels();
        return ResponseEntity.ok(models);
    }
    
    @GetMapping("/models/{modelId}")
    public ResponseEntity<ModelManagementResponse> getModelById(@PathVariable String modelId) {
        ModelManagementResponse model = modelManagementService.getModelById(modelId);
        return ResponseEntity.ok(model);
    }
    
    @PostMapping("/models")
    public ResponseEntity<ModelManagementResponse> createModel(@RequestBody ModelManagementRequest request,
                                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        ModelManagementResponse model = modelManagementService.createModel(request, currentUser.getUsername());
        return ResponseEntity.ok(model);
    }
    
    @PutMapping("/models/{modelId}")
    public ResponseEntity<ModelManagementResponse> updateModel(@PathVariable String modelId,
                                                             @RequestBody ModelManagementRequest request,
                                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        ModelManagementResponse model = modelManagementService.updateModel(modelId, request, currentUser.getUsername());
        return ResponseEntity.ok(model);
    }
    
    @PutMapping("/models/{modelId}/toggle")
    public ResponseEntity<ModelManagementResponse> toggleModel(@PathVariable String modelId,
                                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        ModelManagementResponse model = modelManagementService.toggleModel(modelId, currentUser.getUsername());
        return ResponseEntity.ok(model);
    }
    
    @DeleteMapping("/models/{modelId}")
    public ResponseEntity<MessageResponse> deleteModel(@PathVariable String modelId,
                                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            modelManagementService.deleteModel(modelId, currentUser.getUsername());
            return ResponseEntity.ok(MessageResponse.success("Model deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Cannot delete model: " + e.getMessage()));
        }
    }
}
