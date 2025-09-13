package com.chatai.repository;

import com.chatai.entity.ChatSession;
import com.chatai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    List<ChatSession> findByUserOrderByUpdatedAtDesc(User user);
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.id = :userId AND cs.title LIKE %:search%")
    List<ChatSession> searchUserSessions(@Param("userId") Long userId, @Param("search") String search);
    
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT cs FROM ChatSession cs WHERE cs.createdAt BETWEEN :startDate AND :endDate")
    List<ChatSession> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.createdAt >= :date")
    long countSessionsCreatedAfter(@Param("date") LocalDateTime date);
}
