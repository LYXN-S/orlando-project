# Docker Deployment to Railway

## Prerequisites
- Docker installed and running
- Railway account
- Railway CLI installed

## Step 1: Install Railway CLI

### Windows (PowerShell):
```powershell
iwr https://railway.app/install.ps1 | iex
```

### Alternative (npm):
```bash
npm install -g @railway/cli
```

## Step 2: Login to Railway

```bash
railway login
```

This will open a browser window for authentication.

## Step 3: Create or Link Railway Project

### Create new project:
```bash
cd orlando-project
railway init
```

### Or link to existing project:
```bash
railway link
```

## Step 4: Build Docker Image Locally

```bash
docker build -t orlando-backend:latest .
```

Test locally (optional):
```bash
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://ballast.proxy.rlwy.net:31938/railway \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=aAtRoASEzVkxkxpPrwHWeOTTOJlPvcNF \
  orlando-backend:latest
```

## Step 5: Deploy to Railway

Railway CLI will build and deploy your Docker image:

```bash
railway up
```

This command will:
- Build your Docker image
- Push it to Railway's registry
- Deploy the container
- Show deployment logs

## Step 6: Set Environment Variables

```bash
# Link to your PostgreSQL database
railway variables set DATABASE_URL='jdbc:postgresql://${{PGHOST}}:${{PGPORT}}/${{PGDATABASE}}'
railway variables set DATABASE_USERNAME='${{PGUSER}}'
railway variables set DATABASE_PASSWORD='${{PGPASSWORD}}'
```

Or set them in Railway dashboard:
1. Go to your project
2. Click on your service
3. Go to "Variables" tab
4. Add variables or link PostgreSQL service

## Step 7: Get Your Deployment URL

```bash
railway domain
```

Or check in Railway dashboard under "Settings" → "Domains"

## Alternative: Push to Docker Hub then Deploy

If you prefer using Docker Hub:

### 1. Login to Docker Hub:
```bash
docker login
```

### 2. Tag your image:
```bash
docker tag orlando-backend:latest YOUR_DOCKERHUB_USERNAME/orlando-backend:latest
```

### 3. Push to Docker Hub:
```bash
docker push YOUR_DOCKERHUB_USERNAME/orlando-backend:latest
```

### 4. Deploy from Docker Hub in Railway:
- Go to Railway dashboard
- Create new service
- Select "Docker Image"
- Enter: `YOUR_DOCKERHUB_USERNAME/orlando-backend:latest`
- Set environment variables
- Deploy

## Useful Commands

### View logs:
```bash
railway logs
```

### Open Railway dashboard:
```bash
railway open
```

### Check service status:
```bash
railway status
```

### Redeploy:
```bash
railway up --detach
```

## Troubleshooting

### Build fails:
- Ensure Docker is running
- Check Dockerfile syntax
- Verify all files are present

### Deployment fails:
- Check Railway logs: `railway logs`
- Verify environment variables are set
- Ensure PostgreSQL service is running

### Can't connect to database:
- Use internal Railway URL: `postgres.railway.internal:5432`
- Or use Railway's provided variables: `${{PGHOST}}`, `${{PGPORT}}`, etc.
- Make sure services are in the same project

## Notes

- Railway CLI automatically handles image building and pushing
- No need to manually push to a registry when using `railway up`
- Environment variables can reference other services using `${{VARIABLE}}`
- Railway provides automatic HTTPS and domain management
