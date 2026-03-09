# Railway Deployment Guide

## Prerequisites
- Railway account
- Railway PostgreSQL database already created
- Git repository connected to Railway

## Environment Variables to Set in Railway

Go to your Railway project → Backend service → Variables tab and add:

```
DATABASE_URL=jdbc:postgresql://postgres.railway.internal:5432/railway
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<your-railway-postgres-password>
```

**Important:** Railway provides these variables automatically from your PostgreSQL service:
- `PGHOST` → Use as host
- `PGPORT` → Use as port (usually 5432)
- `PGDATABASE` → Use as database name
- `PGUSER` → Use as username
- `PGPASSWORD` → Use as password

You can reference them like:
```
DATABASE_URL=jdbc:postgresql://${{PGHOST}}:${{PGPORT}}/${{PGDATABASE}}
DATABASE_USERNAME=${{PGUSER}}
DATABASE_PASSWORD=${{PGPASSWORD}}
```

## Deployment Steps

### Option 1: Deploy from GitHub (Recommended)

1. Push your code to GitHub:
   ```bash
   git add .
   git commit -m "Add Dockerfile for Railway deployment"
   git push origin main
   ```

2. In Railway:
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your repository
   - Railway will automatically detect the Dockerfile and build

3. Connect to PostgreSQL:
   - Click on your backend service
   - Go to "Variables" tab
   - Add the environment variables listed above
   - Or link the PostgreSQL service (Railway will auto-inject variables)

4. Deploy:
   - Railway will automatically deploy
   - Check logs for any errors
   - Your app will be available at the generated Railway URL

### Option 2: Deploy using Railway CLI

1. Install Railway CLI:
   ```bash
   npm install -g @railway/cli
   ```

2. Login:
   ```bash
   railway login
   ```

3. Link to project:
   ```bash
   railway link
   ```

4. Deploy:
   ```bash
   railway up
   ```

## Testing Locally with Docker

Build and run the Docker image locally:

```bash
# Build the image
docker build -t orlando-backend .

# Run the container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://ballast.proxy.rlwy.net:31938/railway \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=aAtRoASEzVkxkxpPrwHWeOTTOJlPvcNF \
  orlando-backend
```

## Health Check

Once deployed, verify the health endpoint:
```
https://your-railway-app.railway.app/actuator/health
```

## Troubleshooting

### Build fails
- Check Railway logs for Maven build errors
- Ensure Java 21 is specified in Dockerfile
- Verify all dependencies are in pom.xml

### Database connection fails
- Verify environment variables are set correctly
- Check if PostgreSQL service is running
- Ensure services are in the same Railway project (for internal networking)

### Application crashes on startup
- Check Railway logs
- Verify DATABASE_URL format is correct
- Ensure PostgreSQL is accessible

## Notes

- The Dockerfile uses multi-stage build to keep the final image small
- Health checks are configured for Railway's monitoring
- The application runs as a non-root user for security
- Port 8080 is exposed by default
