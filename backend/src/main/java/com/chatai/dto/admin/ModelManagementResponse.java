package com.chatai.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelManagementResponse {
    
    private Long id;
    private String modelId;
    private String modelName;
    private String description;
    private String category;
    private boolean isEnabled;
    private String reason;
    private boolean isDefault;
    private Integer priority;
    private String groqModelId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
