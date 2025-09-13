package com.chatai.repository;

import com.chatai.entity.User;
import com.chatai.entity.UserModelPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserModelPermissionRepository extends JpaRepository<UserModelPermission, Long> {
    
    List<UserModelPermission> findByUser(User user);
    
    List<UserModelPermission> findByUserId(Long userId);
    
    Optional<UserModelPermission> findByUserAndModelId(User user, String modelId);
    
    Optional<UserModelPermission> findByUserIdAndModelId(Long userId, String modelId);
    
    List<UserModelPermission> findByModelId(String modelId);
    
    @Query("SELECT ump FROM UserModelPermission ump WHERE ump.user.id = :userId AND ump.isAllowed = true")
    List<UserModelPermission> findAllowedModelsForUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ump) FROM UserModelPermission ump WHERE ump.modelId = :modelId AND ump.isAllowed = true")
    long countUsersWithAccessToModel(@Param("modelId") String modelId);
}
