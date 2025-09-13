package com.chatai.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ChatRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;
    
    @NotNull(message = "Model must be specified")
    private String model; // Required model name for streaming
    
    private Double temperature;
    private Integer maxTokens;
}
