# Nawala Gateway Platform - Quick Start Guide

## Prerequisites

- Docker & Docker Compose
- 2GB RAM minimum
- Ports 5432, 6379, 8080, 8081 available

## Installation

### Option 1: Using Start Script (Recommended)

```bash
# Clone repository
git clone https://github.com/nawala-team/nawala-gateway-platform.git
cd nawala-gateway-platform

# Start all services
./start.sh start
```

### Option 2: Using Docker Compose Directly

```bash
docker-compose up -d
```

## Access

| Service | URL | Description |
|---------|-----|-------------|
| Platform | http://localhost:8080 | Admin Dashboard |
| Gateway | http://localhost:8081 | API Gateway |

## Default Credentials

- **Username:** admin
- **Password:** admin123

> ⚠️ Change the default password immediately after first login!

## First-Time Setup Wizard

1. Open http://localhost:8080
2. Login with default credentials
3. Complete the 4-step setup wizard:
   - **Step 1:** Database connection (pre-configured in Docker)
   - **Step 2:** Create admin account
   - **Step 3:** Configure gateway settings
   - **Step 4:** Create your first API route

## Quick Commands

```bash
# Start services
./start.sh start

# Stop services
./start.sh stop

# View logs
./start.sh logs

# View specific service logs
./start.sh logs platform
./start.sh logs gateway

# Check status
./start.sh status

# Rebuild after code changes
./start.sh build

# Remove everything (including data)
./start.sh clean
```

## Creating Your First API Route

1. Go to **Routes** in the sidebar
2. Click **New Route**
3. Fill in:
   - Name: `My API`
   - Method: `GET`
   - Path: `/api/hello`
   - Target URL: `https://httpbin.org/get`
4. Click **Save**

5. Test your route:
```bash
curl http://localhost:8081/gw/api/hello
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://postgres:5432/nawala | Database URL |
| SPRING_DATASOURCE_USERNAME | nawala | Database user |
| SPRING_DATASOURCE_PASSWORD | nawala_secret_2024 | Database password |
| SPRING_REDIS_HOST | redis | Redis host |
| SPRING_REDIS_PORT | 6379 | Redis port |

## Troubleshooting

### Services not starting
```bash
# Check logs
./start.sh logs

# Check if ports are in use
netstat -tlnp | grep -E '5432|6379|8080|8081'
```

### Database connection issues
```bash
# Check PostgreSQL
docker exec nawala-postgres pg_isready -U nawala
```

### Reset everything
```bash
./start.sh clean
./start.sh start
```

## Next Steps

- [API Documentation](./API.md)
- [Configuration Guide](./CONFIGURATION.md)
- [Security Best Practices](./SECURITY.md)
