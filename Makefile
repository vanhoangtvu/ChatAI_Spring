# AI Chat Docker Management
.PHONY: build up down restart logs clean dev prod health

# Default target
all: build up

# Build all images
build:
	@echo "Building Docker images..."
	docker-compose build --no-cache

# Start all services
up:
	@echo "Starting all services..."
	docker-compose up -d

# Stop all services
down:
	@echo "Stopping all services..."
	docker-compose down

# Restart all services
restart: down up

# View logs
logs:
	@echo "Showing logs for all services..."
	docker-compose logs -f

# View logs for specific service
logs-backend:
	docker-compose logs -f backend

logs-frontend:
	docker-compose logs -f frontend

logs-db:
	docker-compose logs -f database

# Clean up everything (containers, images, volumes)
clean:
	@echo "Cleaning up Docker resources..."
	docker-compose down -v --rmi all
	docker system prune -f

# Development mode (with file watching)
dev:
	@echo "Starting in development mode..."
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Production mode (with nginx load balancer)
prod:
	@echo "Starting in production mode..."
	docker-compose --profile production up -d

# Health check
health:
	@echo "Checking service health..."
	@echo "Backend health:"
	@curl -f http://localhost:8081/actuator/health || echo "Backend not healthy"
	@echo "\nFrontend health:"
	@curl -f http://localhost:3005/health || echo "Frontend not healthy"
	@echo "\nDatabase health:"
	@docker-compose exec database mysqladmin ping -h localhost -u root -p1111 || echo "Database not healthy"

# Database operations
db-backup:
	@echo "Backing up database..."
	docker-compose exec database mysqldump -u root -p1111 db_AIchatbot > backup_$(shell date +%Y%m%d_%H%M%S).sql

db-restore:
	@echo "Restoring database from backup..."
	@read -p "Enter backup file name: " file; \
	docker-compose exec -T database mysql -u root -p1111 db_AIchatbot < $$file

# Show Docker resource usage
stats:
	@echo "Docker resource usage:"
	docker stats --no-stream

# Show running containers
ps:
	@echo "Running containers:"
	docker-compose ps

# Update and rebuild
update: down build up

# Quick restart for development
quick-restart:
	docker-compose restart backend frontend

# Show help
help:
	@echo "Available commands:"
	@echo "  build       - Build all Docker images"
	@echo "  up          - Start all services"
	@echo "  down        - Stop all services"
	@echo "  restart     - Stop and start all services"
	@echo "  logs        - Show logs for all services"
	@echo "  clean       - Remove containers, images, and volumes"
	@echo "  dev         - Start in development mode"
	@echo "  prod        - Start in production mode"
	@echo "  health      - Check service health"
	@echo "  stats       - Show Docker resource usage"
	@echo "  ps          - Show running containers"
	@echo "  update      - Update and rebuild everything"
	@echo "  help        - Show this help message"
