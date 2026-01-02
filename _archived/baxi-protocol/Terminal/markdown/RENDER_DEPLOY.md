# Render Deployment Guide

This guide explains how to deploy the complete LPG-EHL system to Render.com.

## Architecture Overview

The system consists of 4 services on Render:

```
┌─────────────────┐
│   lpg-web       │  Static Site (React/Vite)
│   (Frontend)    │  → Calls API via HTTPS
└─────────────────┘
         ↓
┌─────────────────┐
│   lpg-api       │  Web Service (Spring Boot)
│   (Backend)     │  → Manages dispensers, transactions, DB
└─────────────────┘
         ↓
┌─────────────────┐
│  lpg-emulator   │  Web Service (Spring Boot)
│  (Simulator)    │  → Simulates EHL protocol over HTTP
└─────────────────┘
         ↓
┌─────────────────┐
│   lpg-db        │  PostgreSQL Database
│   (Database)    │  → Stores transactions, events, status
└─────────────────┘
```

### Key Design Points

- **No TCP/Serial**: Emulator exposes HTTP endpoints instead of TCP EHL protocol
- **Internal Networking**: API calls Emulator via Render's private networking (`http://lpg-emulator:10000`)
- **Dynamic Ports**: Render assigns PORT dynamically via `$PORT` env var
- **Logging**: Emulator captures logs in-memory buffer, accessible via `/api/emulator/internal/logs`
- **Maven Multi-Module**: Emulator depends on `lpg-ehl-core` as Maven dependency (not separate TCP service)

## Prerequisites

1. **Render Account**: Sign up at https://render.com
2. **GitHub/GitLab Repository**: Push your code to version control
3. **Local Testing**: Run `docker-compose-render-test.yaml` first to verify

## Step 1: Local Testing

Before deploying to Render, test the setup locally:

```bash
# Build the project
./mvnw clean install -DskipTests

# Build Docker images
docker-compose -f docker-compose-render-test.yaml build

# Start all services
docker-compose -f docker-compose-render-test.yaml up

# Test endpoints
curl http://localhost:10001/actuator/health
curl http://localhost:10000/actuator/health
curl http://localhost:10000/api/emulator/internal/logs
curl -H "Authorization: Bearer test-token-render-12345" \
  http://localhost:10001/api/v1/dispensers

# Stop when done
docker-compose -f docker-compose-render-test.yaml down
```

## Step 2: Push Code to Repository

```bash
git add .
git commit -m "Add Render deployment configuration"
git push origin main
```

## Step 3: Deploy to Render

### Option A: Deploy via Blueprint (Recommended)

1. Go to Render Dashboard
2. Click "New" → "Blueprint"
3. Connect your repository
4. Select the `render.yaml` file
5. Click "Apply"

Render will automatically create all 4 services.

### Option B: Manual Deployment

If you prefer manual setup:

#### 3.1 Create Database

1. Dashboard → "New" → "PostgreSQL"
2. **Name**: `lpg-db`
3. **Database**: `lpg_db`
4. **User**: `lpg_user`
5. **Region**: `Frankfurt` (or closest)
6. **Plan**: `Starter` (includes backups)
7. Click "Create Database"

#### 3.2 Deploy Emulator

1. Dashboard → "New" → "Web Service"
2. Connect repository
3. **Name**: `lpg-emulator`
4. **Runtime**: `Docker`
5. **Dockerfile Path**: `./Dockerfile.emulator`
6. **Region**: `Frankfurt`
7. **Plan**: `Standard`
8. **Environment Variables**:
   ```
   JAVA_TOOL_OPTIONS = -Dserver.port=$PORT
   SPRING_PROFILES_ACTIVE = production
   EMULATOR_ADDRESS = 1
   EMULATOR_PRICE_PER_LITRE_CENTS = 1590
   EMULATOR_LITRES_PER_SECOND = 0.5
   ```
9. Click "Create Web Service"

#### 3.3 Deploy API

1. Dashboard → "New" → "Web Service"
2. Connect repository
3. **Name**: `lpg-api`
4. **Runtime**: `Docker`
5. **Dockerfile Path**: `./Dockerfile.api`
6. **Region**: `Frankfurt`
7. **Plan**: `Standard`
8. **Environment Variables**:
   ```
   JAVA_TOOL_OPTIONS = -Dserver.port=$PORT
   SPRING_PROFILES_ACTIVE = production
   EMULATOR_BASE_URL = http://lpg-emulator:10000
   API_AUTH_TOKEN = <GENERATE-SECURE-TOKEN>
   CORS_ALLOWED_ORIGINS = https://lpg-web.onrender.com
   AZURE_ENABLED = false
   AZURE_STORAGE_CONNECTION_STRING = (leave empty if disabled)
   ```
9. **Database Environment Variables** (link to lpg-db):
   - `SPRING_DATASOURCE_URL` → From Database: `lpg-db` → `connectionString`
   - `SPRING_DATASOURCE_USERNAME` → From Database: `lpg-db` → `user`
   - `SPRING_DATASOURCE_PASSWORD` → From Database: `lpg-db` → `password`
10. Click "Create Web Service"

#### 3.4 Deploy Frontend

