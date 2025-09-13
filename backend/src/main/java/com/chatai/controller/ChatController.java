package com.chatai.controller;

import com.chatai.dto.chat.ChatRequest;
import com.chatai.dto.chat.ChatSessionResponse;
import com.chatai.dto.chat.ModelResponse;
import com.chatai.dto.chat.ModelsResponse;
import com.chatai.entity.ChatMessage;
import com.chatai.entity.ChatSession;
import com.chatai.security.UserPrincipal;
import com.chatai.service.ChatHistoryService;
import com.chatai.service.ChatService;
import com.chatai.service.RequestLimitService;
import com.chatai.service.ModelManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;
    private final RequestLimitService requestLimitService;
    private final ModelManagementService modelManagementService;
    
    // Streaming endpoint with authentication and history
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request, 
                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting chat stream for model: {}", currentUser.getUsername(), request.getModel());
        
        // Check request limit
        if (!requestLimitService.canMakeRequest(currentUser.getId())) {
            return Flux.error(new RuntimeException("Daily request limit exceeded"));
        }
        
        // Increment request count
        requestLimitService.incrementRequestCount(currentUser.getId());
        
        // Create or get chat session
        ChatSession session;
        if (request.getSessionId() != null) {
            session = chatHistoryService.getSession(request.getSessionId(), currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Session not found or access denied"));
        } else {
            session = chatHistoryService.createNewSession(currentUser.getId(), request.getModel());
        }
        
        // Save user message FIRST
        chatHistoryService.saveMessage(session.getId(), request.getMessage(), null, 
                ChatMessage.MessageRole.USER, request.getModel(), null);
        
        // Process chat stream and save assistant response (now with user message in history)
        return chatService.processChatStreamWithHistory(request, session.getId(), currentUser.getId())
                .startWith("data: SESSION_ID:" + session.getId() + "\n\n");
    }
    
    // Chat History Endpoints
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getUserSessions(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Getting sessions for user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
        List<ChatSession> sessions = chatHistoryService.getUserSessions(currentUser.getId());
        
        // Get message counts efficiently in batch
        Map<Long, Long> messageCounts = chatHistoryService.getSessionMessageCounts(currentUser.getId());
        
        List<ChatSessionResponse> response = sessions.stream()
                .map(session -> ChatSessionResponse.builder()
                        .id(session.getId())
                        .title(session.getTitle())
                        .modelUsed(session.getModelUsed())
                        .createdAt(session.getCreatedAt())
                        .updatedAt(session.getUpdatedAt())
                        .messageCount(messageCounts.getOrDefault(session.getId(), 0L).intValue())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable Long sessionId,
                                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ChatMessage> messages = chatHistoryService.getSessionMessages(sessionId, currentUser.getId());
        ChatSession session = chatHistoryService.getSession(sessionId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<ChatSessionResponse.ChatMessageResponse> messageResponses = messages.stream()
                .map(msg -> ChatSessionResponse.ChatMessageResponse.builder()
                        .id(msg.getId())
                        .role(msg.getRole().name().toLowerCase())
                        .content(msg.getContent())
                        .thinking(msg.getThinking())
                        .modelUsed(msg.getModelUsed())
                        .tokensUsed(msg.getTokensUsed())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        ChatSessionResponse response = ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .modelUsed(session.getModelUsed())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(messages.size())
                .messages(messageResponses)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<String> deleteSession(@PathVariable Long sessionId,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        chatHistoryService.deleteSession(sessionId, currentUser.getId());
        return ResponseEntity.ok("Session deleted successfully");
    }
    
    @PutMapping("/sessions/{sessionId}/title")
    public ResponseEntity<ChatSessionResponse> updateSessionTitle(@PathVariable Long sessionId,
                                                                  @RequestParam String title,
                                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        ChatSession session = chatHistoryService.updateSessionTitle(sessionId, currentUser.getId(), title);
        
        // Count messages efficiently without loading the collection
        long messageCount = chatHistoryService.getSessionMessageCount(sessionId, currentUser.getId());
        
        ChatSessionResponse response = ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .modelUsed(session.getModelUsed())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount((int) messageCount)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/usage")
    public ResponseEntity<?> getUserUsage(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Getting usage for user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
        int remaining = requestLimitService.getRemainingRequests(currentUser.getId());
        
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("dailyLimit", currentUser.getId() == 1 ? -1 : 100); // Admin check
            put("remainingRequests", remaining);
            put("username", currentUser.getUsername());
        }});
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat API is running!");
    }
    
    @GetMapping("/models")
    public ResponseEntity<ModelsResponse> getAvailableModels() {
        try {
            // Get enabled models from database
            var enabledModels = modelManagementService.getEnabledModels();
            
            // Convert to DTO
            List<ModelResponse> modelDtos = enabledModels.stream()
                .map(model -> new ModelResponse(
                    model.getModelName(),
                    model.getModelId(), 
                    model.getDescription(),
                    model.getCategory()
                ))
                .collect(Collectors.toList());
            
            // Create usage info
            Map<String, Object> usage = Map.of(
                "streaming_endpoint", "POST /api/chat/stream",
                "note", "Model field is REQUIRED. Only streaming is supported."
            );
            
            ModelsResponse response = new ModelsResponse(modelDtos, usage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting available models: {}", e.getMessage());
            // Fallback to enabled models from database if service is not available
            List<ModelResponse> fallbackModels = List.of(
                new ModelResponse("Llama 3.1 8B Instant", "llama-3.1-8b-instant", 
                    "Fastest model, good for quick responses", "Llama"),
                new ModelResponse("Gemma2 9B", "gemma2-9b-it", 
                    "Google's Gemma2 model, 9B parameters", "Google")
            );
            
            Map<String, Object> usage = Map.of(
                "streaming_endpoint", "POST /api/chat/stream",
                "note", "Model field is REQUIRED. Only streaming is supported."
            );
            
            return ResponseEntity.ok(new ModelsResponse(fallbackModels, usage));
        }
    }
}
