package com.chatai.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelManagementRequest {
    
    @NotBlank(message = "Model ID is required")
    private String modelId;
    
    @NotBlank(message = "Model name is required")
    private String modelName;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Enabled status is required")
    private Boolean isEnabled;
    
    private String reason;
    
    @NotNull(message = "Default status is required")
    private Boolean isDefault;
    
    private Integer priority;
    
    private String groqModelId;
}
