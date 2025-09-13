package com.chatai.dto.chat;

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
public class ChatSessionResponse {
    
    private Long id;
    private String title;
    private String modelUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int messageCount;
    private List<ChatMessageResponse> messages;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessageResponse {
        private Long id;
        private String role;
        private String content;
        private String thinking;
        private String modelUsed;
        private Integer tokensUsed;
        private LocalDateTime createdAt;
    }
}
