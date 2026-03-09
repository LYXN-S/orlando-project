# Quick Docker Deployment Guide

## 🚀 Fast Track Deployment

### Option 1: Use Deployment Script (Easiest)

**PowerShell:**
```powershell
cd orlando-project
.\deploy.ps1
```

**Git Bash:**
```bash
cd orlando-project
chmod +x deploy.sh
./deploy.sh
```

The script will:
- ✅ Check Docker is running
- ✅ Install Railway CLI if needed
- ✅ Build Docker image
- ✅ Login to Railway
- ✅ Link/create project
- ✅ Deploy your app

---

### Option 2: Manual Steps

#### 1. Install Railway CLI

**PowerShell:**
```powershell
iwr https://railway.app/install.ps1 | iex
```

**Or via npm:**
```bash
npm install -g @railway/cli
```

#### 2. Build Docker Image
```bash
cd orlando-project
docker build -t orlando-backend:latest .
```

#### 3. Login to Railway
```bash
railway login
```

#### 4. Initialize/Link Project
```bash
# For new project:
railway init

# For existing project:
railway link
```

#### 5. Deploy
```bash
railway up
```

---

## ⚙️ Configure Environment Variables

After deployment, set these in Railway dashboard:

1. Go to your project in Railway
2. Click on your backend service
3. Go to "Variables" tab
4. Add these variables:

```
DATABASE_URL=jdbc:postgresql://${{PGHOST}}:${{PGPORT}}/${{PGDATABASE}}
DATABASE_USERNAME=${{PGUSER}}
DATABASE_PASSWORD=${{PGPASSWORD}}
```

**Or link PostgreSQL service:**
- Click "New Variable" → "Add Reference"
- Select your PostgreSQL service
- Railway will auto-inject all database variables

---

## 🔍 Verify Deployment

### Get your app URL:
```bash
railway domain
```

### Check health endpoint:
```
https://your-app.railway.app/actuator/health
```

### View logs:
```bash
railway logs
```

### Check status:
```bash
railway status
```

---

## 🔄 Redeploy After Changes

```bash
# Rebuild Docker image
docker build -t orlando-backend:latest .

# Deploy
railway up
```

---

## 📝 Update Frontend

Once deployed, update your frontend `.env`:

```env
REACT_APP_API_URL=https://your-app.railway.app/api/v1
```

Replace `your-app.railway.app` with your actual Railway domain.

---

## 🐛 Troubleshooting

### Docker build fails:
```bash
# Check Docker is running
docker info

# Clean build
docker build --no-cache -t orlando-backend:latest .
```

### Railway deployment fails:
```bash
# Check logs
railway logs

# Check status
railway status

# Verify environment variables
railway variables
```

### Database connection fails:
- Ensure PostgreSQL service is running in Railway
- Verify environment variables are set
- Check if services are in the same project
- Use internal URL: `postgres.railway.internal:5432`

---

## 📚 Additional Resources

- [Railway Documentation](https://docs.railway.app/)
- [Docker Documentation](https://docs.docker.com/)
- Full guide: See `DOCKER_DEPLOYMENT.md`
