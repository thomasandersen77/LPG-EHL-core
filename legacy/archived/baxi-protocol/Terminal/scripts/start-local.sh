#!/bin/bash
# Quick start script for LPG-EHL local development
# This starts all services in Docker and shows you the access points

set -e

echo "🚀 Starting LPG-EHL Local Development Environment"
echo "================================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Stop any existing containers
echo "🧹 Cleaning up old containers..."
docker-compose -f docker-compose-local.yaml down 2>/dev/null || true
echo ""

# Build and start all services
echo "🏗️  Building and starting all services..."
echo "This may take a few minutes on first run..."
echo ""
docker-compose -f docker-compose-local.yaml up -d --build

echo ""
echo "⏳ Waiting for services to be ready..."
echo ""

# Wait for API to be healthy
max_attempts=60
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ API is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo -n "."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo ""
    echo "⚠️  API did not start in time. Check logs with:"
    echo "   docker-compose -f docker-compose-local.yaml logs api"
    exit 1
fi

echo ""
echo ""
echo "✨ LPG-EHL is now running!"
echo "=========================="
echo ""
echo "🌐 Access Points:"
echo "  Frontend:       http://localhost:3000"
echo "  API:            http://localhost:8080"
echo "  Swagger UI:     http://localhost:8080/swagger-ui.html"
echo "  API Health:     http://localhost:8080/actuator/health"
echo ""
echo "🗄️  Database:"
echo "  Host:           localhost"
echo "  Port:           5432"
echo "  Database:       lpg_ehl"
echo "  User:           lpg_user"
echo "  Password:       lpg_dev_password"
echo "  Connection:     psql -h localhost -U lpg_user -d lpg_ehl"
echo ""
echo "🔧 Development Tools:"
echo "  Emulator:       tcp://localhost:9000"
echo "  Azurite Queue:  http://localhost:10001"
echo "  WireMock:       http://localhost:8081"
echo ""
echo "📋 Useful Commands:"
echo "  View logs:      docker-compose -f docker-compose-local.yaml logs -f"
echo "  View API logs:  docker-compose -f docker-compose-local.yaml logs -f api"
echo "  Stop all:       docker-compose -f docker-compose-local.yaml down"
echo "  Restart:        docker-compose -f docker-compose-local.yaml restart"
echo ""
echo "🎯 Quick Test:"
echo "  curl http://localhost:8080/actuator/health"
echo ""
echo "Happy coding! 🚀"