1. Dashboard → "New" → "Static Site"
2. Connect repository
3. **Name**: `lpg-web`
4. **Root Directory**: `lpg-web`
5. **Build Command**: `npm ci && npm run build`
6. **Publish Directory**: `dist`
7. **Region**: `Frankfurt`
8. **Environment Variables**:
   ```
   VITE_API_URL = https://lpg-api.onrender.com
   ```
   (Replace with actual API URL from step 3.3)
9. Click "Create Static Site"

## Step 4: Configure Environment Variables

### Required Secrets

Generate a strong API token:
```bash
openssl rand -base64 32
```

Set this in API service as `API_AUTH_TOKEN`.

### Optional: Azure Storage Queue

If you want to sync transactions to Azure:

1. Create Azure Storage Account
2. Get connection string
3. Set in API:
   ```
   AZURE_ENABLED = true
   AZURE_STORAGE_CONNECTION_STRING = <your-connection-string>
   AZURE_QUEUE_NAME = lpg-transactions
   ```

## Step 5: Verify Deployment

### Check Service Health

```bash
# Emulator
curl https://lpg-emulator.onrender.com/actuator/health

# API
curl https://lpg-api.onrender.com/actuator/health

# Frontend
curl https://lpg-web.onrender.com
```

### Test API Authentication

```bash
curl -H "Authorization: Bearer YOUR-API-TOKEN" \
  https://lpg-api.onrender.com/api/v1/dispensers
```

### Check Emulator Logs

```bash
curl https://lpg-emulator.onrender.com/api/emulator/internal/logs
```

### Test Database Connection

```bash
curl -H "Authorization: Bearer YOUR-API-TOKEN" \
  https://lpg-api.onrender.com/api/v1/transactions
```

## Step 6: Monitor Services

### View Logs

1. Go to each service in Render Dashboard
2. Click "Logs" tab
3. Stream real-time logs

### Metrics

1. Service Dashboard → "Metrics"
2. Monitor CPU, Memory, Response Time

### Health Checks

Render automatically monitors `/actuator/health` endpoints.

## Troubleshooting

### Emulator Not Responding

**Symptom**: API logs show "Connection refused to emulator"

**Solution**:
1. Check emulator health: `curl https://lpg-emulator.onrender.com/actuator/health`
2. Verify `EMULATOR_BASE_URL` in API uses internal DNS: `http://lpg-emulator:10000`
3. Check emulator logs for startup errors

### Database Connection Failed

**Symptom**: API fails to start, Liquibase errors

**Solution**:
1. Verify database env vars are set (connectionString, user, password)
2. Check database is in same region
3. Verify database is "Available" in Render Dashboard

### Port Binding Error

**Symptom**: "Address already in use" or "Failed to bind"

**Solution**:
- Ensure `JAVA_TOOL_OPTIONS = -Dserver.port=$PORT` is set
- Verify Dockerfile doesn't hardcode port
- Check application-production.yaml uses `${PORT:10000}`

### Frontend Can't Reach API

**Symptom**: CORS errors, 404 errors in browser console

**Solution**:
1. Verify `VITE_API_URL` in frontend points to correct API URL
2. Add frontend URL to `CORS_ALLOWED_ORIGINS` in API
3. Rebuild frontend after changing env vars

### Slow Cold Starts

**Symptom**: First request after idle takes 30+ seconds

**Solution**:
- Upgrade to Render "Standard" plan (no cold starts)
- Or accept 30-60s cold start on free tier
- Add keep-alive ping to services

## Updating the Deployment

### Code Changes

```bash
git add .
git commit -m "Update feature X"
git push origin main
```

Render auto-deploys on push.

### Environment Variable Changes

1. Go to Service → "Environment"
2. Add/Update variables
3. Click "Save Changes"
4. Service auto-restarts

### Manual Redeploy

1. Service Dashboard → "Manual Deploy"
2. Select branch
3. Click "Deploy"

## Costs (Estimate)

- **Database (Starter)**: $7/month
- **API (Standard)**: $7/month
- **Emulator (Standard)**: $7/month
- **Frontend (Static)**: Free
- **Total**: ~$21/month

## Production Checklist

- [ ] Strong `API_AUTH_TOKEN` set (32+ random characters)
- [ ] `AZURE_STORAGE_CONNECTION_STRING` set if using Azure sync
- [ ] Database backups enabled (Starter plan or higher)
- [ ] CORS configured correctly for production domain
- [ ] Health checks passing for all services
- [ ] Logs reviewed for startup errors
- [ ] Test complete flow: Frontend → API → Emulator → Database

## Rollback Procedure

If deployment fails:

1. Go to Service → "Deploy" tab
2. Find previous successful deploy
3. Click "Rollback"

Or via git:
```bash
git revert HEAD
git push origin main
```

## Support

- Render Docs: https://render.com/docs
- Render Community: https://community.render.com
- Project Issues: [Your GitHub Issues URL]

## Next Steps

1. **Custom Domain**: Add your domain in Render Dashboard
2. **SSL**: Automatic via Let's Encrypt
3. **Monitoring**: Set up uptime monitoring (e.g., UptimeRobot)
4. **Backups**: Schedule database backups
5. **CI/CD**: Add tests to run before deploy (GitHub Actions)
