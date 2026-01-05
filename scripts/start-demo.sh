#!/bin/bash
set -e

echo "🚀 Starting LPG-EHL + MinLPG Demo Environment"
echo "=============================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Install with: sdk install java 21.0.7-tem"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found. Install Node.js 20+"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Install Docker Desktop"
    exit 1
fi

echo "✅ Prerequisites OK"
echo ""

# Start LPG-EHL infrastructure
echo "${BLUE}🔵 Starting LPG-EHL (Pump System)${NC}"
echo "-----------------------------------"
cd ~/git/NorgesGass/lpg-ehl

echo "Starting PostgreSQL and Azurite..."
docker-compose -f docker-compose-local.yaml up -d postgres azurite

echo "Waiting for database to be ready..."
sleep 5

echo ""
echo "${GREEN}✅ LPG-EHL infrastructure started${NC}"
echo "   - PostgreSQL: localhost:5432"
echo "   - Azurite: localhost:10001"
echo ""

# Start MinLPG infrastructure
echo "${BLUE}🟢 Starting MinLPG (Cloud Admin)${NC}"
echo "-----------------------------------"
cd ~/git/NorgesGass/MinLPG

echo "Starting MinLPG database..."
docker-compose up -d minlpg-db

echo "Waiting for database to be ready..."
sleep 5

echo ""
echo "${GREEN}✅ MinLPG infrastructure started${NC}"
echo "   - PostgreSQL: localhost:5433"
echo ""

# Instructions
echo "${YELLOW}📖 Next Steps:${NC}"
echo "=============="
echo ""
echo "1️⃣  Start LPG-EHL Backend:"
echo "   cd ~/git/NorgesGass/lpg-ehl/lpg-ehl-api"
echo "   mvn spring-boot:run -Dspring-boot.run.profiles=local"
echo ""
echo "2️⃣  Start LPG-EHL Frontend (new terminal):"
echo "   cd ~/git/NorgesGass/lpg-ehl/lpg-web"
echo "   npm run dev"
echo ""
echo "3️⃣  Start MinLPG Backend (new terminal):"
echo "   cd ~/git/NorgesGass/MinLPG/backend/backend-api"
echo "   mvn spring-boot:run -Dspring-boot.run.profiles=local"
echo ""
echo "4️⃣  Start MinLPG Frontend (new terminal):"
echo "   cd ~/git/NorgesGass/MinLPG/frontend"
echo "   npm run dev"
echo ""
echo "${GREEN}🎯 Access Points:${NC}"
echo "   LPG-EHL Frontend:  http://localhost:3001"
echo "   LPG-EHL API:       http://localhost:8080"
echo "   MinLPG Frontend:   http://localhost:3000"
echo "   MinLPG API:        http://localhost:8081"
echo ""
echo "${YELLOW}📖 See DEMO_GUIDE.md for complete demo scenarios${NC}"
