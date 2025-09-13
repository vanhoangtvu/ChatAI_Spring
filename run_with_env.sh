#!/bin/bash

# Run Backend with Environment Variables Script

echo "🚀 Starting Backend with Environment Variables..."
echo "================================================"

# Check if .env file exists
if [ ! -f .env ]; then
    echo "❌ .env file not found! Creating from .env.example"
    cp .env.example .env
    echo "✅ Created .env file. Please update GROQ_API_KEY if needed."
fi

# Load environment variables
echo "📋 Loading environment variables from .env file..."
set -a
source .env
set +a

# Display loaded variables (hiding sensitive data)
echo ""
echo "🔍 Loaded Environment Variables:"
echo "--------------------------------"
echo "DB_HOST: $DB_HOST"
echo "DB_PORT: $DB_PORT" 
echo "DB_NAME: $DB_NAME"
echo "DB_USERNAME: $DB_USERNAME"
echo "JWT_SECRET: ${JWT_SECRET:0:20}..."
echo "JWT_EXPIRATION: $JWT_EXPIRATION"
echo "GROQ_API_KEY: ${GROQ_API_KEY:0:10}..."
echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo ""

# Check if MySQL is running
echo "🗄️ Checking MySQL connection..."
if mysql -h${DB_HOST:-localhost} -P${DB_PORT:-3306} -u${DB_USERNAME:-root} -p${DB_PASSWORD:-1111} -e "SELECT 1;" ${DB_NAME:-db_AIchatbot} 2>/dev/null; then
    echo "✅ MySQL connection successful"
else
    echo "❌ MySQL connection failed. Starting MySQL if needed..."
    
    # Try to start MySQL service
    if sudo systemctl is-active --quiet mysql; then
        echo "✅ MySQL service is running"
    else
        echo "🔄 Starting MySQL service..."
        sudo systemctl start mysql
        sleep 3
    fi
    
    # Test again
    if mysql -h${DB_HOST:-localhost} -P${DB_PORT:-3306} -u${DB_USERNAME:-root} -p${DB_PASSWORD:-1111} -e "SELECT 1;" ${DB_NAME:-db_AIchatbot} 2>/dev/null; then
        echo "✅ MySQL connection successful after restart"
    else
        echo "⚠️ MySQL connection still failing, but continuing anyway..."
        echo "   Backend will try to connect and may auto-create database"
    fi
fi

# Check if backend classes exist
if [ ! -d backend/target/classes ]; then
    echo "🔨 Backend classes not found. Building..."
    cd backend
    mvn compile -DskipTests
    cd ..
    
    if [ ! -d backend/target/classes ]; then
        echo "❌ Compile failed. Please check Maven compile errors."
        exit 1
    fi
else
    echo "✅ Backend classes found"
fi

# Export environment variables for Java application
export DB_HOST
export DB_PORT
export DB_NAME
export DB_USERNAME
export DB_PASSWORD
export JWT_SECRET
export JWT_EXPIRATION
export GROQ_API_KEY
export SPRING_PROFILES_ACTIVE

echo ""
echo "🎯 Starting Spring Boot Backend..."
echo "=================================="
echo "Press Ctrl+C to stop"
echo ""

# Run the backend with environment variables
cd backend
java -jar \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default} \
    -Dspring.datasource.url=jdbc:mysql://${DB_HOST:-localhost}:${DB_PORT:-3306}/${DB_NAME:-db_AIchatbot}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&autoReconnect=true&failOverReadOnly=false&maxReconnects=3&initialTimeout=1&useLocalSessionState=true&elideSetAutoCommits=true&cachePrepStmts=true&useServerPrepStmts=false&rewriteBatchedStatements=true&maintainTimeStats=false&useUnbufferedInput=false&useReadAheadInput=false \
    -Dspring.datasource.username=${DB_USERNAME:-root} \
    -Dspring.datasource.password=${DB_PASSWORD:-1111} \
    -Djwt.secret=${JWT_SECRET} \
    -Djwt.expiration=${JWT_EXPIRATION:-86400000} \
    -Dgroq.api-key=${GROQ_API_KEY} \
    -Dserver.port=8080 \
    target/spring-ai-chat-1.0.0.jar
