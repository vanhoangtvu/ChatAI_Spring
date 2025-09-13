package com.chatai.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class GroqRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer max_tokens;
    private Boolean stream;
    
    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
