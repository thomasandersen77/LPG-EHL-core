#!/bin/bash

set -e

echo "🚀 Starting LPG EHL System..."

# Start infrastructure
echo "📦 Starting Docker services (PostgreSQL + Azurite)..."
docker-compose -f docker-compose-local.yaml up -d postgres azurite

echo "⏳ Waiting for database to be ready..."
sleep 5

# Build backend
echo "🔨 Building backend..."
mvn clean package -DskipTests

# Start API
echo "🌐 Starting API on port 8080..."
cd lpg-ehl-api
nohup mvn spring-boot:run -Dspring-boot.run.profiles=local > ../api.log 2>&1 &
API_PID=$!
echo "API started with PID: $API_PID"
cd ..

echo "⏳ Waiting for API to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ API is ready!"
        break
    fi
    echo "   Waiting... ($i/30)"
    sleep 2
done

# Start Frontend
echo "🎨 Starting Frontend on port 3000..."
cd lpg-web
nohup npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend started with PID: $FRONTEND_PID"
cd ..

echo ""
echo "✅ System started successfully!"
echo ""
echo "📍 Access points:"
echo "   Frontend:  http://localhost:3000"
echo "   API:       http://localhost:8080"
echo "   Swagger:   http://localhost:8080/swagger-ui.html"
echo ""
echo "📋 Logs:"
echo "   API:       tail -f api.log"
echo "   Frontend:  tail -f frontend.log"
echo ""
echo "🛑 To stop:"
echo "   kill $API_PID $FRONTEND_PID"
echo "   docker-compose -f docker-compose-local.yaml down"
