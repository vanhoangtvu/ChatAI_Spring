package com.chatai.repository;

import com.chatai.entity.ModelManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelManagementRepository extends JpaRepository<ModelManagement, Long> {
    
    Optional<ModelManagement> findByModelId(String modelId);
    
    List<ModelManagement> findByIsEnabledTrue();
    
    List<ModelManagement> findByCategory(String category);
    
    List<ModelManagement> findByIsEnabled(boolean isEnabled);
    
    @Query("SELECT m FROM ModelManagement m WHERE m.isEnabled = true ORDER BY m.priority DESC, m.modelName ASC")
    List<ModelManagement> findEnabledModelsOrdered();
    
    @Query("SELECT m FROM ModelManagement m ORDER BY m.priority DESC, m.modelName ASC")
    List<ModelManagement> findAllOrdered();
    
    boolean existsByModelId(String modelId);
    
    @Query("SELECT COUNT(m) FROM ModelManagement m WHERE m.isEnabled = true")
    long countEnabledModels();
}
