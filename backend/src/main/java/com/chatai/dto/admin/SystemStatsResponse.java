package com.chatai.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatsResponse {
    
    private Long totalUsers;
    private Long activeUsers;
    private Long totalSessions;
    private Long totalMessages;
    private Long todaySessions;
    private Long todayMessages;
    private Map<String, Integer> modelUsageStats;
    private Map<String, Integer> dailyUserActivity; // Last 7 days
}
