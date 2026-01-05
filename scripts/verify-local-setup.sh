#!/bin/bash
# Verify Local Development Setup
# This script checks that all required services are running

set -e

echo "🔍 Verifying Local Development Setup..."
echo ""

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi
echo "✅ Docker is running"

# Check PostgreSQL container
if docker ps --filter "name=lpg-postgres-dev" --format "{{.Names}}" | grep -q "lpg-postgres-dev"; then
    echo "✅ PostgreSQL is running on port 5432"
    
    # Test connection
    if docker exec lpg-postgres-dev pg_isready -U lpg_user -d lpg_ehl > /dev/null 2>&1; then
        echo "   └─ Database connection: OK"
    else
        echo "   └─ ⚠️  Database not ready yet, give it a moment..."
    fi
else
    echo "❌ PostgreSQL is not running"
    echo "   Run: docker-compose -f docker-compose.postgres.yaml up -d"
    exit 1
fi

# Check Azurite container
if docker ps --filter "name=lpg-azurite" --format "{{.Names}}" | grep -q "lpg-azurite"; then
    echo "✅ Azurite is running on port 10001"
    
    # Test queue service
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:10001/devstoreaccount1?comp=list | grep -q "200\|400"; then
        echo "   └─ Queue service: OK"
    else
        echo "   └─ ⚠️  Queue service not responding yet..."
    fi
else
    echo "❌ Azurite is not running"
    echo "   Run: docker-compose -f docker-compose.postgres.yaml up -d"
    exit 1
fi

# Check Java version
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" = "21" ]; then
        echo "✅ Java 21 is installed"
    else
        echo "⚠️  Java version is $JAVA_VERSION (expected 21)"
        echo "   Run: sdk use java 21.0.7-tem"
    fi
else
    echo "❌ Java is not installed or not in PATH"
    exit 1
fi

# Check if port 8080 is available
if lsof -Pi :8080 -sTCP:LISTEN -t > /dev/null 2>&1; then
    echo "⚠️  Port 8080 is already in use"
    echo "   You may need to stop the running process"
    PROCESS=$(lsof -Pi :8080 -sTCP:LISTEN | tail -n 1)
    echo "   $PROCESS"
else
    echo "✅ Port 8080 is available"
fi

echo ""
echo "🎉 Setup verification complete!"
echo ""
echo "Next steps:"
echo "  1. Open the project in IntelliJ IDEA"
echo "  2. Run the 'LPG-EHL-API (Local Dev)' configuration"
echo "  3. Access http://localhost:8080"
echo ""
echo "See INTELLIJ_SETUP.md for detailed instructions."
