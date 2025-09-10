# 🤖 AI Chat Application - Spring Boot & React

Ứng dụng chat AI toàn diện với backend Spring Boot và frontend React, hỗ trợ nhiều model AI thông qua Groq API.

## 🎯 Tổng quan

Đây là một ứng dụng chat AI hiện đại được xây dựng với kiến trúc microservices, bao gồm:

- **Backend**: Spring Boot với JWT authentication, MySQL database
- **Frontend**: React với TypeScript, Material-UI, real-time streaming
- **AI Integration**: Groq API với 12 models khác nhau
- **Features**: User management, chat history, admin dashboard, request limiting

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│                 │    │                 │    │                 │
│  React Frontend │◄──►│ Spring Backend  │◄──►│   Groq API      │
│  (Port 3000)    │    │  (Port 8080)    │    │   (AI Models)   │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │                 │
                       │  MySQL Database │
                       │  (Port 3306)    │
                       │                 │
                       └─────────────────┘
```

## 🚀 Tính năng chính

### 🔐 Authentication & Security
- ✅ JWT-based authentication
- ✅ Role-based access control (USER, ADMIN)
- ✅ Secure password encryption
- ✅ CORS configuration
- ✅ Request rate limiting

### 💬 Chat Features
- ✅ Real-time streaming chat
- ✅ 12 AI models support
- ✅ Chat history management
- ✅ Session management
- ✅ Message persistence
- ✅ Model switching during chat

### 👨‍💼 Admin Features
- ✅ User management
- ✅ System statistics
- ✅ Request limit management
- ✅ User search and filtering

### 🎨 Frontend Features
- ✅ Modern React with TypeScript
- ✅ Material-UI components
- ✅ Responsive design
- ✅ Dark/Light theme
- ✅ Real-time message streaming
- ✅ Chat history sidebar

## 📋 Danh sách AI Models

### ⚡ Groq Models (12 models)
1. **Llama 3.1 8B Instant** - `llama-3.1-8b-instant`
2. **Llama 3.3 70B Versatile** - `llama-3.3-70b-versatile`
3. **Gemma2 9B** - `gemma2-9b-it`
4. **Llama 4 Maverick 17B** - `meta-llama/llama-4-maverick-17b-128e-instruct`
5. **Llama 4 Scout 17B** - `meta-llama/llama-4-scout-17b-16e-instruct`
6. **Qwen 3 32B** - `qwen/qwen3-32b`
7. **Kimi K2 Instruct** - `moonshotai/kimi-k2-instruct`
8. **Compound Beta** - `compound-beta`
9. **Compound Beta Mini** - `compound-beta-mini`
10. **GPT-OSS 20B** - `openai/gpt-oss-20b`
11. **GPT-OSS 120B** - `openai/gpt-oss-120b`
12. **DeepSeek R1 Distill** - `deepseek-r1-distill-llama-70b`

## 🛠️ Cài đặt và Chạy

### Yêu cầu hệ thống
- **Java**: 21+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Maven**: 3.6+

### 1. Clone Repository
```bash
git clone <repository-url>
cd Chat_SpringAI
```

### 2. Cấu hình Database
```sql
CREATE DATABASE db_AIchatbot;
```

### 3. Cấu hình API Keys
Tạo file `backend/src/main/resources/api_key.yml`:
```yaml
groq:
  api-key: your-groq-api-key-here
```

### 4. Chạy Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
Backend sẽ chạy tại: `http://localhost:8080`

### 5. Chạy Frontend
```bash
cd frontend
npm install
npm start
```
Frontend sẽ chạy tại: `http://localhost:3000`

## 📚 API Documentation

### 🔐 Authentication Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signin` | User login |
| POST | `/api/auth/signup` | User registration |
| POST | `/api/auth/signout` | User logout |

### 💬 Chat Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat/stream` | Streaming chat |
| GET | `/api/chat/sessions` | Get user sessions |
| GET | `/api/chat/sessions/{id}` | Get session details |
| DELETE | `/api/chat/sessions/{id}` | Delete session |
| PUT | `/api/chat/sessions/{id}/title` | Update session title |
| GET | `/api/chat/usage` | Get user usage stats |
| GET | `/api/chat/models` | Get available models |

