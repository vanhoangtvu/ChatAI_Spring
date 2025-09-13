# 📋 Tài liệu Kỹ thuật Chi tiết - AI Chat Spring Application

## 📖 Mục lục

1. [Tổng quan Kiến trúc](#tổng-quan-kiến-trúc)
2. [Backend Architecture](#backend-architecture)
3. [Frontend Architecture](#frontend-architecture)
4. [API Documentation](#api-documentation)
5. [Database Design](#database-design)
6. [Security Implementation](#security-implementation)
7. [Performance Optimizations](#performance-optimizations)
8. [Deployment Guide](#deployment-guide)
9. [Troubleshooting Guide](#troubleshooting-guide)
10. [Best Practices](#best-practices)

---

## 🏗️ Tổng quan Kiến trúc

### Kiến trúc Tổng thể

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT SIDE                              │
├─────────────────────────────────────────────────────────────┤
│  React Frontend (TypeScript)                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │   Auth      │ │    Chat     │ │   Admin     │          │
│  │ Components  │ │ Components  │ │ Components  │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │           Context Providers & Hooks                    │ │
│  │  AuthContext │ ChatContext │ ThemeContext              │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                         HTTP/WebSocket
                              │
┌─────────────────────────────────────────────────────────────┐
│                    SERVER SIDE                              │
├─────────────────────────────────────────────────────────────┤
│  Spring Boot Backend                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │    Auth     │ │    Chat     │ │   Admin     │          │
│  │ Controller  │ │ Controller  │ │ Controller  │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                  Services Layer                         │ │
│  │  AuthService │ ChatService │ ModelService │ ...        │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                Repository Layer                         │ │
│  │  UserRepo │ ChatRepo │ SessionRepo │ ...               │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              │
┌─────────────────────────────────────────────────────────────┐
│                 EXTERNAL SERVICES                           │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │   MySQL     │ │   Groq API  │ │   Redis     │          │
│  │  Database   │ │ (AI Models) │ │   (Cache)   │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Security**: JWT Authentication, Spring Security
- **Database**: MySQL 8.0 với JPA/Hibernate
- **API Integration**: Groq API cho AI models
- **Build Tool**: Maven

#### Frontend
- **Framework**: React 18.x với TypeScript
- **UI Library**: Material-UI (MUI) v5
- **State Management**: Context API + useReducer
- **HTTP Client**: Axios
- **Build Tool**: Create React App với TypeScript template

---

## 🛠️ Backend Architecture

### Cấu trúc thư mục Backend

```
backend/src/main/java/com/chatai/
├── ChatApplication.java                 # Main Spring Boot application
├── config/
│   ├── CorsConfig.java                  # CORS configuration
│   ├── SecurityConfig.java              # Spring Security setup
│   ├── SwaggerConfig.java               # API documentation
│   └── GroqApiConfig.java              # Groq API configuration
├── controller/
│   ├── AuthController.java              # Authentication endpoints
│   ├── ChatController.java              # Chat & streaming endpoints
│   ├── AdminController.java             # Admin management
│   └── HealthController.java            # Health checks
├── dto/                                 # Data Transfer Objects
│   ├── auth/
│   │   ├── LoginRequest.java
│   │   ├── SignupRequest.java
│   │   └── JwtResponse.java
│   ├── chat/
│   │   ├── ChatRequest.java
│   │   ├── ChatResponse.java
│   │   └── ChatSessionResponse.java
│   └── admin/
│       └── UserManagementDto.java
├── entity/                              # JPA Entities
│   ├── User.java
│   ├── Role.java
│   ├── ChatSession.java
│   ├── ChatMessage.java
│   └── ModelManagement.java
├── repository/                          # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── ChatSessionRepository.java
│   ├── ChatMessageRepository.java
│   └── ModelManagementRepository.java
├── service/                            # Business logic layer
│   ├── AuthService.java
│   ├── ChatService.java
│   ├── ChatHistoryService.java
│   ├── UserService.java
│   ├── RequestLimitService.java
│   └── ModelManagementService.java
├── security/                           # Security components
│   ├── JwtUtils.java
│   ├── UserPrincipal.java
│   └── JwtAuthenticationFilter.java
└── exception/                          # Custom exceptions
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── BadRequestException.java
```

### Core Backend Components

#### 1. Authentication & Security

**JWT Implementation:**
```java
@Component
public class JwtUtils {
    private static final String JWT_SECRET = "mySecretKey";
    private static final int JWT_EXPIRATION = 86400000; // 24 hours
    
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }
}
```

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

#### 2. Chat Service Architecture

**Streaming Chat Implementation:**
```java
@Service
public class ChatService {
    public Flux<String> processChatStreamWithHistory(ChatRequest request, Long sessionId, Long userId) {
        return webClient
            .post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer " + groqApiKey)
            .bodyValue(buildGroqRequest(request))
            .retrieve()
            .bodyToFlux(String.class)
            .map(this::processStreamChunk)
            .doOnComplete(() -> saveAssistantMessage(sessionId, fullResponse));
    }
}
```

**Database Entities:**

```java
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "title")
    private String title = "New Chat";
    
    @Column(name = "model_used")
    private String modelUsed;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
    
    // timestamps, getters, setters...
}
```

---

## ⚛️ Frontend Architecture

### Cấu trúc thư mục Frontend

```
frontend/src/
├── components/                          # React components
│   ├── Auth/
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   └── AuthGuard.tsx
│   ├── Chat/
│   │   ├── ChatInterface.tsx            # Main chat component
│   │   ├── MessageList.tsx              # Message rendering
│   │   ├── MessageContent.tsx           # Content formatting
│   │   ├── MessageInput.tsx             # Input handling
│   │   ├── ModelSelector.tsx            # AI model selection
│   │   ├── EnhancedCodeBlock.tsx        # Code syntax highlighting
│   │   ├── MarkdownRenderer.tsx         # Markdown processing
│   │   ├── ImprovedMarkdownRenderer.tsx # Enhanced renderer
│   │   ├── SessionSidebar.tsx           # Chat history
│   │   └── ThinkingDisplay.tsx          # Loading states
│   ├── Admin/
│   │   ├── AdminDashboard.tsx
│   │   ├── UserManagement.tsx
│   │   └── SystemStats.tsx
│   └── Layout/
│       ├── Header.tsx
│       ├── Sidebar.tsx
│       └── Layout.tsx
├── context/                            # React Context providers
│   ├── AuthContext.tsx                 # Authentication state
│   ├── ChatContext.tsx                 # Chat state management
│   └── ThemeContext.tsx                # UI theme management
├── hooks/                              # Custom React hooks
│   ├── useAuth.ts                      # Authentication logic
│   ├── useChat.ts                      # Chat functionality
│   └── useModelSelection.ts            # Model management
├── services/                           # API integration
│   ├── auth.service.ts                 # Authentication API
│   ├── chat.service.ts                 # Chat API
│   ├── admin.service.ts                # Admin API
│   └── api.ts                          # Base API configuration
├── types/                              # TypeScript definitions
│   ├── auth.types.ts
│   ├── chat.types.ts
│   └── admin.types.ts
├── utils/                              # Utility functions
│   ├── constants.ts
│   └── helpers.ts
├── App.tsx                             # Main app component
└── index.tsx                           # Application entry point
```

### Key Frontend Components

#### 1. Enhanced Message Rendering

**Improved Markdown Renderer Features:**
- ✅ **Better Performance**: Memoized parsing với regex optimization
- ✅ **Accurate Formatting**: Fixed conflicts between code blocks và inline code
- ✅ **Enhanced Styling**: Modern UI với custom scrollbars
- ✅ **Table Support**: Full markdown table rendering
- ✅ **Nested Lists**: Proper handling of multi-level lists
- ✅ **Security**: HTML sanitization để prevent XSS

```typescript
// Enhanced parsing logic in ImprovedMarkdownRenderer.tsx
const parseMarkdown = useCallback((text: string): string => {
  // Escape HTML entities first
  html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;');
  
  // Process headers (h6 to h1 to avoid conflicts)
  html = html.replace(/^#{6}\s+(.+)$/gm, '<h6>$1</h6>');
  
  // Improved list handling with nesting support
  const processedLines = lines.map((line) => {
    const unorderedMatch = trimmedLine.match(/^(\s*)[-*+]\s+(.+)$/);
    if (unorderedMatch) {
      const level = Math.floor(unorderedMatch[1].length / 2);
      return handleListItem('ul', level, unorderedMatch[2]);
    }
    // ... more processing
  });
  
  return html;
}, []);
```

#### 2. Advanced Code Block Component

**EnhancedCodeBlock Features:**
- ✅ **Syntax Highlighting**: Using Prism.js với 100+ languages
- ✅ **Copy Functionality**: One-click code copying
- ✅ **Download Support**: Save code as files
- ✅ **Language Detection**: Smart language recognition
- ✅ **Performance**: Virtualization cho large code blocks
- ✅ **Responsive**: Mobile-friendly design

#### 3. Real-time Chat Streaming

**Streaming Implementation:**
```typescript
const useChat = () => {
  const [isStreaming, setIsStreaming] = useState(false);
  const [currentMessage, setCurrentMessage] = useState('');
  
  const streamChat = async (message: string, model: string) => {
    setIsStreaming(true);
    
    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ message, model }),
      });
      
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        const chunk = decoder.decode(value);
        setCurrentMessage(prev => prev + chunk);
      }
    } finally {
      setIsStreaming(false);
    }
  };
  
  return { streamChat, isStreaming, currentMessage };
};
```

---

## 📡 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/api/auth/signin` | User login | `{username, password}` | `{token, user, roles}` |
| POST | `/api/auth/signup` | User registration | `{username, email, password}` | `{message}` |
| POST | `/api/auth/signout` | User logout | - | `{message}` |

### Chat Endpoints

| Method | Endpoint | Description | Headers | Request Body |
|--------|----------|-------------|---------|--------------|
| POST | `/api/chat/stream` | Streaming chat | `Authorization: Bearer <token>` | `{message, model, sessionId?}` |
| GET | `/api/chat/sessions` | Get user sessions | `Authorization: Bearer <token>` | - |
| GET | `/api/chat/sessions/{id}` | Get session details | `Authorization: Bearer <token>` | - |
| DELETE | `/api/chat/sessions/{id}` | Delete session | `Authorization: Bearer <token>` | - |
| PUT | `/api/chat/sessions/{id}/title` | Update session title | `Authorization: Bearer <token>` | `{title}` |
| GET | `/api/chat/models` | Get available models | - | - |
| GET | `/api/chat/usage` | Get user usage stats | `Authorization: Bearer <token>` | - |

### Admin Endpoints

| Method | Endpoint | Description | Required Role | Request Body |
|--------|----------|-------------|---------------|--------------|
| GET | `/api/admin/users` | Get all users | ADMIN | - |
| GET | `/api/admin/users/search` | Search users | ADMIN | `?query=<term>` |
| PUT | `/api/admin/users/{id}` | Update user | ADMIN | `{username, email, roles}` |
| DELETE | `/api/admin/users/{id}` | Delete user | ADMIN | - |
| POST | `/api/admin/users/{id}/reset-limit` | Reset user limit | ADMIN | - |
| GET | `/api/admin/stats` | Get system statistics | ADMIN | - |

### Response Formats

#### Success Response
```json
{
  "success": true,
  "data": {
    // response data
  },
  "message": "Operation successful"
}
```

#### Error Response
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": "Additional details"
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

---

## 🗄️ Database Design

### Entity Relationship Diagram

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│      users      │────▶│   user_roles    │◀────│      roles      │
│                 │     │                 │     │                 │
│ id (PK)         │     │ user_id (FK)    │     │ id (PK)         │
│ username        │     │ role_id (FK)    │     │ name            │
│ email           │     └─────────────────┘     │ ROLE_USER       │
│ password        │                             │ ROLE_ADMIN      │
│ created_at      │                             └─────────────────┘
│ updated_at      │
└─────────────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│  chat_sessions  │────▶│  chat_messages  │
│                 │     │                 │
│ id (PK)         │     │ id (PK)         │
│ user_id (FK)    │     │ session_id (FK) │
│ title           │     │ content (TEXT)  │
│ model_used      │     │ thinking (TEXT) │
│ created_at      │     │ role (ENUM)     │
│ updated_at      │     │ model_used      │
└─────────────────┘     │ tokens_used     │
                        │ created_at      │
                        └─────────────────┘
```

### Database Schema Details

#### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email)
);
```

#### Roles Table
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name ENUM('ROLE_USER', 'ROLE_ADMIN') NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');
```

#### Chat Sessions Table
```sql
CREATE TABLE chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) DEFAULT 'New Chat',
    model_used VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);
```

#### Chat Messages Table
```sql
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    thinking TEXT,
    role ENUM('USER', 'ASSISTANT') NOT NULL,
    model_used VARCHAR(100),
    tokens_used INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_role (role)
);
```

#### Model Management Table
```sql
CREATE TABLE model_management (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL,
    model_id VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50),
    is_enabled BOOLEAN DEFAULT TRUE,
    max_tokens INTEGER,
    cost_per_token DECIMAL(10,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_model_id (model_id),
    INDEX idx_is_enabled (is_enabled)
);
```

### Database Optimization Strategies

#### 1. Indexing Strategy
- **Primary Keys**: Auto-increment BIGINT for scalability
- **Foreign Keys**: Indexed for join performance
- **Search Columns**: Username, email indexed for fast lookups
- **Time-based Queries**: created_at indexed for chronological queries

#### 2. Query Optimization
```java
// Efficient pagination for chat history
@Query("SELECT s FROM ChatSession s WHERE s.userId = :userId ORDER BY s.updatedAt DESC")
Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId, Pageable pageable);

// Batch message count query
@Query("SELECT s.id, COUNT(m) FROM ChatSession s LEFT JOIN s.messages m WHERE s.userId = :userId GROUP BY s.id")
List<Object[]> findSessionMessageCounts(@Param("userId") Long userId);
```

---

## 🔒 Security Implementation

### 1. Authentication & Authorization

#### JWT Implementation
- **Secret Key Management**: Environment variables
- **Token Expiration**: 24 hours default
- **Refresh Token**: Automatic renewal mechanism
- **Role-based Access**: ADMIN/USER roles

#### Password Security
```java
@Service
public class UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User createUser(SignupRequest request) {
        User user = new User();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // BCrypt with salt rounds = 12
        return userRepository.save(user);
    }
}
```

### 2. API Security

#### CORS Configuration
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "https://yourdomain.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        return source;
    }
}
```

#### Request Validation
```java
public class ChatRequest {
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 4000, message = "Message too long")
    private String message;
    
    @NotBlank(message = "Model must be specified")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_/]+$", message = "Invalid model format")
    private String model;
}
```

### 3. Data Protection

#### SQL Injection Prevention
- **JPA/Hibernate**: Parameterized queries
- **Query Validation**: Input sanitization
- **Prepared Statements**: All dynamic queries

#### XSS Protection
```typescript
// Frontend HTML sanitization
const sanitizeHtml = (html: string): string => {
  return html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};
```

---

## ⚡ Performance Optimizations

### 1. Backend Performance

#### Database Optimizations
```java
// Efficient lazy loading
@Entity
public class ChatSession {
    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    private List<ChatMessage> messages;
    
    // Batch fetch optimization
    @BatchSize(size = 50)
    private Set<ChatMessage> batchMessages;
}

// Query optimization with projections
@Query("SELECT new com.chatai.dto.SessionSummary(s.id, s.title, COUNT(m)) " +
       "FROM ChatSession s LEFT JOIN s.messages m " +
       "WHERE s.userId = :userId GROUP BY s.id")
List<SessionSummary> findSessionSummaries(@Param("userId") Long userId);
```

#### Caching Strategy
```java
@Service
@Cacheable("user-sessions")
public class ChatHistoryService {
    @Cacheable(value = "sessions", key = "#userId")
    public List<ChatSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    @CacheEvict(value = "sessions", key = "#userId")
    public void clearUserSessionCache(Long userId) {
        // Cache invalidation on updates
    }
}
```

### 2. Frontend Performance

#### Component Optimization
```typescript
// Memoized components for expensive renders
const MessageList = React.memo(({ messages, isUser }) => {
  const renderedMessages = useMemo(() => 
    messages.map(msg => renderMessage(msg)), 
    [messages]
  );
  
  return <VirtualizedList items={renderedMessages} />;
});

// Debounced input for search
const useDebounce = (value: string, delay: number) => {
  const [debouncedValue, setDebouncedValue] = useState(value);
  
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  
  return debouncedValue;
};
```

#### Bundle Optimization
```javascript
// Code splitting for better loading
const AdminDashboard = lazy(() => import('./components/Admin/AdminDashboard'));
const ChatInterface = lazy(() => import('./components/Chat/ChatInterface'));

// Dynamic imports
const loadComponent = async (componentName) => {
  const module = await import(`./components/${componentName}`);
  return module.default;
};
```

### 3. Streaming Performance

#### Efficient Streaming
```java
@Service
public class ChatService {
    public Flux<String> processChatStream(ChatRequest request) {
        return webClient
            .post()
            .uri("/v1/chat/completions")
            .bodyValue(buildRequest(request))
            .retrieve()
            .bodyToFlux(String.class)
            .publishOn(Schedulers.boundedElastic()) // Non-blocking processing
            .map(this::processChunk)
            .onErrorResume(this::handleStreamError);
    }
    
    private String processChunk(String chunk) {
        // Efficient chunk processing
        if (chunk.startsWith("data: ")) {
            return chunk.substring(6);
        }
        return chunk;
    }
}
```

---

## 🚀 Deployment Guide

### 1. Production Build

#### Backend Build
```bash
# Clean and build
cd backend
mvn clean package -DskipTests

# Create production JAR
mvn spring-boot:build-image

# Environment variables setup
export GROQ_API_KEY=your_api_key
export DB_PASSWORD=production_password
export JWT_SECRET=production_jwt_secret
```

#### Frontend Build
```bash
# Production build
cd frontend
npm run build

# Environment variables
REACT_APP_API_BASE_URL=https://api.yourdomain.com
REACT_APP_WS_BASE_URL=wss://api.yourdomain.com
```

### 2. Docker Deployment

#### Docker Compose Setup
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: db_AIchatbot
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
  
  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      GROQ_API_KEY: ${GROQ_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
      DB_HOST: mysql
      DB_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - mysql
    ports:
      - "8080:8080"
  
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "443:443"
    depends_on:
      - frontend
      - backend

volumes:
  mysql_data:
```

#### Nginx Configuration
```nginx
# nginx.conf
server {
    listen 80;
    listen 443 ssl;
    server_name yourdomain.com;
    
    # SSL configuration
    ssl_certificate /etc/ssl/certs/your-cert.pem;
    ssl_certificate_key /etc/ssl/private/your-key.pem;
    
    # Frontend
    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 3. Cloud Deployment (AWS)

#### Infrastructure Setup
```yaml
# cloudformation.yml
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  # ECS Cluster
  ECSCluster:
    Type: AWS::ECS::Cluster
    Properties:
      ClusterName: ai-chat-cluster
  
  # RDS MySQL Instance
  RDSInstance:
    Type: AWS::RDS::DBInstance
    Properties:
      DBInstanceClass: db.t3.micro
      Engine: mysql
      EngineVersion: '8.0'
      AllocatedStorage: 20
      DBName: db_AIchatbot
      MasterUsername: admin
      MasterUserPassword: !Ref DBPassword
      VPCSecurityGroups:
        - !Ref DBSecurityGroup
  
  # Application Load Balancer
  ApplicationLoadBalancer:
    Type: AWS::ElasticLoadBalancingV2::LoadBalancer
    Properties:
      Type: application
      Scheme: internet-facing
      SecurityGroups:
        - !Ref ALBSecurityGroup
      Subnets:
        - !Ref PublicSubnet1
        - !Ref PublicSubnet2
```

---

## 🔧 Troubleshooting Guide

### 1. Common Backend Issues

#### Database Connection Issues
```bash
# Check MySQL connection
mysql -h localhost -u root -p
SHOW DATABASES;
USE db_AIchatbot;
SHOW TABLES;

# Common fixes:
# 1. Check MySQL service status
sudo systemctl status mysql
sudo systemctl start mysql

# 2. Verify credentials in application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_AIchatbot
    username: root
    password: your_password
```

#### JWT Token Issues
```java
// Debug JWT problems
@Component
public class JwtDebugFilter {
    public void debug(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
            
            System.out.println("Token valid, user: " + claims.getSubject());
            System.out.println("Expires: " + claims.getExpiration());
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("Invalid signature: " + e.getMessage());
        }
    }
}
```

#### Groq API Issues
```java
// Check API connectivity
@Service
public class GroqApiHealthCheck {
    public boolean checkApiHealth() {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.groq.com/openai/v1/models",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Groq API health check failed: {}", e.getMessage());
            return false;
        }
    }
}
```

### 2. Common Frontend Issues

#### CORS Errors
```typescript
// Check CORS configuration
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor for debugging
api.interceptors.request.use(
  (config) => {
    console.log('Request:', config);
    return config;
  },
  (error) => {
    console.error('Request Error:', error);
    return Promise.reject(error);
  }
);
```

#### Performance Issues
```typescript
// Debug rendering performance
const useRenderTracking = (componentName: string) => {
  const renderCount = useRef(0);
  
  useEffect(() => {
    renderCount.current++;
    console.log(`${componentName} rendered ${renderCount.current} times`);
  });
};

// Memory leak detection
const useMemoryLeak = () => {
  useEffect(() => {
    const interval = setInterval(() => {
      console.log('Memory usage:', performance.memory?.usedJSHeapSize);
    }, 5000);
    
    return () => clearInterval(interval);
  }, []);
};
```

### 3. Deployment Issues

#### Docker Build Problems
```bash
# Clear Docker cache
docker system prune -a

# Debug build process
docker build --no-cache -t ai-chat-backend ./backend
docker logs <container-id>

# Check container health
docker exec -it <container-id> /bin/bash
curl http://localhost:8080/api/public/health
```

#### SSL Certificate Issues
```bash
# Generate self-signed certificate for testing
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

# Check certificate validity
openssl x509 -in cert.pem -text -noout

# Nginx SSL configuration test
nginx -t
systemctl reload nginx
```

---

## 📋 Best Practices

### 1. Code Quality

#### Backend Best Practices
```java
// Use DTOs for API responses
@Data
@Builder
public class ChatResponse {
    private String id;
    private String content;
    private LocalDateTime timestamp;
    private String modelUsed;
    
    // Never expose entity directly
    public static ChatResponse fromEntity(ChatMessage message) {
        return ChatResponse.builder()
            .id(message.getId().toString())
            .content(message.getContent())
            .timestamp(message.getCreatedAt())
            .modelUsed(message.getModelUsed())
            .build();
    }
}

// Proper exception handling
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
```

#### Frontend Best Practices
```typescript
// Custom hooks for reusability
const useAsyncOperation = <T>(operation: () => Promise<T>) => {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const execute = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const result = await operation();
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [operation]);
  
  return { data, loading, error, execute };
};

// Error boundaries for robust error handling
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }
  
  static getDerivedStateFromError(error) {
    return { hasError: true };
  }
  
  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
    // Send to error reporting service
  }
  
  render() {
    if (this.state.hasError) {
      return <ErrorFallback />;
    }
    
    return this.props.children;
  }
}
```

### 2. Security Best Practices

#### Input Validation
```java
// Backend validation
@Valid
public class ChatRequest {
    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 4000, message = "Message length must be between 1 and 4000 characters")
    private String message;
    
    @NotBlank(message = "Model is required")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_/.]+$", message = "Invalid model identifier")
    private String model;
    
    @Min(value = 1, message = "Session ID must be positive")
    private Long sessionId;
}
```

```typescript
// Frontend validation
const validateChatInput = (message: string, model: string) => {
  const errors: string[] = [];
  
  if (!message?.trim()) {
    errors.push('Message cannot be empty');
  }
  
  if (message?.length > 4000) {
    errors.push('Message too long (max 4000 characters)');
  }
  
  if (!model || !/^[a-zA-Z0-9\-_/.]+$/.test(model)) {
    errors.push('Invalid model selection');
  }
  
  return errors;
};
```

### 3. Performance Best Practices

#### Database Optimization
```sql
-- Proper indexing strategy
CREATE INDEX idx_sessions_user_updated ON chat_sessions(user_id, updated_at DESC);
CREATE INDEX idx_messages_session_created ON chat_messages(session_id, created_at DESC);

-- Query optimization
EXPLAIN SELECT s.*, COUNT(m.id) as message_count 
FROM chat_sessions s 
LEFT JOIN chat_messages m ON s.id = m.session_id 
WHERE s.user_id = ? 
GROUP BY s.id 
ORDER BY s.updated_at DESC 
LIMIT 20;
```

#### Caching Strategy
```java
// Redis caching configuration
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(jedisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

### 4. Monitoring & Logging

#### Application Monitoring
```java
// Metrics collection
@Component
public class ChatMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter chatRequestCounter;
    private final Timer chatResponseTimer;
    
    public ChatMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.chatRequestCounter = Counter.builder("chat.requests.total")
            .description("Total chat requests")
            .tag("type", "chat")
            .register(meterRegistry);
        
        this.chatResponseTimer = Timer.builder("chat.response.duration")
            .description("Chat response time")
            .register(meterRegistry);
    }
    
    public void recordChatRequest(String model) {
        chatRequestCounter.increment(Tags.of("model", model));
    }
    
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
}
```

#### Structured Logging
```java
// Structured logging with Logback
@Slf4j
@Service
public class ChatService {
    public void processChatRequest(ChatRequest request, String userId) {
        MDC.put("userId", userId);
        MDC.put("model", request.getModel());
        MDC.put("requestId", UUID.randomUUID().toString());
        
        try {
            log.info("Processing chat request: message_length={}", request.getMessage().length());
            // Process request
            log.info("Chat request processed successfully");
        } catch (Exception e) {
            log.error("Chat request failed: error={}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

---

## 📊 Performance Benchmarks

### Response Time Targets
- **API Response**: < 200ms for non-streaming endpoints
- **Streaming Start**: < 500ms time to first byte
- **Database Queries**: < 50ms for simple queries
- **Frontend Rendering**: < 100ms for component updates

### Scalability Metrics
- **Concurrent Users**: 1000+ simultaneous connections
- **Message Throughput**: 100+ messages/second
- **Database Performance**: 1000+ queries/second
- **Memory Usage**: < 512MB per backend instance

---

Tài liệu này cung cấp cái nhìn toàn diện về kiến trúc, implementation và best practices của AI Chat Spring Application. Để cập nhật hoặc đóng góp, vui lòng tạo pull request hoặc liên hệ team development.

**Made with ❤️ by Zettix Team**
