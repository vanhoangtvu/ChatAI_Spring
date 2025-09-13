package com.chatai.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private List<String> roles;
    private Integer dailyRequestLimit;
    private Integer requestsUsedToday;
    
    public JwtResponse(String accessToken, Long id, String username, String email, 
                      String fullName, List<String> roles, Integer dailyRequestLimit, 
                      Integer requestsUsedToday) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.dailyRequestLimit = dailyRequestLimit;
        this.requestsUsedToday = requestsUsedToday;
    }
}