### 👨‍💼 Admin Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | Get all users |
| GET | `/api/admin/users/search` | Search users |
| PUT | `/api/admin/users/{id}` | Update user |
| DELETE | `/api/admin/users/{id}` | Delete user |
| POST | `/api/admin/users/{id}/reset-limit` | Reset user limit |
| GET | `/api/admin/stats` | Get system stats |

### 🏥 Health Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/public/health` | System health |
| GET | `/api/public/info` | System info |

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Roles Table
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name ENUM('ROLE_USER', 'ROLE_ADMIN') NOT NULL
);
```

### Chat Sessions Table
```sql
CREATE TABLE chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) DEFAULT 'New Chat',
    model_used VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Chat Messages Table
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
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);
```

## 🎨 Frontend Structure

```
frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── components/
│   │   ├── Auth/
│   │   │   ├── Login.tsx
│   │   │   └── Register.tsx
│   │   ├── Chat/
│   │   │   ├── ChatInterface.tsx
│   │   │   ├── MessageList.tsx
│   │   │   ├── MessageInput.tsx
│   │   │   └── ModelSelector.tsx
│   │   ├── Admin/
│   │   │   ├── AdminDashboard.tsx
│   │   │   ├── UserManagement.tsx
│   │   │   └── SystemStats.tsx
│   │   ├── Layout/
│   │   │   ├── Header.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── Layout.tsx
│   │   └── Common/
│   │       ├── Loading.tsx
│   │       └── ErrorBoundary.tsx
│   ├── services/
│   │   ├── auth.service.ts
│   │   ├── chat.service.ts
│   │   ├── admin.service.ts
│   │   └── api.ts
│   ├── types/
│   │   ├── auth.types.ts
│   │   ├── chat.types.ts
│   │   └── admin.types.ts
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useChat.ts
│   │   └── useWebSocket.ts
│   ├── context/
│   │   ├── AuthContext.tsx
│   │   └── ThemeContext.tsx
│   ├── utils/
│   │   ├── constants.ts
│   │   └── helpers.ts
│   ├── App.tsx
│   └── index.tsx
├── package.json
└── tsconfig.json
```

## 🔧 Configuration

### Backend Configuration (`application.yml`)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_AIchatbot
    username: root
    password: 1111
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: your-jwt-secret
  expiration: 86400000

groq:
  api-key: ${GROQ_API_KEY}
  base-url: https://api.groq.com
```

### Frontend Configuration (`.env`)
```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_WS_BASE_URL=ws://localhost:8080
```

## 🧪 Testing

### Backend Testing
```bash
cd backend
mvn test
```

### Frontend Testing
```bash
cd frontend
npm test
```

## 📈 Performance & Monitoring

### Metrics Tracked
- Request count per user
- Response time per model
- Error rates
- Active sessions
- Database performance

### Logging
- Application logs: `/logs/application.log`
- Error logs: `/logs/error.log`
- Access logs: `/logs/access.log`

## 🔒 Security

### Implemented Security Measures
- JWT token authentication
- Password encryption (BCrypt)
- CORS configuration
- Request rate limiting
- SQL injection prevention
- XSS protection

## 🚀 Deployment

### Docker Deployment
```bash
# Build backend
cd backend
mvn clean package
docker build -t ai-chat-backend .

# Build frontend
cd frontend
npm run build
docker build -t ai-chat-frontend .

# Run with docker-compose
docker-compose up -d
```

### Production Deployment
1. Build production artifacts
2. Configure environment variables
3. Setup reverse proxy (Nginx)
4. Configure SSL certificates
5. Setup monitoring and logging

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check MySQL service is running
   - Verify connection string and credentials

2. **API Key Error**
   - Ensure Groq API key is valid
   - Check api_key.yml configuration

3. **CORS Error**
   - Verify frontend URL in CORS configuration
   - Check if backend is running

4. **Authentication Error**
   - Check JWT secret configuration
   - Verify token expiration settings

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Groq API** for AI model access
- **Spring Boot** framework
- **React** ecosystem
- **Material-UI** components
- **MySQL** database

---

**Made with ❤️ by Zettix Team**

For support, email us at: admin@zettixteam.com
# ChatAI_Spring
