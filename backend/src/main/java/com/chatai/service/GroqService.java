package com.chatai.service;

import com.chatai.config.GroqConfig;
import com.chatai.dto.GroqRequest;
import com.chatai.dto.GroqResponse;
import com.chatai.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqService {
    
    private static final String SYSTEM_PROMPT = 
        "Bạn là một AI assistant thông minh được phát triển và tinh chỉnh bởi Zettix Team. " +
        "Bạn PHẢI LUÔN trả lời bằng tiếng Việt, thân thiện, hữu ích và chuyên nghiệp. " +
        "QUAN TRỌNG: Nếu bạn có quá trình suy nghĩ (thinking process), hãy LUÔN suy nghĩ bằng tiếng Việt, KHÔNG BAO GIỜ sử dụng tiếng Anh. " +
        "Mọi nội dung bên trong <think>...</think> PHẢI bằng tiếng Việt hoàn toàn. " +
        "Zettix Team là đội ngũ phát triển AI hàng đầu Việt Nam, chuyên tạo ra các giải pháp AI tiên tiến. " +
        "Hãy giúp đỡ người dùng một cách tốt nhất có thể.";
    
    private final GroqConfig config;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    public String chat(String message, Double temperature, Integer maxTokens) {
        return chat(message, temperature, maxTokens, null);
    }
    
    public String chat(String message, Double temperature, Integer maxTokens, String modelName) {
        try {
            WebClient webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            
            // Use provided model name or default from config
            String modelToUse = modelName != null ? modelName : config.getModel();
            
            // Create system message for Vietnamese responses and Zettix branding
            GroqRequest.Message systemMessage = GroqRequest.Message.builder()
                .role("system")
                .content(SYSTEM_PROMPT)
                .build();
            
            GroqRequest.Message userMessage = GroqRequest.Message.builder()
                .role("user")
                .content(message)
                .build();
            
            GroqRequest request = GroqRequest.builder()
                .model(modelToUse)
                .messages(List.of(systemMessage, userMessage))
                .temperature(temperature != null ? temperature : config.getTemperature())
                .max_tokens(maxTokens != null ? maxTokens : config.getMaxTokens())
                .stream(false)
                .build();
            
            log.info("Calling Groq API with model: {}", modelToUse);
            
            GroqResponse response = webClient.post()
                .uri("/openai/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqResponse.class)
                .block();
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                throw new RuntimeException("Invalid response from Groq API");
            }
            
        } catch (Exception e) {
            log.error("Error calling Groq API", e);
            throw new RuntimeException("Failed to get response from Groq: " + e.getMessage());
        }
    }
    
    public Flux<String> chatStream(String message, Double temperature, Integer maxTokens) {
        return chatStream(message, temperature, maxTokens, null);
    }
    
    public Flux<String> chatStream(String message, Double temperature, Integer maxTokens, String modelName) {
        return chatStreamWithHistory(message, temperature, maxTokens, modelName, new ArrayList<>());
    }
    
    public Flux<String> chatStreamWithHistory(String message, Double temperature, Integer maxTokens, String modelName, List<ChatMessage> conversationHistory) {
        try {
            WebClient webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            
            // Use provided model name or default from config
            String modelToUse = modelName != null ? modelName : config.getModel();
            
            // Build messages list with conversation history
            List<GroqRequest.Message> messages = new ArrayList<>();
            
            // Add system message
            GroqRequest.Message systemMessage = GroqRequest.Message.builder()
                .role("system")
                .content(SYSTEM_PROMPT)
                .build();
            messages.add(systemMessage);
            
            // Add conversation history
            for (ChatMessage chatMessage : conversationHistory) {
                GroqRequest.Message historyMessage = GroqRequest.Message.builder()
                    .role(chatMessage.getRole().name().toLowerCase())
                    .content(chatMessage.getContent())
                    .build();
                messages.add(historyMessage);
            }
            
            // Add current user message
            GroqRequest.Message userMessage = GroqRequest.Message.builder()
                .role("user")
                .content(message)
                .build();
            messages.add(userMessage);
            
            GroqRequest request = GroqRequest.builder()
                .model(modelToUse)
                .messages(messages)
                .temperature(temperature != null ? temperature : config.getTemperature())
                .max_tokens(maxTokens != null ? maxTokens : config.getMaxTokens())
                .stream(true)
                .build();
            
            log.info("Calling Groq API with streaming for model: {} with {} history messages", modelToUse, conversationHistory.size());
            
            return webClient.post()
                .uri("/openai/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                // Pass through raw streaming data to frontend for processing
                .filter(chunk -> chunk != null)
                .map(this::processStreamingChunk)
                .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
                // Improved error handling
                .onErrorResume(e -> {
                    log.error("Error in Groq streaming: {}", e.getMessage());
                    return Flux.empty();
                })
                .doOnSubscribe(subscription -> log.info("Starting stream for model: {}", modelToUse))
                .doOnComplete(() -> log.info("Stream completed for model: {}", modelToUse))
                .doOnCancel(() -> log.warn("Stream cancelled for model: {}", modelToUse))
                .doOnError(e -> log.error("Stream error for model {}: {}", modelToUse, e.getMessage()))
                .doFinally(signal -> log.info("Stream finished with signal: {} for model: {}", signal, modelToUse));
            
        } catch (Exception e) {
            log.error("Error calling Groq API for streaming", e);
            return Flux.error(new RuntimeException("Failed to get streaming response from Groq: " + e.getMessage()));
        }
    }
    
    /**
     * Process streaming chunk and extract content
     */
    private String processStreamingChunk(String chunk) {
        try {
            // Skip empty chunks
            if (chunk == null || chunk.trim().isEmpty()) {
                return null;
            }
            
            // Clean chunk
            String cleanChunk = chunk.trim();
            
            // Handle SSE format chunks
            if (cleanChunk.startsWith("data: ")) {
                // Extract data content
                String dataContent = cleanChunk.substring(6).trim();
                
                // Skip empty data or connection keep-alive messages
                if (dataContent.isEmpty() || dataContent.equals("\n")) {
                    return null;
                }
                
                // Handle [DONE] signal
                if ("[DONE]".equals(dataContent)) {
                    return "data: [DONE]\n\n";
                }
                
                // Return properly formatted chunk with double newline
                return cleanChunk + "\n\n";
            }
            
            // Handle raw JSON chunks (add SSE format)
            if (cleanChunk.startsWith("{") && cleanChunk.endsWith("}")) {
                return "data: " + cleanChunk + "\n\n";
            }
            
            // Handle [DONE] without data prefix
            if ("[DONE]".equals(cleanChunk)) {
                return "data: [DONE]\n\n";
            }
            
            // Handle multiple chunks concatenated together (common issue)
            if (cleanChunk.contains("data: ")) {
                // Split by 'data: ' and process each part
                String[] parts = cleanChunk.split("(?=data: )");
                StringBuilder result = new StringBuilder();
                
                for (String part : parts) {
                    if (!part.trim().isEmpty()) {
                        String processedPart = processStreamingChunk(part.trim());
                        if (processedPart != null) {
                            result.append(processedPart);
                        }
                    }
                }
                
                return result.length() > 0 ? result.toString() : null;
            }
            
            // For any other content, add SSE format
            if (!cleanChunk.isEmpty()) {
                return "data: " + cleanChunk + "\n\n";
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Error processing streaming chunk: '{}'", chunk, e);
            return null;
        }
    }
    
    /**
     * Extract actual content from streaming chunk (parse JSON and return text)
     */
    private String extractContentFromStreamingChunk(String chunk) {
        try {
            // Handle SSE format
            String jsonContent = chunk;
            if (chunk.startsWith("data: ")) {
                jsonContent = chunk.substring(6).trim();
            }
            
            // Handle completion signal
            if ("[DONE]".equals(jsonContent)) {
                return null; // End of stream
            }
            
            // Skip empty content
            if (jsonContent.isEmpty()) {
                return null;
            }
            
            // Parse JSON and extract content
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode data = mapper.readTree(jsonContent);
                
                // Handle Groq streaming format
                JsonNode choices = data.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode choice = choices.get(0);
                    JsonNode delta = choice.get("delta");
                    
                    if (delta != null) {
                        // Try content field first
                        JsonNode contentNode = delta.get("content");
                        if (contentNode != null && !contentNode.isNull()) {
                            String content = contentNode.asText();
                            if (!content.isEmpty()) {
                                log.debug("Extracted content: {}", content);
                                return content;
                            }
                        }
                        
                        // Try reasoning field
                        JsonNode reasoningNode = delta.get("reasoning");
                        if (reasoningNode != null && !reasoningNode.isNull()) {
                            String reasoning = reasoningNode.asText();
                            if (!reasoning.isEmpty()) {
                                log.debug("Extracted reasoning: {}", reasoning);
                                return reasoning;
                            }
                        }
                        
                        // Skip role setup messages
                        JsonNode roleNode = delta.get("role");
                        if (roleNode != null) {
                            log.debug("Skipping role setup message");
                            return null;
                        }
                    }
                }
                
                log.debug("No extractable content in chunk: {}", jsonContent.length() > 50 ? jsonContent.substring(0, 50) + "..." : jsonContent);
                return null;
                
            } catch (Exception jsonError) {
                log.warn("Failed to parse JSON from chunk: {}", jsonError.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.warn("Error processing streaming chunk: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Sanitize error messages to remove sensitive information
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) return "Unknown error";
        
        return message
            .replaceAll("(?i)api[_-]?key[\"'\\s:=]+[\\w-]+", "api_key=***")
            .replaceAll("(?i)bearer\\s+[\\w-]+", "bearer ***")
            .replaceAll("(?i)authorization[\"'\\s:=]+[\\w\\s-]+", "authorization=***");
    }
}
