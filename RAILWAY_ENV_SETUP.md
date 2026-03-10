# Railway Environment Variables Setup

## Required Database Variables

Railway's PostgreSQL plugin automatically provides `DATABASE_URL`, but Spring Boot needs individual components. Set these in your Railway service:

### Option 1: Use Railway's Reference Variables (Recommended)

In Railway dashboard, add these variables to your backend service:

```
PGHOST=${{Postgres.PGHOST}}
PGPORT=${{Postgres.PGPORT}}
PGDATABASE=${{Postgres.PGDATABASE}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}
```

### Option 2: Manual Configuration

If Railway doesn't provide the PG* variables, use these:

```
PGHOST=postgres.railway.internal
PGPORT=5432
PGDATABASE=railway
PGUSER=postgres
PGPASSWORD=<your-postgres-password>
```

## Other Required Variables

```
ALLOWED_ORIGINS=https://your-frontend-url.com,https://another-frontend.com
PORT=8080
```

## How to Set Variables in Railway

1. Go to your Railway project
2. Click on your backend service
3. Go to the "Variables" tab
4. Click "New Variable"
5. Add each variable with its value
6. Railway will automatically redeploy

## Verify Variables

After setting variables, check the deployment logs to ensure the app connects to the database successfully.
