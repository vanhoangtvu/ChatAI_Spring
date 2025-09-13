package com.chatai.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelsResponse {
    private List<ModelResponse> models;
    private Map<String, Object> usage;
}
