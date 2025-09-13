package com.chatai.service;

import com.chatai.dto.admin.ModelManagementRequest;
import com.chatai.dto.admin.ModelManagementResponse;
import com.chatai.entity.ModelManagement;
import com.chatai.repository.ModelManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelManagementService {
    
    private final ModelManagementRepository modelManagementRepository;
    
    public List<ModelManagementResponse> getAllModels() {
        List<ModelManagement> models = modelManagementRepository.findAllOrdered();
        return models.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<ModelManagementResponse> getEnabledModels() {
        List<ModelManagement> models = modelManagementRepository.findEnabledModelsOrdered();
        return models.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public ModelManagementResponse getModelById(String modelId) {
        ModelManagement model = modelManagementRepository.findByModelId(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        return mapToResponse(model);
    }
    
    @Transactional
    public ModelManagementResponse createModel(ModelManagementRequest request, String adminUsername) {
        if (modelManagementRepository.existsByModelId(request.getModelId())) {
            throw new RuntimeException("Model with ID already exists: " + request.getModelId());
        }
        
        ModelManagement model = new ModelManagement();
        model.setModelId(request.getModelId());
        model.setModelName(request.getModelName());
        model.setDescription(request.getDescription());
        model.setCategory(request.getCategory());
        model.setIsEnabled(request.getIsEnabled());
        model.setReason(request.getReason());
        model.setIsDefault(request.getIsDefault());
        model.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        model.setGroqModelId(request.getGroqModelId());
        model.setUpdatedBy(adminUsername);
        
        ModelManagement savedModel = modelManagementRepository.save(model);
        log.info("Admin {} created model: {}", adminUsername, request.getModelId());
        
        return mapToResponse(savedModel);
    }
    
    @Transactional
    public ModelManagementResponse updateModel(String modelId, ModelManagementRequest request, String adminUsername) {
        ModelManagement model = modelManagementRepository.findByModelId(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        
        model.setModelName(request.getModelName());
        model.setDescription(request.getDescription());
        model.setCategory(request.getCategory());
        model.setIsEnabled(request.getIsEnabled());
        model.setReason(request.getReason());
        model.setIsDefault(request.getIsDefault());
        if (request.getPriority() != null) {
            model.setPriority(request.getPriority());
        }
        if (request.getGroqModelId() != null) {
            model.setGroqModelId(request.getGroqModelId());
        }
        model.setUpdatedBy(adminUsername);
        
        ModelManagement savedModel = modelManagementRepository.save(model);
        log.info("Admin {} updated model: {}", adminUsername, modelId);
        
        return mapToResponse(savedModel);
    }
    
    @Transactional
    public ModelManagementResponse toggleModel(String modelId, String adminUsername) {
        ModelManagement model = modelManagementRepository.findByModelId(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        
        boolean newStatus = !model.isEnabled();
        model.setIsEnabled(newStatus);
        model.setReason(newStatus ? null : "Disabled by admin " + adminUsername);
        model.setUpdatedBy(adminUsername);
        
        ModelManagement savedModel = modelManagementRepository.save(model);
        log.info("Admin {} {} model: {}", adminUsername, newStatus ? "enabled" : "disabled", modelId);
        
        return mapToResponse(savedModel);
    }
    
    @Transactional
    public void deleteModel(String modelId, String adminUsername) {
        ModelManagement model = modelManagementRepository.findByModelId(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        
        if (model.isDefault()) {
            throw new RuntimeException("Cannot delete default model: " + modelId);
        }
        
        modelManagementRepository.delete(model);
        log.info("Admin {} deleted model: {}", adminUsername, modelId);
    }
    
    
    private ModelManagementResponse mapToResponse(ModelManagement model) {
        return ModelManagementResponse.builder()
                .id(model.getId())
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .description(model.getDescription())
                .category(model.getCategory())
                .isEnabled(model.isEnabled())
                .reason(model.getReason())
                .isDefault(model.isDefault())
                .priority(model.getPriority())
                .groqModelId(model.getGroqModelId())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .updatedBy(model.getUpdatedBy())
                .build();
    }
}
