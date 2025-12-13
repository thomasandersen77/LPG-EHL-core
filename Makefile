.PHONY: help build test run-local run-api stop clean docker-build docker-up docker-down logs

# Default target
help:
	@echo "LPG EHL - Makefile Commands"
	@echo ""
	@echo "Development:"
	@echo "  make build          - Build all modules"
	@echo "  make test           - Run all tests"
	@echo "  make run-local      - Start local environment (Docker Compose)"
	@echo "  make run-api        - Run API locally (requires DB)"
	@echo "  make stop           - Stop all running containers"
	@echo "  make clean          - Clean build artifacts"
	@echo ""
	@echo "Docker:"
	@echo "  make docker-build   - Build Docker images"
	@echo "  make docker-up      - Start docker-compose (detached)"
	@echo "  make docker-down    - Stop and remove containers"
	@echo "  make logs           - Tail all container logs"
	@echo ""
	@echo "Testing:"
	@echo "  make test-api       - Run API tests only"
	@echo "  make test-core      - Run core tests only"
	@echo "  make test-integration - Run integration tests"

# Build all modules
build:
	@echo "🔨 Building all modules..."
	mvn clean package -DskipTests

# Run all tests
test:
	@echo "🧪 Running all tests..."
	mvn test

# Run API tests only
test-api:
	@echo "🧪 Running API tests..."
	mvn test -pl lpg-ehl-api

# Run core tests only
test-core:
	@echo "🧪 Running core tests..."
	mvn test -pl lpg-ehl-core

# Run integration tests
test-integration:
	@echo "🧪 Running integration tests..."
	mvn verify -pl lpg-ehl-api

# Start local development environment
run-local:
	@echo "🚀 Starting local development environment..."
	docker-compose -f docker-compose-local.yaml up

# Run API locally (assumes PostgreSQL is running)
run-api:
	@echo "🚀 Running API with local profile..."
	mvn spring-boot:run -pl lpg-ehl-api -Dspring-boot.run.profiles=local

# Stop all containers
stop:
	@echo "🛑 Stopping containers..."
	docker-compose -f docker-compose-local.yaml down

# Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	mvn clean
	rm -rf local-data/
	rm -rf */target/

# Build Docker images
docker-build:
	@echo "🐳 Building Docker images..."
	docker build -t lpg-ehl:latest .

# Start docker-compose in detached mode
docker-up:
	@echo "🐳 Starting Docker Compose (detached)..."
	docker-compose -f docker-compose-local.yaml up -d

# Stop and remove docker-compose containers
docker-down:
	@echo "🐳 Stopping Docker Compose..."
	docker-compose -f docker-compose-local.yaml down -v

# Tail logs
logs:
	@echo "📜 Tailing logs..."
	docker-compose -f docker-compose-local.yaml logs -f

# Quick local test cycle
quick-test: docker-up
	@echo "⚡ Running quick test cycle..."
	sleep 5
	curl -f http://localhost:8080/actuator/health || echo "API not ready yet"
	@echo "✅ Quick test complete"
