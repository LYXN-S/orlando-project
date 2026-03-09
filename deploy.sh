#!/bin/bash

# Railway Docker Deployment Script for Bash/Git Bash

echo "🚀 Orlando Backend - Railway Deployment Script"
echo "============================================="
echo ""

# Check if Docker is running
echo "📦 Checking Docker..."
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi
echo "✅ Docker is running"
echo ""

# Check if Railway CLI is installed
echo "🚂 Checking Railway CLI..."
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI is not installed."
    echo "📥 Installing Railway CLI via npm..."
    npm install -g @railway/cli
    
    # Verify installation
    if ! command -v railway &> /dev/null; then
        echo "❌ Failed to install Railway CLI."
        echo "Please install manually: npm install -g @railway/cli"
        exit 1
    fi
fi
echo "✅ Railway CLI is installed"
echo ""

# Build Docker image
echo "🔨 Building Docker image..."
if ! docker build -t orlando-backend:latest .; then
    echo "❌ Docker build failed!"
    exit 1
fi
echo "✅ Docker image built successfully"
echo ""

# Check if logged in to Railway
echo "🔐 Checking Railway authentication..."
if ! railway whoami > /dev/null 2>&1; then
    echo "⚠️  Not logged in to Railway. Opening login..."
    if ! railway login; then
        echo "❌ Railway login failed!"
        exit 1
    fi
fi
echo "✅ Authenticated with Railway"
echo ""

# Check if project is linked
echo "🔗 Checking Railway project..."
if ! railway status > /dev/null 2>&1; then
    echo "⚠️  No Railway project linked."
    echo "Please choose an option:"
    echo "  1. Link to existing project"
    echo "  2. Create new project"
    read -p "Enter choice (1 or 2): " choice
    
    if [ "$choice" = "1" ]; then
        railway link
    elif [ "$choice" = "2" ]; then
        railway init
    else
        echo "❌ Invalid choice!"
        exit 1
    fi
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to setup Railway project!"
        exit 1
    fi
fi
echo "✅ Railway project linked"
echo ""

# Deploy to Railway
echo "🚀 Deploying to Railway..."
echo "This may take a few minutes..."
railway up

if [ $? -ne 0 ]; then
    echo "❌ Deployment failed!"
    echo "Check logs with: railway logs"
    exit 1
fi

echo ""
echo "✅ Deployment successful!"
echo ""
echo "📋 Next steps:"
echo "  1. Set environment variables in Railway dashboard"
echo "  2. Link PostgreSQL service or set DATABASE_URL"
echo "  3. Get your deployment URL: railway domain"
echo "  4. View logs: railway logs"
echo ""
echo "🌐 Opening Railway dashboard..."
railway open
