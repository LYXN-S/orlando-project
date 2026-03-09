# Railway Docker Deployment Script for Windows PowerShell

Write-Host "Orlando Backend - Railway Deployment Script" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "[1/6] Checking Docker..." -ForegroundColor Yellow
docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker is not running. Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Docker is running" -ForegroundColor Green
Write-Host ""

# Check if Railway CLI is installed
Write-Host "[2/6] Checking Railway CLI..." -ForegroundColor Yellow
railway --version > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: Railway CLI is not installed." -ForegroundColor Red
    Write-Host "Installing Railway CLI..." -ForegroundColor Yellow
    iwr https://railway.app/install.ps1 | iex
    
    # Verify installation
    railway --version > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to install Railway CLI. Please install manually:" -ForegroundColor Red
        Write-Host "   npm install -g @railway/cli" -ForegroundColor Yellow
        exit 1
    }
}
Write-Host "SUCCESS: Railway CLI is installed" -ForegroundColor Green
Write-Host ""

# Build Docker image
Write-Host "[3/6] Building Docker image..." -ForegroundColor Yellow
Write-Host "This may take several minutes on first build..." -ForegroundColor Gray
docker build -t orlando-backend:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Docker image built successfully" -ForegroundColor Green
Write-Host ""

# Check if logged in to Railway
Write-Host "[4/6] Checking Railway authentication..." -ForegroundColor Yellow
railway whoami > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: Not logged in to Railway. Opening login..." -ForegroundColor Yellow
    railway login
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Railway login failed!" -ForegroundColor Red
        exit 1
    }
}
Write-Host "SUCCESS: Authenticated with Railway" -ForegroundColor Green
Write-Host ""

# Check if project is linked
Write-Host "[5/6] Checking Railway project..." -ForegroundColor Yellow
railway status > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: No Railway project linked." -ForegroundColor Yellow
    Write-Host "Please choose an option:" -ForegroundColor Cyan
    Write-Host "  1. Link to existing project" -ForegroundColor White
    Write-Host "  2. Create new project" -ForegroundColor White
    $choice = Read-Host "Enter choice (1 or 2)"
    
    if ($choice -eq "1") {
        railway link
    } elseif ($choice -eq "2") {
        railway init
    } else {
        Write-Host "ERROR: Invalid choice!" -ForegroundColor Red
        exit 1
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to setup Railway project!" -ForegroundColor Red
        exit 1
    }
}
Write-Host "SUCCESS: Railway project linked" -ForegroundColor Green
Write-Host ""

# Deploy to Railway
Write-Host "[6/6] Deploying to Railway..." -ForegroundColor Yellow
Write-Host "This may take a few minutes..." -ForegroundColor Gray
railway up

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Deployment failed!" -ForegroundColor Red
    Write-Host "Check logs with: railway logs" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "SUCCESS: Deployment completed!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Set environment variables in Railway dashboard" -ForegroundColor White
Write-Host "  2. Link PostgreSQL service or set DATABASE_URL" -ForegroundColor White
Write-Host "  3. Get your deployment URL: railway domain" -ForegroundColor White
Write-Host "  4. View logs: railway logs" -ForegroundColor White
Write-Host ""
Write-Host "Opening Railway dashboard..." -ForegroundColor Yellow
railway open
