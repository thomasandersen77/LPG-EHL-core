#!/bin/bash
# LPG-EHL Production Deployment Script
# 
# This script deploys the LPG-EHL monolith to a Linux station.
# It installs PostgreSQL, creates users, sets up the systemd service,
# and deploys the application JAR.
#
# Requirements:
# - Ubuntu/Debian Linux (tested on Ubuntu 22.04+)
# - Root or sudo access
# - Internet connection for package installation
#
# Usage:
#   1. Copy this script and the JAR to the target machine:
#      scp deploy.sh lpg-ehl-monolith.jar lpg-ehl.service user@station:/tmp/
#   
#   2. SSH to the machine and run:
#      cd /tmp
#      chmod +x deploy.sh
#      sudo ./deploy.sh
#
#   3. Configure station-specific settings:
#      sudo nano /opt/lpg-ehl/lpg-ehl.env
#
#   4. Start the service:
#      sudo systemctl start lpg-ehl
#
# What this script does:
# - Installs PostgreSQL 16
# - Creates lpg_ehl database and lpg_user
# - Creates lpg-ehl system user
# - Sets up /opt/lpg-ehl directory structure
# - Installs systemd service
# - Configures automatic restarts

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
APP_USER="lpg-ehl"
APP_GROUP="lpg-ehl"
APP_DIR="/opt/lpg-ehl"
JAR_NAME="lpg-ehl-monolith.jar"
DB_NAME="lpg_ehl"
DB_USER="lpg_user"
DB_PASSWORD=""  # Will be generated

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}ERROR: This script must be run as root (use sudo)${NC}"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  LPG-EHL Production Deployment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Install PostgreSQL
echo -e "${GREEN}[1/7]${NC} Installing PostgreSQL..."

if command -v psql &> /dev/null; then
    echo "  PostgreSQL already installed: $(psql --version)"
else
    echo "  Installing PostgreSQL 16..."
    apt-get update
    apt-get install -y postgresql-16 postgresql-contrib-16
    systemctl enable postgresql
    systemctl start postgresql
    echo -e "  ${GREEN}✓${NC} PostgreSQL installed"
fi
echo ""

# Step 2: Create database and user
echo -e "${GREEN}[2/7]${NC} Setting up PostgreSQL database..."

# Generate secure password if not set
if [ -z "$DB_PASSWORD" ]; then
    DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    echo "  Generated database password: $DB_PASSWORD"
fi

# Create database and user
sudo -u postgres psql <<EOF
-- Create database if it doesn't exist
SELECT 'CREATE DATABASE $DB_NAME'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME')\gexec

-- Create user if it doesn't exist
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = '$DB_USER') THEN
        CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';
    END IF;
END
\$\$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;

-- Connect to database and grant schema permissions
\c $DB_NAME
GRANT ALL ON SCHEMA public TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $DB_USER;
EOF

echo -e "  ${GREEN}✓${NC} Database configured"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Step 3: Create application user
echo -e "${GREEN}[3/7]${NC} Creating application user..."

if id "$APP_USER" &>/dev/null; then
    echo "  User $APP_USER already exists"
else
    useradd --system --no-create-home --shell /bin/false $APP_USER
    echo -e "  ${GREEN}✓${NC} User created: $APP_USER"
fi
echo ""

# Step 4: Create application directory structure
echo -e "${GREEN}[4/7]${NC} Setting up application directories..."

mkdir -p $APP_DIR/logs
mkdir -p $APP_DIR/data
mkdir -p $APP_DIR/backup

# Create environment file with database credentials
cat > $APP_DIR/lpg-ehl.env <<EOF
# LPG-EHL Environment Configuration
# Generated on: $(date)

# Database
DB_PASSWORD=$DB_PASSWORD

# Station Identification (CONFIGURE FOR EACH STATION)
STATION_ID=S001
EDGE_ID=EDGE-S001-01
DISPENSER_ID=D001

# Serial Port Configuration (adjust for your hardware)
SERIAL_PORT=/dev/ttyUSB0
SERIAL_BAUD_RATE=9600

# Azure Cloud Sync (optional)
AZURE_ENABLED=false
# AZURE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=...
# AZURE_QUEUE_NAME=lpg-transactions

# Nets Cloud Connect (optional)
NETS_ENABLED=false
# NETS_CLOUD_HOST=3.33.230.243
# NETS_CLOUD_PORT=6001

# Security
API_AUTH_TOKEN=$(openssl rand -hex 32)

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_LPG=INFO
EOF

