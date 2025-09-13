# 🚀 Hướng dẫn Tối ưu hóa và Troubleshooting - AI Chat Spring

## 📋 Mục lục

1. [Tối ưu hóa Định dạng AI Response](#tối-ưu-hóa-định-dạng-ai-response)
2. [Performance Optimization](#performance-optimization)
3. [Memory Management](#memory-management)
4. [Database Optimization](#database-optimization)
5. [Frontend Optimizations](#frontend-optimizations)
6. [API Response Optimization](#api-response-optimization)
7. [Caching Strategies](#caching-strategies)
8. [Error Handling & Logging](#error-handling--logging)
9. [Monitoring & Alerting](#monitoring--alerting)
10. [Deployment Optimizations](#deployment-optimizations)

---

## 🎨 Tối ưu hóa Định dạng AI Response

### Vấn đề hiện tại trong MessageContent.tsx và MarkdownRenderer.tsx

#### 1. **Performance Issues**
```typescript
// ❌ BEFORE: Inefficient regex processing
const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g;
while ((match = codeBlockRegex.exec(text)) !== null) {
  // Potential infinite loop
}

// ✅ AFTER: Optimized with safeguards
const formatCodeBlocks = useCallback((text: string) => {
  if (!text || text.length < 10) {
    return [{ type: 'text', content: text }];
  }
  
  const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g;
  let iterationCount = 0;
  const maxIterations = 100;
  
  while ((match = codeBlockRegex.exec(text)) !== null && iterationCount < maxIterations) {
    iterationCount++;
    // Safe processing
  }
}, []);
```

#### 2. **Regex Conflicts**
```typescript
// ❌ BEFORE: Conflicting patterns
.replace(/`([^`]+)`/g, '<code>$1</code>')  // Inline code
.replace(/```[\s\S]*?```/g, match => match) // Code blocks

// ✅ AFTER: Proper order and isolation
// Process code blocks first (preserve them)
html = html.replace(/```[\s\S]*?```/g, (match) => {
  return `__CODE_BLOCK_${codeBlocks.length - 1}__`;
});

// Then process inline code (avoiding code blocks)
html = html.replace(/`([^`\n]+)`/g, '<code class="inline-code">$1</code>');
```

#### 3. **Memory Leaks in Markdown Processing**
```typescript
// ❌ BEFORE: No cleanup, heavy processing
const processInlineCode = (text: string) => {
  const parts = [];
  let match;
  while ((match = /`([^`]+)`/g.exec(text)) !== null) {
    // No iteration limit, potential memory leak
    parts.push(/* heavy object */);
  }
  return parts;
};

// ✅ AFTER: Optimized with limits and cleanup
const processInlineCode = useCallback((text: string) => {
  if (!text || text.length < 3) return [{ type: 'text', content: text }];
  
  const parts = [];
  let iterationCount = 0;
  const maxIterations = 50; // Prevent infinite loops
  
  while ((match = inlineCodeRegex.exec(text)) !== null && iterationCount < maxIterations) {
    iterationCount++;
    
    if (match[1].length > 1000) {
      // Truncate very long inline code
      parts.push({
        type: 'inlinecode',
        content: match[1].substring(0, 1000) + '...'
      });
    } else {
      parts.push({ type: 'inlinecode', content: match[1] });
    }
  }
  
  return parts.length > 0 ? parts : [{ type: 'text', content: text }];
}, []);
```

### Giải pháp tối ưu: ImprovedMarkdownRenderer.tsx

#### **Key Improvements:**

1. **HTML Sanitization**
```typescript
// Escape HTML entities to prevent XSS and conflicts
html = html
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;');
```

2. **Smart List Processing**
```typescript
// Advanced list handling with nesting support
const processedLines = lines.map((line, index) => {
  const trimmedLine = line.trim();
  
  // Detect unordered lists with indentation levels
  const unorderedMatch = trimmedLine.match(/^(\s*)[-*+]\s+(.+)$/);
  if (unorderedMatch) {
    const level = Math.floor(unorderedMatch[1].length / 2);
    if (!inList || listType !== 'ul' || level !== listLevel) {
      inList = true;
      listType = 'ul';
      listLevel = level;
      return `<ul><li>${unorderedMatch[2]}</li>`;
    }
    return `<li>${unorderedMatch[2]}</li>`;
  }
  
  return line;
});
```

3. **Enhanced Styling**
```typescript
'& code.inline-code': {
  backgroundColor: isUser 
    ? 'rgba(255,255,255,0.15)'
    : alpha(theme.palette.background.default, 0.8),
  color: isUser 
    ? 'rgba(255,255,255,0.9)'
    : theme.palette.text.primary,
  padding: '2px 6px',
  borderRadius: '4px',
  fontSize: '0.875em',
  fontFamily: '"Fira Code", "JetBrains Mono", "SF Mono", "Monaco", monospace',
  border: `1px solid ${alpha(theme.palette.divider, 0.2)}`,
  fontWeight: 500,
}
```

---

## ⚡ Performance Optimization

### 1. Backend Performance

#### **Database Query Optimization**

```java
// ❌ BEFORE: N+1 query problem
@GetMapping("/sessions")
public List<ChatSessionResponse> getUserSessions(@AuthenticationPrincipal UserPrincipal currentUser) {
    List<ChatSession> sessions = chatHistoryService.getUserSessions(currentUser.getId());
    return sessions.stream()
        .map(session -> {
            int messageCount = session.getMessages().size(); // N+1 query!
            return ChatSessionResponse.builder()
                .id(session.getId())
                .messageCount(messageCount)
                .build();
        })
        .collect(toList());
}

// ✅ AFTER: Batch query optimization
@Query("SELECT s.id, COUNT(m) FROM ChatSession s LEFT JOIN s.messages m WHERE s.userId = :userId GROUP BY s.id")
List<Object[]> findSessionMessageCounts(@Param("userId") Long userId);

@GetMapping("/sessions")
public ResponseEntity<List<ChatSessionResponse>> getUserSessions(@AuthenticationPrincipal UserPrincipal currentUser) {
    List<ChatSession> sessions = chatHistoryService.getUserSessions(currentUser.getId());
    Map<Long, Long> messageCounts = chatHistoryService.getSessionMessageCounts(currentUser.getId());
    
    List<ChatSessionResponse> response = sessions.stream()
        .map(session -> ChatSessionResponse.builder()
            .id(session.getId())
            .title(session.getTitle())
            .messageCount(messageCounts.getOrDefault(session.getId(), 0L).intValue())
            .build())
        .collect(toList());
    
    return ResponseEntity.ok(response);
}
```

#### **Connection Pool Optimization**

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      validation-timeout: 5000
      leak-detection-threshold: 60000
```

#### **JVM Optimization**

```bash
# JVM tuning for production
export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication -XX:+OptimizeStringConcat \
  -Dspring.profiles.active=prod -Dserver.port=8080"
```

### 2. Streaming Performance

#### **Efficient Chunk Processing**

```java
@Service
public class OptimizedChatService {
    
    // ❌ BEFORE: Blocking processing
    public Flux<String> processChatStream(ChatRequest request) {
        return webClient
            .post()
            .bodyValue(buildRequest(request))
            .retrieve()
            .bodyToFlux(String.class)
            .map(this::processChunk); // Blocking operation
    }
    
    // ✅ AFTER: Non-blocking with backpressure handling
    public Flux<String> processChatStreamOptimized(ChatRequest request) {
        return webClient
            .post()
            .bodyValue(buildRequest(request))
            .retrieve()
            .bodyToFlux(String.class)
            .publishOn(Schedulers.boundedElastic()) // Non-blocking scheduler
            .map(this::processChunk)
            .onBackpressureBuffer(1000) // Handle backpressure
            .doOnError(error -> log.error("Stream error: {}", error.getMessage()))
            .onErrorResume(this::handleStreamError)
            .doFinally(signalType -> cleanupResources());
    }
    
    private String processChunk(String chunk) {
        // Optimized chunk processing
        if (chunk == null || chunk.isEmpty()) return "";
        
        // Remove data: prefix efficiently
        return chunk.startsWith("data: ") ? chunk.substring(6) : chunk;
    }
    
    private Flux<String> handleStreamError(Throwable error) {
        log.error("Chat stream error", error);
        return Flux.just("data: {\"error\": \"Stream interrupted\"}\n\n");
    }
}
```

### 3. Frontend Performance

#### **Component Optimization**

```typescript
// ❌ BEFORE: Re-rendering on every change
const MessageList: React.FC<MessageListProps> = ({ messages, isUser }) => {
  return (
    <div>
      {messages.map((message, index) => (
        <MessageContent 
          key={index}
          content={message.content}
          isUser={message.role === 'user'}
        />
      ))}
    </div>
  );
};

// ✅ AFTER: Optimized with memoization and virtualization
const MessageList: React.FC<MessageListProps> = React.memo(({ messages, isUser }) => {
  const [containerRef, setContainerRef] = useState<HTMLDivElement | null>(null);
  const [containerHeight, setContainerHeight] = useState(600);
  
  // Memoize message rendering
  const renderedMessages = useMemo(() => 
    messages.map((message) => ({
      id: message.id,
      content: message.content,
      isUser: message.role === 'user',
      timestamp: message.createdAt
    })), 
    [messages]
  );
  
  // Virtual scrolling for large message lists
  const rowRenderer = useCallback(({ index, key, style }) => (
    <div key={key} style={style}>
      <MemoizedMessageContent 
        content={renderedMessages[index].content}
        isUser={renderedMessages[index].isUser}
      />
    </div>
  ), [renderedMessages]);
  
  return (
    <div ref={setContainerRef} style={{ height: containerHeight }}>
      {renderedMessages.length > 100 ? (
        <List
          width="100%"
          height={containerHeight}
          rowCount={renderedMessages.length}
          rowHeight={({ index }) => calculateRowHeight(renderedMessages[index])}
          rowRenderer={rowRenderer}
          overscanRowCount={10}
        />
      ) : (
        renderedMessages.map((message) => (
          <MemoizedMessageContent 
            key={message.id}
            content={message.content}
            isUser={message.isUser}
          />
        ))
      )}
    </div>
  );
});

// Memoized message content component
const MemoizedMessageContent = React.memo(MessageContent, (prevProps, nextProps) => 
  prevProps.content === nextProps.content && 
  prevProps.isUser === nextProps.isUser
);
```

#### **Bundle Optimization**

```javascript
// webpack.config.js optimization
module.exports = {
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          priority: 10,
          enforce: true,
        },
        common: {
          name: 'common',
          minChunks: 2,
          priority: 5,
          reuseExistingChunk: true,
        },
        components: {
          name: 'components',
          test: /[\\/]src[\\/]components[\\/]/,
          priority: 20,
        }
      },
    },
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          compress: {
            drop_console: true,
            drop_debugger: true,
          },
        },
      }),
    ],
  },
};

// Dynamic imports for code splitting
const LazyAdminDashboard = lazy(() => 
  import('./components/Admin/AdminDashboard').then(module => ({
    default: module.AdminDashboard
  }))
);

const LazyChatInterface = lazy(() => 
  import('./components/Chat/ChatInterface').then(module => ({
    default: module.ChatInterface
  }))
);
```

---

## 💾 Memory Management

### 1. Backend Memory Optimization

#### **Entity Optimization**
```java
// ❌ BEFORE: Eager loading causing memory issues
@Entity
public class ChatSession {
    @OneToMany(mappedBy = "session", fetch = FetchType.EAGER) // Memory killer!
    private List<ChatMessage> messages;
}

// ✅ AFTER: Lazy loading with batch optimization
@Entity
public class ChatSession {
    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    @BatchSize(size = 50) // Load in batches of 50
    private Set<ChatMessage> messages = new LinkedHashSet<>();
    
    // Helper method for controlled loading
    @Transient
    public List<ChatMessage> getRecentMessages(int limit) {
        return messages.stream()
            .sorted(Comparator.comparing(ChatMessage::getCreatedAt).reversed())
            .limit(limit)
            .collect(toList());
    }
}
```

#### **DTO Projections**
```java
// ❌ BEFORE: Loading full entities
public List<ChatSession> getUserSessions(Long userId) {
    return sessionRepository.findByUserId(userId); // Loads everything!
}

// ✅ AFTER: Using projections for memory efficiency
public interface ChatSessionSummary {
    Long getId();
    String getTitle();
    String getModelUsed();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}

@Query("SELECT s.id as id, s.title as title, s.modelUsed as modelUsed, " +
       "s.createdAt as createdAt, s.updatedAt as updatedAt " +
       "FROM ChatSession s WHERE s.userId = :userId " +
       "ORDER BY s.updatedAt DESC")
List<ChatSessionSummary> findSessionSummariesByUserId(@Param("userId") Long userId);
```

### 2. Frontend Memory Management

#### **Cleanup Event Listeners**
```typescript
// ❌ BEFORE: Memory leaks from uncleaned listeners
const useChat = () => {
  useEffect(() => {
    const eventSource = new EventSource('/api/chat/stream');
    eventSource.onmessage = handleMessage;
    // Missing cleanup!
  }, []);
};

// ✅ AFTER: Proper cleanup
const useChat = () => {
  const eventSourceRef = useRef<EventSource | null>(null);
  
  useEffect(() => {
    return () => {
      // Cleanup on unmount
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, []);
  
  const startStream = useCallback(() => {
    // Close existing connection
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }
    
    eventSourceRef.current = new EventSource('/api/chat/stream');
    eventSourceRef.current.onmessage = handleMessage;
    eventSourceRef.current.onerror = handleError;
    
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, []);
};
```

#### **Image and Media Optimization**
```typescript
// ✅ Optimized image handling with lazy loading
const OptimizedImage: React.FC<{ src: string; alt: string }> = ({ src, alt }) => {
  const [imageSrc, setImageSrc] = useState<string>('');
  const [isLoaded, setIsLoaded] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);
  
  useEffect(() => {
    const img = imgRef.current;
    if (!img) return;
    
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            setImageSrc(src);
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.1 }
    );
    
    observer.observe(img);
    
    return () => {
      observer.disconnect();
    };
  }, [src]);
  
  return (
    <img
      ref={imgRef}
      src={imageSrc}
      alt={alt}
      onLoad={() => setIsLoaded(true)}
      style={{
        opacity: isLoaded ? 1 : 0,
        transition: 'opacity 0.3s ease',
        maxWidth: '100%',
        height: 'auto'
      }}
    />
  );
};
```

---

## 🗄️ Database Optimization

### 1. Query Optimization

#### **Efficient Pagination**
```java
// ❌ BEFORE: Inefficient offset-based pagination
@Query("SELECT s FROM ChatSession s WHERE s.userId = :userId ORDER BY s.updatedAt DESC")
Page<ChatSession> findByUserId(@Param("userId") Long userId, Pageable pageable);

// For page 1000, MySQL still scans first 1000*20 = 20,000 records!

// ✅ AFTER: Cursor-based pagination
@Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND " +
       "(:cursor IS NULL OR s.updatedAt < :cursor) " +
       "ORDER BY s.updatedAt DESC LIMIT :limit")
List<ChatSession> findByUserIdWithCursor(
    @Param("userId") Long userId,
    @Param("cursor") LocalDateTime cursor,
    @Param("limit") int limit
);
```

#### **Index Optimization**
```sql
-- ❌ BEFORE: Single column indexes
CREATE INDEX idx_user_id ON chat_sessions(user_id);
CREATE INDEX idx_updated_at ON chat_sessions(updated_at);

-- ✅ AFTER: Composite indexes for better performance
CREATE INDEX idx_sessions_user_updated ON chat_sessions(user_id, updated_at DESC);
CREATE INDEX idx_messages_session_created ON chat_messages(session_id, created_at DESC);
CREATE INDEX idx_messages_role_created ON chat_messages(role, created_at DESC);

-- Covering index for common queries
CREATE INDEX idx_sessions_covering ON chat_sessions(user_id, updated_at DESC) 
  INCLUDE (id, title, model_used, created_at);
```

#### **Query Analysis**
```sql
-- Use EXPLAIN to analyze query performance
EXPLAIN ANALYZE SELECT s.*, COUNT(m.id) as message_count 
FROM chat_sessions s 
LEFT JOIN chat_messages m ON s.id = m.session_id 
WHERE s.user_id = 1 
GROUP BY s.id 
ORDER BY s.updated_at DESC 
LIMIT 20;

-- Optimize based on results:
-- 1. Add covering indexes
-- 2. Use subqueries for complex aggregations
-- 3. Consider denormalization for read-heavy queries
```

### 2. Connection Optimization

#### **Connection Pool Tuning**
```yaml
# application-prod.yml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      # Pool size based on CPU cores and load
      maximum-pool-size: ${DB_POOL_SIZE:20}
      minimum-idle: ${DB_MIN_IDLE:5}
      
      # Connection lifecycle
      max-lifetime: 1800000    # 30 minutes
      idle-timeout: 600000     # 10 minutes
      connection-timeout: 30000 # 30 seconds
      
      # Validation
      validation-timeout: 5000
      leak-detection-threshold: 60000
      
      # Performance tuning
      cache-prep-stmts: true
      prep-stmt-cache-size: 250
      prep-stmt-cache-sql-limit: 2048
      use-server-prep-stmts: true
      use-local-session-state: true
      rewrite-batched-statements: true
      cache-result-set-metadata: true
      cache-server-configuration: true
      elide-set-auto-commits: true
      maintain-time-stats: false
```

---

## 🎯 API Response Optimization

### 1. Response Compression

```java
// Enable compression for API responses
@Configuration
public class CompressionConfig {
    
    @Bean
    public CompressingFilter compressingFilter() {
        CompressingFilter filter = new CompressingFilter();
        filter.setMinGzipSize(1024); // Only compress responses > 1KB
        filter.setCompressionThreshold(1024);
        return filter;
    }
    
    @Bean
    public FilterRegistrationBean<CompressingFilter> compressionFilterRegistration() {
        FilterRegistrationBean<CompressingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(compressingFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("compressionFilter");
        registration.setOrder(1);
        return registration;
    }
}
```

### 2. Response Caching

```java
@RestController
@RequestMapping("/api/chat")
public class OptimizedChatController {
    
    // ✅ Cache static data
    @GetMapping("/models")
    @Cacheable(value = "models", unless = "#result.isEmpty()")
    public ResponseEntity<ModelsResponse> getAvailableModels() {
        // Implementation
    }
    
    // ✅ Conditional requests for session data
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getUserSessions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "If-None-Match", required = false) String etag) {
        
        List<ChatSession> sessions = chatHistoryService.getUserSessions(currentUser.getId());
        String currentEtag = generateEtag(sessions);
        
        if (etag != null && etag.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        
        // Build response
        List<ChatSessionResponse> response = buildSessionResponse(sessions);
        
        return ResponseEntity.ok()
            .eTag(currentEtag)
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
            .body(response);
    }
    
    private String generateEtag(List<ChatSession> sessions) {
        String content = sessions.stream()
            .map(s -> s.getId() + ":" + s.getUpdatedAt().toString())
            .collect(Collectors.joining(","));
        return "\"" + DigestUtils.md5Hex(content) + "\"";
    }
}
```

### 3. Streaming Optimization

```java
@Service
public class StreamingOptimizationService {
    
    // ✅ Batch processing for better throughput
    public Flux<String> processStreamWithBatching(ChatRequest request) {
        return chatService.processChatStream(request)
            .buffer(Duration.ofMillis(50)) // Batch chunks every 50ms
            .filter(batch -> !batch.isEmpty())
            .map(batch -> String.join("", batch))
            .doOnNext(batchedContent -> {
                // Send batched content instead of individual chunks
                if (batchedContent.length() > 10) {
                    sendBatchedResponse(batchedContent);
                }
            });
    }
    
    // ✅ Connection pooling for external APIs
    @Bean
    public WebClient optimizedWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("groq-api")
            .maxConnections(100)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(10))
            .evictInBackground(Duration.ofSeconds(120))
            .build();
            
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create(provider)
                    .responseTimeout(Duration.ofMinutes(2))
                    .keepAlive(true)
                    .compress(true)
            ))
            .build();
    }
}
```

---

## 📊 Caching Strategies

### 1. Redis Implementation

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Different TTL for different caches
        cacheConfigurations.put("user-sessions", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("models", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("user-stats", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

@Service
public class CachedChatService {
    
    @Cacheable(value = "user-sessions", key = "#userId")
    public List<ChatSessionResponse> getUserSessions(Long userId) {
        return chatHistoryService.getUserSessions(userId).stream()
            .map(this::toResponse)
            .collect(toList());
    }
    
    @CacheEvict(value = "user-sessions", key = "#userId")
    public void invalidateUserSessions(Long userId) {
        // Cache will be refreshed on next request
    }
    
    @Caching(evict = {
        @CacheEvict(value = "user-sessions", key = "#sessionResponse.userId"),
        @CacheEvict(value = "user-stats", key = "#sessionResponse.userId")
    })
    public ChatSessionResponse createSession(CreateSessionRequest request) {
        // Create session and evict related caches
    }
}
```

### 2. Frontend Caching

```typescript
// Service Worker for API caching
// sw.js
const CACHE_NAME = 'ai-chat-v1';
const urlsToCache = [
  '/api/chat/models',
  '/api/public/health',
  '/static/js/main.*.js',
  '/static/css/main.*.css'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.url.includes('/api/chat/models')) {
    event.respondWith(
      caches.match(event.request)
        .then((response) => {
          // Return cached version or fetch from network
          if (response) {
            // Check if cache is still valid (5 minutes)
            const cacheDate = new Date(response.headers.get('date'));
            const now = new Date();
            if (now - cacheDate < 5 * 60 * 1000) {
              return response;
            }
          }
          
          return fetch(event.request).then((response) => {
            if (response.ok) {
              const responseToCache = response.clone();
              caches.open(CACHE_NAME)
                .then((cache) => cache.put(event.request, responseToCache));
            }
            return response;
          });
        })
    );
  }
});

// React Query for intelligent caching
import { QueryClient, QueryClientProvider, useQuery } from 'react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Don't retry on 4xx errors
        if (error.status >= 400 && error.status < 500) {
          return false;
        }
        return failureCount < 3;
      },
    },
  },
});

// Usage in components
const useModels = () => {
  return useQuery('models', fetchModels, {
    staleTime: 30 * 60 * 1000, // Models don't change often
    cacheTime: 60 * 60 * 1000,
  });
};

const useSessions = (userId: string) => {
  return useQuery(['sessions', userId], () => fetchSessions(userId), {
    staleTime: 2 * 60 * 1000, // Sessions change more frequently
    cacheTime: 5 * 60 * 1000,
  });
};
```

---

## 🚨 Error Handling & Logging

### 1. Structured Logging

```java
// logback-spring.xml
<configuration>
    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <mdc/>
                    <message/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
        
        <logger name="com.chatai" level="INFO"/>
        <logger name="org.springframework.security" level="WARN"/>
        <logger name="org.hibernate.SQL" level="DEBUG"/>
        
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>
</configuration>

@Component
public class RequestLoggingFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestId = UUID.randomUUID().toString();
        String userId = extractUserId(httpRequest);
        
        // Add to MDC for structured logging
        MDC.put("requestId", requestId);
        MDC.put("userId", userId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri", httpRequest.getRequestURI());
        MDC.put("userAgent", httpRequest.getHeader("User-Agent"));
        
        long startTime = System.currentTimeMillis();
        
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("duration", String.valueOf(duration));
            MDC.put("status", String.valueOf(httpResponse.getStatus()));
            
            log.info("Request processed");
            
            MDC.clear();
        }
    }
}
```

### 2. Error Response Standardization

```java
@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
    private String requestId;
}

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Input validation failed")
            .details(e.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .requestId(MDC.get("requestId"))
            .build();
            
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
            .code("RESOURCE_NOT_FOUND")
            .message("Requested resource was not found")
            .details(e.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .requestId(MDC.get("requestId"))
            .build();
            
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .details("Please contact support with request ID: " + requestId)
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .requestId(requestId)
            .build();
            
        log.error("Unexpected error in request {}: ", requestId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### 3. Frontend Error Handling

```typescript
// Global error handler
class ErrorHandler {
  static handleApiError(error: any): string {
    if (error.response) {
      // Server responded with error status
      const { status, data } = error.response;
      
      switch (status) {
        case 400:
          return data.message || 'Invalid request. Please check your input.';
        case 401:
          this.handleAuthError();
          return 'Please log in to continue.';
        case 403:
          return 'You don\'t have permission to perform this action.';
        case 404:
          return 'The requested resource was not found.';
        case 429:
          return 'Too many requests. Please try again later.';
        case 500:
          this.reportError(error);
          return 'Server error. Our team has been notified.';
        default:
          this.reportError(error);
          return 'An unexpected error occurred.';
      }
    } else if (error.request) {
      // Network error
      return 'Connection error. Please check your internet connection.';
    } else {
      // Client-side error
      this.reportError(error);
      return 'An error occurred while processing your request.';
    }
  }
  
  private static handleAuthError() {
    // Clear auth tokens
    localStorage.removeItem('authToken');
    sessionStorage.removeItem('authToken');
    
    // Redirect to login
    window.location.href = '/login';
  }
  
  private static reportError(error: any) {
    // Send to error reporting service (e.g., Sentry)
    console.error('Reported error:', error);
    
    // In production, send to monitoring service
    if (process.env.NODE_ENV === 'production') {
      fetch('/api/errors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: error.message,
          stack: error.stack,
          url: window.location.href,
          timestamp: new Date().toISOString(),
          userAgent: navigator.userAgent,
        }),
      }).catch(() => {
        // Ignore errors in error reporting
      });
    }
  }
}

// Error Boundary Component
interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  ErrorBoundaryState
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  
  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }
  
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });
    ErrorHandler.reportError({ error, errorInfo });
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <div className="error-fallback">
          <h2>Something went wrong</h2>
          <p>We've been notified about this error and are working to fix it.</p>
          <button onClick={() => window.location.reload()}>
            Refresh Page
          </button>
          {process.env.NODE_ENV === 'development' && (
            <details style={{ marginTop: 20 }}>
              <summary>Error Details (Development)</summary>
              <pre>{this.state.error?.stack}</pre>
              <pre>{this.state.errorInfo?.componentStack}</pre>
            </details>
          )}
        </div>
      );
    }
    
    return this.props.children;
  }
}
```

---

## 📈 Monitoring & Alerting

### 1. Application Metrics

```java
@Component
public class ApplicationMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter chatRequestsTotal;
    private final Timer chatResponseTime;
    private final Gauge activeConnections;
    private final Counter errorCounter;
    
    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.chatRequestsTotal = Counter.builder("chat.requests.total")
            .description("Total number of chat requests")
            .register(meterRegistry);
            
        this.chatResponseTime = Timer.builder("chat.response.duration")
            .description("Chat response time")
            .register(meterRegistry);
            
        this.activeConnections = Gauge.builder("chat.connections.active")
            .description("Active chat connections")
            .register(meterRegistry, this, ApplicationMetrics::getActiveConnectionsCount);
            
        this.errorCounter = Counter.builder("chat.errors.total")
            .description("Total number of errors")
            .register(meterRegistry);
    }
    
    public void recordChatRequest(String model, String userId) {
        chatRequestsTotal.increment(
            Tags.of("model", model, "user_type", getUserType(userId))
        );
    }
    
    public Timer.Sample startResponseTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordError(String errorType, String endpoint) {
        errorCounter.increment(
            Tags.of("error_type", errorType, "endpoint", endpoint)
        );
    }
    
    private double getActiveConnectionsCount() {
        // Implementation to count active WebSocket connections
        return webSocketSessionManager.getActiveConnectionsCount();
    }
}

@RestController
public class MetricsController {
    
    @GetMapping("/actuator/health/custom")
    public ResponseEntity<Map<String, Object>> customHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        // Database connectivity
        boolean dbHealthy = checkDatabaseHealth();
        health.put("database", dbHealthy ? "UP" : "DOWN");
        
        // External API connectivity
        boolean groqHealthy = checkGroqApiHealth();
        health.put("groq_api", groqHealthy ? "UP" : "DOWN");
        
        // Memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        health.put("memory_usage_percent", memoryUsagePercent);
        
        // Overall status
        boolean isHealthy = dbHealthy && groqHealthy && memoryUsagePercent < 90;
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        return ResponseEntity.ok(health);
    }
}
```

### 2. Alerting Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'ai-chat-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 10s

# alerting rules
groups:
  - name: ai-chat.rules
    rules:
      - alert: HighErrorRate
        expr: rate(chat_errors_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"
      
      - alert: HighResponseTime
        expr: rate(chat_response_duration_sum[5m]) / rate(chat_response_duration_count[5m]) > 2
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High response time detected"
          description: "Average response time is {{ $value }}s"
      
      - alert: DatabaseDown
        expr: up{job="ai-chat-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database connection lost"
          description: "Cannot connect to database"
```

---

## 🚀 Deployment Optimizations

### 1. Docker Optimization

```dockerfile
# Multi-stage build for backend
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM openjdk:21-jre-slim
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy JAR file
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app
USER appuser

# JVM optimization for containers
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Frontend optimization
FROM node:18-alpine as frontend-builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force
COPY . .
RUN npm run build

FROM nginx:alpine
# Copy optimized nginx config
COPY --from=frontend-builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

# Security headers
RUN echo 'add_header X-Frame-Options "SAMEORIGIN" always;' > /etc/nginx/conf.d/security.conf && \
    echo 'add_header X-Content-Type-Options "nosniff" always;' >> /etc/nginx/conf.d/security.conf && \
    echo 'add_header X-XSS-Protection "1; mode=block" always;' >> /etc/nginx/conf.d/security.conf

EXPOSE 80
```

### 2. Kubernetes Configuration

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-chat-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: ai-chat-backend
  template:
    metadata:
      labels:
        app: ai-chat-backend
    spec:
      containers:
      - name: backend
        image: ai-chat-backend:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          value: "mysql-service"
        - name: GROQ_API_KEY
          valueFrom:
            secretKeyRef:
              name: api-secrets
              key: groq-api-key
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: ai-chat-backend-service
spec:
  selector:
    app: ai-chat-backend
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-chat-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-chat-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 3. CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy AI Chat Application

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Run backend tests
      run: |
        cd backend
        ./mvnw test
    
    - name: Run frontend tests
      run: |
        cd frontend
        npm ci
        npm run test -- --coverage --watchAll=false
    
    - name: SonarQube Scan
      uses: sonarqube-quality-gate-action@master
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Build and push Docker images
      run: |
        docker build -t ${{ secrets.REGISTRY }}/ai-chat-backend:${{ github.sha }} ./backend
        docker build -t ${{ secrets.REGISTRY }}/ai-chat-frontend:${{ github.sha }} ./frontend
        
        docker push ${{ secrets.REGISTRY }}/ai-chat-backend:${{ github.sha }}
        docker push ${{ secrets.REGISTRY }}/ai-chat-frontend:${{ github.sha }}
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/ai-chat-backend \
          backend=${{ secrets.REGISTRY }}/ai-chat-backend:${{ github.sha }}
        kubectl set image deployment/ai-chat-frontend \
          frontend=${{ secrets.REGISTRY }}/ai-chat-frontend:${{ github.sha }}
        
        kubectl rollout status deployment/ai-chat-backend
        kubectl rollout status deployment/ai-chat-frontend
```

---

## 📋 Performance Checklist

### Backend Performance
- [ ] Database queries optimized with proper indexes
- [ ] Connection pool properly configured
- [ ] JVM tuned for production workload
- [ ] Caching implemented for frequent queries
- [ ] API responses compressed
- [ ] Streaming optimized with backpressure handling
- [ ] Error handling with proper logging
- [ ] Metrics and monitoring configured

### Frontend Performance
- [ ] Components memoized where appropriate
- [ ] Bundle size optimized with code splitting
- [ ] Images lazy loaded and optimized
- [ ] Service Worker implemented for caching
- [ ] Error boundaries in place
- [ ] Memory leaks prevented with proper cleanup
- [ ] Virtual scrolling for large lists
- [ ] React Query for intelligent caching

### Infrastructure
- [ ] Docker images optimized with multi-stage builds
- [ ] Kubernetes resources properly configured
- [ ] HPA configured for auto-scaling
- [ ] Health checks and probes configured
- [ ] Security headers implemented
- [ ] SSL/TLS properly configured
- [ ] Monitoring and alerting set up
- [ ] CI/CD pipeline optimized

### Security
- [ ] Input validation on both client and server
- [ ] XSS protection implemented
- [ ] SQL injection prevention verified
- [ ] Authentication and authorization working
- [ ] Rate limiting configured
- [ ] Security headers configured
- [ ] Secrets management implemented
- [ ] Audit logging enabled

---

Hướng dẫn này cung cấp các tối ưu hóa toàn diện cho AI Chat Spring Application. Áp dụng theo từng bước và theo dõi metrics để đánh giá hiệu quả của các cải thiện.

**Made with ❤️ by Zettix Team**
