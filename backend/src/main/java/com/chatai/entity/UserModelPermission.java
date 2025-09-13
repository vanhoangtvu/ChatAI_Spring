package com.chatai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_model_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "model_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserModelPermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "model_id", nullable = false)
    private String modelId;
    
    @Column(name = "model_name")
    private String modelName;
    
    @Builder.Default
    @Column(name = "is_allowed")
    private Boolean isAllowed = true;
    
    @Builder.Default
    @Column(name = "requests_used_today")
    private Integer requestsUsedToday = 0;
    
    @Column(name = "daily_request_limit")
    private Integer dailyRequestLimit;
    
    @Column(name = "last_request_reset")
    private LocalDateTime lastRequestReset;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
