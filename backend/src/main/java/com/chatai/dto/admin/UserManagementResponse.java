package com.chatai.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementResponse {
    
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Boolean isActive;
    private Integer dailyRequestLimit;
    private Integer requestsUsedToday;
    private LocalDateTime lastRequestReset;
    private LocalDateTime createdAt;
    private List<String> roles;
    private Integer totalSessions;
    private Integer totalMessages;
}
