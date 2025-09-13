# 🐳 AI Chat Docker Setup

Hướng dẫn chạy toàn bộ dự án AI Chat bằng Docker.

## 📋 Yêu cầu

- Docker Engine 20.10+
- Docker Compose 2.0+
- Ít nhất 4GB RAM
- 10GB disk space

## 🚀 Quick Start

### 1. Chuẩn bị môi trường

```bash
# Copy file cấu hình môi trường
cp .env.example .env

# Chỉnh sửa .env với các giá trị thực tế
nano .env
```

**Quan trọng**: Cần cập nhật `GROQ_API_KEY` trong file `.env` với API key thực tế.

### 2. Build và chạy ứng dụng

```bash
# Cách 1: Sử dụng Makefile (khuyến nghị)
make build
make up

# Cách 2: Sử dụng Docker Compose trực tiếp
docker-compose build
docker-compose up -d
```

### 3. Truy cập ứng dụng

- **Frontend**: http://localhost:3005
- **Backend API**: http://localhost:8081
- **Database**: localhost:3306

## 🛠 Các lệnh quản lý

### Makefile Commands

```bash
make help          # Hiển thị tất cả lệnh có sẵn
make build         # Build tất cả images
make up            # Khởi động tất cả services
make down          # Dừng tất cả services  
make restart       # Restart tất cả services
make logs          # Xem logs của tất cả services
make clean         # Dọn dẹp tất cả (containers, images, volumes)
make health        # Kiểm tra trạng thái services
make stats         # Xem resource usage
make ps            # Xem containers đang chạy
```

### Docker Compose Commands

```bash
# Khởi động
docker-compose up -d

# Dừng
docker-compose down

# Xem logs
docker-compose logs -f [service-name]

# Restart service cụ thể
docker-compose restart backend

# Scale service
docker-compose up -d --scale backend=2

# Exec vào container
docker-compose exec backend bash
```

## 📊 Services

### 🗄️ Database (MySQL 8.0)
- **Container**: `ai-chat-db`
- **Port**: 3306
- **Database**: `db_AIchatbot`
- **Credentials**: root/1111

### 🔧 Backend (Spring Boot)
- **Container**: `ai-chat-backend`
- **Port**: 8081
- **Health Check**: `/actuator/health`

### 🎨 Frontend (React + Nginx)
- **Container**: `ai-chat-frontend`  
- **Port**: 3005
- **Health Check**: `/health`

### 🚀 Redis (Optional)
- **Container**: `ai-chat-redis`
- **Port**: 6379
- **Purpose**: Caching

## 🔍 Monitoring & Debugging

### Health Checks

```bash
# Kiểm tra tất cả services
make health

# Kiểm tra từng service
curl http://localhost:8081/actuator/health  # Backend
curl http://localhost:3005/health           # Frontend
```

### Logs

```bash
# Tất cả logs
make logs

# Logs service cụ thể
make logs-backend
make logs-frontend  
make logs-db

# Logs real-time
docker-compose logs -f backend
```

### Debug Container

```bash
# Vào container backend
docker-compose exec backend bash

# Vào container database
docker-compose exec database mysql -u root -p1111

# Xem container stats
make stats
```

## 🗄️ Database Operations

### Backup Database

```bash
make db-backup
# Hoặc
docker-compose exec database mysqldump -u root -p1111 db_AIchatbot > backup.sql
```

### Restore Database

```bash
make db-restore
# Hoặc
docker-compose exec -T database mysql -u root -p1111 db_AIchatbot < backup.sql
```

## 🚦 Production Deployment

### 1. Production Mode

```bash
# Sử dụng nginx load balancer
make prod
# Hoặc
docker-compose --profile production up -d
```

### 2. Environment Variables

Cập nhật file `.env` với cấu hình production:

```env
SPRING_PROFILES_ACTIVE=production
GROQ_API_KEY=your-production-api-key
JWT_SECRET=your-production-jwt-secret
```

### 3. SSL Configuration

Thêm SSL certificates vào thư mục `nginx/ssl/`:
```
nginx/ssl/cert.pem
nginx/ssl/key.pem
```

## 🐛 Troubleshooting

### Common Issues

1. **Port conflicts**
   ```bash
   # Thay đổi ports trong docker-compose.yml
   ports:
     - "3006:3005"  # frontend
     - "8082:8080"  # backend
   ```

2. **Memory issues**
   ```bash
   # Tăng memory limit cho JVM
   environment:
     JAVA_OPTS: "-XX:MaxRAMPercentage=50.0"
   ```

3. **Database connection failed**
   ```bash
   # Kiểm tra database health
   docker-compose exec database mysqladmin ping -h localhost -u root -p1111
   
   # Restart database
   docker-compose restart database
   ```

4. **Frontend build fails**
   ```bash
   # Build lại frontend
   docker-compose build --no-cache frontend
   ```

### Clean Reset

```bash
# Dọn dẹp hoàn toàn và build lại
make clean
make build
make up
```

## 📏 Resource Requirements

### Minimum
- **CPU**: 2 cores
- **RAM**: 4GB
- **Disk**: 10GB

### Recommended
- **CPU**: 4 cores
- **RAM**: 8GB  
- **Disk**: 20GB

## 🔧 Customization

### Environment Variables

Xem file `.env.example` để biết tất cả các biến có thể cấu hình.

### Custom Networks

```yaml
networks:
  ai-chat-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

### Volume Mounts

```yaml
volumes:
  - ./custom-config:/app/config
  - ./logs:/app/logs
```

## 📞 Support

Nếu gặp vấn đề, hãy:

1. Kiểm tra logs: `make logs`
2. Kiểm tra health: `make health`  
3. Xem resource usage: `make stats`
4. Thử clean reset: `make clean && make build && make up`
