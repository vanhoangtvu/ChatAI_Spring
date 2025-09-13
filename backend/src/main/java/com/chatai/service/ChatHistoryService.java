package com.chatai.service;

import com.chatai.entity.ChatMessage;
import com.chatai.entity.ChatSession;
import com.chatai.entity.User;
import com.chatai.repository.ChatMessageRepository;
import com.chatai.repository.ChatSessionRepository;
import com.chatai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public ChatSession createNewSession(Long userId, String modelUsed) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ChatSession session = ChatSession.builder()
                .title("New Chat")
                .modelUsed(modelUsed)
                .user(user)
                .build();
        
        return chatSessionRepository.save(session);
    }
    
    @Transactional
    public ChatMessage saveMessage(Long sessionId, String content, String thinking, 
                                 ChatMessage.MessageRole role, String modelUsed, Integer tokensUsed) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        
        ChatMessage message = ChatMessage.builder()
                .chatSession(session)
                .role(role)
                .content(content)
                .thinking(thinking)
                .modelUsed(modelUsed)
                .tokensUsed(tokensUsed)
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Update session title if this is the first user message
        boolean needsUpdate = false;
        if (role == ChatMessage.MessageRole.USER) {
            // Count messages more efficiently
            long messageCount = chatMessageRepository.countByChatSessionId(session.getId());
            if (messageCount <= 1) {
                // Generate title from content directly instead of using entity method
                String title = content.length() > 50 ? content.substring(0, 47) + "..." : content;
                session.setTitle(title);
                needsUpdate = true;
            }
        }
        
        // Update session's last modified time
        session.setUpdatedAt(LocalDateTime.now());
        
        // Save session only once if needed
        if (needsUpdate) {
            chatSessionRepository.save(session);
        }
        
        return savedMessage;
    }
    
    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    public List<ChatMessage> getSessionMessages(Long sessionId, Long userId) {
        // Use a single query with JOIN to avoid N+1 problem
        return chatMessageRepository.findByChatSessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId);
    }
    
    public long getSessionMessageCount(Long sessionId, Long userId) {
        // Efficient count without loading messages
        return chatMessageRepository.countByChatSessionId(sessionId);
    }
    
    public Map<Long, Long> getSessionMessageCounts(Long userId) {
        // Get all message counts for user's sessions in one query
        return chatMessageRepository.findSessionMessageCountsByUserId(userId);
    }
    
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        // Use efficient query that includes user check
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Chat session not found or access denied"));
        
        chatSessionRepository.delete(session);
    }
    
    public Optional<ChatSession> getSession(Long sessionId, Long userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId);
    }
    
    @Transactional
    public ChatSession updateSessionTitle(Long sessionId, Long userId, String newTitle) {
        // Use efficient query that includes user check
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Chat session not found or access denied"));
        
        session.setTitle(newTitle);
        return chatSessionRepository.save(session);
    }
}
