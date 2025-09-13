package com.chatai.repository;

import com.chatai.entity.ChatMessage;
import com.chatai.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession chatSession);
    
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
    
    long countByChatSessionId(Long chatSessionId);
    
    @Query("SELECT cm FROM ChatMessage cm JOIN cm.chatSession cs WHERE cs.id = :sessionId AND cs.user.id = :userId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByChatSessionIdAndUserIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
    
    @Query("SELECT cs.id as sessionId, COUNT(cm) as messageCount FROM ChatMessage cm JOIN cm.chatSession cs WHERE cs.user.id = :userId GROUP BY cs.id")
    List<Object[]> findSessionMessageCountsByUserIdRaw(@Param("userId") Long userId);
    
    default Map<Long, Long> findSessionMessageCountsByUserId(Long userId) {
        return findSessionMessageCountsByUserIdRaw(userId).stream()
                .collect(Collectors.toMap(
                    row -> (Long) row[0], // sessionId
                    row -> ((Number) row[1]).longValue()  // messageCount - handle different number types
                ));
    }
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatSession.user.id = :userId AND cm.createdAt >= :date")
    long countUserMessagesAfter(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    @Query("SELECT SUM(cm.tokensUsed) FROM ChatMessage cm WHERE cm.chatSession.user.id = :userId AND cm.createdAt >= :date")
    Long sumTokensUsedByUserAfter(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession.user.id = :userId AND cm.content LIKE %:search%")
    List<ChatMessage> searchUserMessages(@Param("userId") Long userId, @Param("search") String search);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.role = 'ASSISTANT' AND cm.createdAt >= :date")
    long countAssistantMessagesAfter(@Param("date") LocalDateTime date);
}