chmod 600 $APP_DIR/lpg-ehl.env
chown -R $APP_USER:$APP_GROUP $APP_DIR

echo -e "  ${GREEN}✓${NC} Directory structure created"
echo "  Location: $APP_DIR"
echo ""

# Step 5: Install JAR file
echo -e "${GREEN}[5/7]${NC} Installing application JAR..."

if [ ! -f "/tmp/$JAR_NAME" ]; then
    echo -e "${RED}ERROR: JAR file not found at /tmp/$JAR_NAME${NC}"
    echo "Please copy the JAR file to /tmp/ first"
    exit 1
fi

cp /tmp/$JAR_NAME $APP_DIR/$JAR_NAME
chmod 755 $APP_DIR/$JAR_NAME
chown $APP_USER:$APP_GROUP $APP_DIR/$JAR_NAME

echo -e "  ${GREEN}✓${NC} JAR installed: $APP_DIR/$JAR_NAME"
JAR_SIZE=$(du -h "$APP_DIR/$JAR_NAME" | cut -f1)
echo "  Size: $JAR_SIZE"
echo ""

# Step 6: Install systemd service
echo -e "${GREEN}[6/7]${NC} Installing systemd service..."

if [ ! -f "/tmp/lpg-ehl.service" ]; then
    echo -e "${RED}ERROR: Service file not found at /tmp/lpg-ehl.service${NC}"
    echo "Please copy the service file to /tmp/ first"
    exit 1
fi

cp /tmp/lpg-ehl.service /etc/systemd/system/lpg-ehl.service
chmod 644 /etc/systemd/system/lpg-ehl.service

systemctl daemon-reload
systemctl enable lpg-ehl

echo -e "  ${GREEN}✓${NC} Service installed and enabled"
echo ""

# Step 7: Configure PostgreSQL to start on boot
echo -e "${GREEN}[7/7]${NC} Configuring automatic startup..."

systemctl enable postgresql
systemctl is-enabled postgresql >/dev/null && echo -e "  ${GREEN}✓${NC} PostgreSQL auto-start: enabled"
systemctl is-enabled lpg-ehl >/dev/null && echo -e "  ${GREEN}✓${NC} LPG-EHL auto-start: enabled"
echo ""

# Deployment summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Deployment Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Installation Summary:${NC}"
echo "  PostgreSQL:    Installed and running"
echo "  Database:      $DB_NAME"
echo "  DB User:       $DB_USER"
echo "  App User:      $APP_USER"
echo "  App Directory: $APP_DIR"
echo "  JAR File:      $APP_DIR/$JAR_NAME"
echo "  Service:       lpg-ehl.service"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo ""
echo "1. Configure station-specific settings:"
echo -e "   ${BLUE}sudo nano $APP_DIR/lpg-ehl.env${NC}"
echo ""
echo "   Edit these values:"
echo "   - STATION_ID (e.g., S001, S002)"
echo "   - EDGE_ID (e.g., EDGE-S001-01)"
echo "   - DISPENSER_ID (e.g., D001)"
echo "   - SERIAL_PORT (e.g., /dev/ttyUSB0)"
echo ""
echo "2. Start the service:"
echo -e "   ${BLUE}sudo systemctl start lpg-ehl${NC}"
echo ""
echo "3. Check service status:"
echo -e "   ${BLUE}sudo systemctl status lpg-ehl${NC}"
echo ""
echo "4. View logs:"
echo -e "   ${BLUE}sudo journalctl -u lpg-ehl -f${NC}"
echo ""
echo "5. Access the application:"
echo "   Web UI:  http://localhost:8080"
echo "   API:     http://localhost:8080/api/*"
echo "   Swagger: http://localhost:8080/swagger-ui.html"
echo ""
echo -e "${YELLOW}Service Management:${NC}"
echo "  Start:   sudo systemctl start lpg-ehl"
echo "  Stop:    sudo systemctl stop lpg-ehl"
echo "  Restart: sudo systemctl restart lpg-ehl"
echo "  Status:  sudo systemctl status lpg-ehl"
echo "  Logs:    sudo journalctl -u lpg-ehl -f"
echo ""
echo -e "${YELLOW}Security Note:${NC}"
echo "  Database password and API token are stored in:"
echo "  $APP_DIR/lpg-ehl.env (600 permissions)"
echo ""
echo -e "${GREEN}Ready for production! 🚀${NC}"
