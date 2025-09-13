package com.chatai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Zettix AI Chat Backend");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "Zettix AI Chat Backend");
        info.put("description", "Backend for AI Chat integration By Zettix Team");
        info.put("version", "1.0.0");
        info.put("developed_by", "Zettix Team");
        info.put("features", new String[]{
            "JWT Authentication",
            "Role-based Access Control",
            "Chat History Management", 
            "Multiple AI Models Support",
            "Request Limiting",
            "Admin Dashboard",
            "Thinking Process Support"
        });
        
        return ResponseEntity.ok(info);
    }
}
