# Deployment Quick Start

Fast track to deploying LPG-EHL to Render.

## 60-Second Overview

This system deploys 4 services:
- **Frontend** (React static site)
- **API** (Spring Boot backend)
- **Emulator** (HTTP-based dispenser simulator)
- **Database** (PostgreSQL)

## Prerequisites

- Render account (https://render.com)
- Code pushed to GitHub/GitLab
- 10 minutes

## Deploy (3 Steps)

### 1. Test Locally (Optional but Recommended)

```bash
# Build everything
./mvnw clean install -DskipTests

# Test with Docker
docker-compose -f docker-compose-render-test.yaml up

# Verify (in another terminal)
curl http://localhost:10001/actuator/health  # API
curl http://localhost:10000/actuator/health  # Emulator

# Stop
docker-compose -f docker-compose-render-test.yaml down
```

### 2. Push to Git

```bash
git add .
git commit -m "Add Render deployment"
git push origin main
```

### 3. Deploy to Render

**Via Blueprint (Easiest)**:
1. Render Dashboard → "New" → "Blueprint"
2. Connect repository
3. Select `render.yaml`
4. Click "Apply"
5. Wait 5-10 minutes ⏳

**Manual**:
Follow [RENDER_DEPLOY.md](RENDER_DEPLOY.md) for step-by-step manual setup.

## Post-Deploy Setup

### Generate API Token

```bash
openssl rand -base64 32
```

### Update API Service

1. Go to `lpg-api` service
2. Environment → Add/Edit:
   ```
   API_AUTH_TOKEN = <paste-token-here>
   ```
3. Save (auto-restarts)

### Update Frontend

1. Go to `lpg-web` service
2. Environment → Edit:
   ```
   VITE_API_URL = https://lpg-api.onrender.com
   ```
   (Use actual API URL from Render)
3. Save (auto-rebuilds)

## Verify It Works

```bash
# Check services
curl https://lpg-api.onrender.com/actuator/health
curl https://lpg-emulator.onrender.com/actuator/health

# Test API (use your token)
curl -H "Authorization: Bearer YOUR-TOKEN" \
  https://lpg-api.onrender.com/api/v1/dispensers

# Check emulator logs
curl https://lpg-emulator.onrender.com/api/emulator/internal/logs

# Open frontend
open https://lpg-web.onrender.com
```

## Common Issues

### "Service Unavailable" (503)
- **Cause**: Services still starting (cold start)
- **Wait**: 30-60 seconds, try again

### "Connection Refused" from API to Emulator
- **Fix**: Verify `EMULATOR_BASE_URL = http://lpg-emulator:10000` in API
- **Not**: `https://lpg-emulator.onrender.com` (use internal DNS)

### CORS Errors in Browser
- **Fix**: Add frontend URL to `CORS_ALLOWED_ORIGINS` in API
- **Example**: `https://lpg-web.onrender.com`

### Database Migration Failed
- **Fix**: Check database is "Available" in Render
- **Check**: `SPRING_DATASOURCE_*` env vars are set

## File Reference

- `render.yaml` - Blueprint configuration
- `Dockerfile.api` - API container
- `Dockerfile.emulator` - Emulator container
- `docker-compose-render-test.yaml` - Local test environment
- `RENDER_DEPLOY.md` - Detailed deployment guide

## Architecture

```
Frontend (Static)  →  API (Web)  →  Emulator (Web)  →  Core (Lib)
                       ↓
                    Database (PostgreSQL)
```

## Cost

- ~$21/month (Standard plan for 3 services + Starter DB)
- Frontend is free
- Can reduce to ~$7/month with free tier (but has cold starts)

## Next Steps

1. ✅ Deploy to Render
2. 🔒 Set strong `API_AUTH_TOKEN`
3. 🌐 Add custom domain (optional)
4. 📊 Set up monitoring
5. 🧪 Add integration tests
6. 🚀 Configure Azure sync (if needed)

## Full Documentation

See [RENDER_DEPLOY.md](RENDER_DEPLOY.md) for comprehensive guide.

## Need Help?

- Render Docs: https://render.com/docs
- Render Status: https://status.render.com
- Project README: [README.md](README.md)
