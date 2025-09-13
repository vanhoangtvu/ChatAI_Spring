package com.chatai.service;

import com.chatai.dto.chat.ChatRequest;
import com.chatai.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final GroqService groqService;
    private final ChatHistoryService chatHistoryService;
    
    public Flux<String> processChatStream(ChatRequest request) {
        try {
            log.info("Processing streaming chat request for model: {}", request.getModel());
            
            return groqService.chatStream(
                request.getMessage(), 
                request.getTemperature(), 
                request.getMaxTokens(), 
                request.getModel()
            )
            .doOnSubscribe(s -> log.debug("Stream subscribed for model: {}", request.getModel()))
            .doOnNext(chunk -> log.trace("Streaming chunk: {}", chunk))
            .doOnComplete(() -> log.debug("Stream completed for model: {}", request.getModel()))
            .doOnError(e -> log.error("Stream error for model: {}", request.getModel(), e));
                
        } catch (Exception e) {
            log.error("Error processing streaming chat request", e);
            return Flux.error(new RuntimeException("Failed to process streaming request: " + e.getMessage()));
        }
    }
    
    public Flux<String> processChatStreamWithHistory(ChatRequest request, Long sessionId, Long userId) {
        StringBuilder responseBuilder = new StringBuilder();
        StringBuilder thinkingBuilder = new StringBuilder();
        
        // Get conversation history for context
        List<ChatMessage> conversationHistory = chatHistoryService.getSessionMessages(sessionId, userId);
        
        log.info("🔍 Conversation History Debug - SessionId: {}, UserId: {}, HistorySize: {}", 
                sessionId, userId, conversationHistory.size());
        
        // Log first few messages for debugging
        for (int i = 0; i < Math.min(conversationHistory.size(), 3); i++) {
            ChatMessage msg = conversationHistory.get(i);
            log.debug("History[{}]: {} - {}", i, msg.getRole(), 
                    msg.getContent().length() > 50 ? msg.getContent().substring(0, 50) + "..." : msg.getContent());
        }
        
        return groqService.chatStreamWithHistory(
                request.getMessage(), 
                request.getTemperature(), 
                request.getMaxTokens(), 
                request.getModel(),
                conversationHistory
            )
            .doOnNext(chunk -> {
                // Parse chunk to extract content and thinking for history
                try {
                    if (!chunk.trim().equals("[DONE]")) {
                        String content = extractContentFromChunk(chunk);
                        if (content != null && !content.isEmpty()) {
                            if (content.contains("<think>")) {
                                String thinking = extractThinkingFromContent(content);
                                if (thinking != null) {
                                    thinkingBuilder.append(thinking);
                                }
                                content = removeThinkingFromContent(content);
                            }
                            responseBuilder.append(content);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parsing streaming chunk for history: {}", e.getMessage());
                }
            })
            .doOnComplete(() -> {
                // Save assistant response to history
                String finalResponse = responseBuilder.toString();
                String finalThinking = thinkingBuilder.toString();
                
                if (!finalResponse.trim().isEmpty() || !finalThinking.trim().isEmpty()) {
                    try {
                        chatHistoryService.saveMessage(
                            sessionId, 
                            finalResponse, 
                            finalThinking.isEmpty() ? null : finalThinking,
                            ChatMessage.MessageRole.ASSISTANT, 
                            request.getModel(), 
                            null // Token count not available in streaming
                        );
                        log.debug("Saved assistant response to session: {}", sessionId);
                    } catch (Exception e) {
                        log.error("Error saving assistant response to history: {}", e.getMessage());
                    }
                }
            })
            .doOnError(e -> {
                log.error("Stream error for session {}: {}", sessionId, e.getMessage());
                // Don't rethrow to avoid breaking the stream
            });
    }
    
    private String extractContentFromChunk(String chunk) {
        try {
            // First try to extract from "content" field
            if (chunk.contains("\"content\":\"")) {
                int start = chunk.indexOf("\"content\":\"") + 11;
                int end = chunk.indexOf("\"", start);
                if (end > start) {
                    String content = chunk.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
                    if (!content.trim().isEmpty()) {
                        return content;
                    }
                }
            }
            
            // Then try to extract from "reasoning" field (some models use this)
            if (chunk.contains("\"reasoning\":\"")) {
                int start = chunk.indexOf("\"reasoning\":\"") + 13;
                int end = chunk.indexOf("\"", start);
                if (end > start) {
                    String reasoning = chunk.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
                    if (!reasoning.trim().isEmpty()) {
                        return reasoning;
                    }
                }
            }
            
            log.debug("No extractable content found in chunk: {}", chunk.length() > 100 ? chunk.substring(0, 100) + "..." : chunk);
        } catch (Exception e) {
            log.warn("Error extracting content from chunk: {}", e.getMessage());
        }
        return null;
    }
    
    private String extractThinkingFromContent(String content) {
        if (content.contains("<think>") && content.contains("</think>")) {
            int start = content.indexOf("<think>") + 7;
            int end = content.indexOf("</think>");
            if (end > start) {
                return content.substring(start, end).trim();
            }
        }
        return null;
    }
    
    private String removeThinkingFromContent(String content) {
        if (content.contains("<think>") && content.contains("</think>")) {
            return content.replaceAll("<think>.*?</think>", "").trim();
        }
        return content;
    }
}
