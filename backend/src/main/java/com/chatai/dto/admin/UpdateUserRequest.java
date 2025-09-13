package com.chatai.dto.admin;

import lombok.Data;

@Data
public class UpdateUserRequest {
    
    private String fullName;
    private String email;
    private Boolean isActive;
    private Integer dailyRequestLimit;
    private String[] roles; // Array of role names like ["USER", "ADMIN"]
}
