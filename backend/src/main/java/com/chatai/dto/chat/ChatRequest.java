package com.chatai.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ChatRequest {
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotNull(message = "Model must be specified")
    private String model;
    
    @DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 2.0")
    @DecimalMax(value = "2.0", message = "Temperature must be between 0.0 and 2.0")
    private Double temperature = 0.7;
    
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 4000, message = "Max tokens cannot exceed 4000")
    private Integer maxTokens = 1000;
    
    // Optional: existing session ID for continuing conversation
    private Long sessionId;
}
