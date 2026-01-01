# Docker Deployment Guide

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+

## Quick Start

### 1. Setup Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 2. Start Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f lpg-ehl-app
```

### 3. Access Services

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Database UI (Adminer)**: http://localhost:8081
  - System: PostgreSQL
  - Server: postgres
  - Username: lpg_user
  - Password: (from .env DB_PASSWORD)
  - Database: lpg_ehl

## Development

### Start in Development Mode

```bash
# Use development configuration
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Hot reload is enabled
# Remote debugging on port 5005
```

### Build Application

```bash
# Build Docker image
docker-compose build

# Rebuild without cache
docker-compose build --no-cache
```

### Database Management

```bash
# Access PostgreSQL CLI
docker-compose exec postgres psql -U lpg_user -d lpg_ehl

# Backup database
docker-compose exec postgres pg_dump -U lpg_user lpg_ehl > backup.sql

# Restore database
docker-compose exec -T postgres psql -U lpg_user lpg_ehl < backup.sql
```

## Serial Port Configuration

### Linux

```bash
# Find serial device
ls -la /dev/tty*

# Update .env
SERIAL_PORT=/dev/ttyUSB0

# Grant permissions (if needed)
sudo chmod 666 /dev/ttyUSB0
```

### macOS

```bash
# Find serial device
ls -la /dev/tty.usbserial-*

# Update .env
SERIAL_PORT=/dev/tty.usbserial-A12345
```

### Windows (Docker Desktop + WSL2)

```bash
# Use COM port mapping
SERIAL_PORT=COM3
```

## Production Deployment

### 1. Build Production Image

```bash
docker-compose build --no-cache
```

### 2. Configure Production Environment

```bash
# Update .env with production values
DB_PASSWORD=<strong-password>
LOG_LEVEL=WARN
SERIAL_PORT=/dev/ttyUSB0
DISPENSER_ADDRESSES=1,2,3,4
```

### 3. Start Production Stack

```bash
docker-compose up -d
```

### 4. Monitor Services

```bash
# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Check health
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### Application Won't Start

```bash
# Check logs
docker-compose logs lpg-ehl-app

# Verify database connection
docker-compose exec postgres pg_isready -U lpg_user
```

### Serial Port Access Issues

```bash
# Verify device exists
ls -la /dev/ttyUSB0

# Check container has access
docker-compose exec lpg-ehl-app ls -la /dev/ttyUSB0

# Grant permissions (Linux)
sudo usermod -a -G dialout $USER
sudo chmod 666 /dev/ttyUSB0
```

### Database Issues

```bash
# Reset database
docker-compose down -v  # WARNING: Deletes all data
docker-compose up -d
```

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes data)
docker-compose down -v

# Stop and remove images
docker-compose down --rmi all
```

## Updating Application

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Monitoring

### View Resource Usage

```bash
docker stats lpg-ehl-app lpg-ehl-postgres
```

### Access Container Shell

```bash
# Application container
docker-compose exec lpg-ehl-app sh

# Database container
docker-compose exec postgres bash
```

## Security Notes

- Change default database password in `.env`
- Use strong passwords in production
- Restrict port exposure in production (remove Adminer)
- Use secrets management for sensitive data
- Run as non-root user (handled in Dockerfile)
- Enable TLS for database connections in production

## Next Steps

After deployment:
1. Configure dispenser addresses
2. Test serial communication
3. Set up monitoring and alerting
4. Configure backups
5. Set up log aggregation
