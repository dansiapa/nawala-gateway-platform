# Nawala Gateway - Configuration Guide

## Platform Configuration

### application.properties

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/nawala
spring.datasource.username=nawala
spring.datasource.password=your_password

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Session
server.servlet.session.timeout=30m

# File Upload
spring.servlet.multipart.max-file-size=10MB
```

## Gateway Configuration

### application.yml

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nawala
    username: nawala
    password: your_password

gateway:
  proxy:
    connect-timeout: 5000
    read-timeout: 30000
    write-timeout: 30000
    max-memory-size: 16777216  # 16MB
  
  rate-limit:
    default-limit: 100
    window-seconds: 60
  
  cache:
    enabled: true
    default-ttl-seconds: 60
    max-size: 10000
  
  circuit-breaker:
    failure-threshold: 5
    open-duration-seconds: 30
    half-open-max-calls: 3
  
  health-check:
    interval-seconds: 30
    timeout-seconds: 5
```

## Route Configuration

### Basic Route

| Field | Description | Default |
|-------|-------------|--------|
| name | Route name | Required |
| method | HTTP method (GET, POST, etc.) or * for all | Required |
| path | URL pattern with wildcards | Required |
| target_url | Backend URL | Required |
| active | Enable/disable route | true |
| timeout_seconds | Request timeout | 30 |

### Path Patterns

```
/api/users          - Exact match
/api/users/*        - Single segment wildcard
/api/users/**       - Multi-segment wildcard
/api/users/{id}     - Path variable
```

### Authentication

| Field | Description | Default |
|-------|-------------|--------|
| auth_required | Require API key or token | false |

### Rate Limiting

| Field | Description | Default |
|-------|-------------|--------|
| rate_limit_enabled | Enable rate limiting | false |
| rate_limit_per_minute | Max requests/minute | 60 |

### Load Balancing

| Field | Description | Default |
|-------|-------------|--------|
| load_balanced | Enable load balancing | false |
| lb_strategy | ROUND_ROBIN, RANDOM, WEIGHTED, IP_HASH | ROUND_ROBIN |

## WAF Configuration

### Protection Features

| Feature | Description | Default |
|---------|-------------|--------|
| SQL Injection | Block SQL injection attempts | Enabled |
| XSS | Block cross-site scripting | Enabled |
| Path Traversal | Block directory traversal | Enabled |

### Custom Rules

Create custom WAF rules with:
- Pattern matching (regex)
- Actions: BLOCK, LOG, ALLOW
- Priority ordering

## API Versioning

### Supported Methods

1. **URL Path**: `/v1/users`, `/v2/users`
2. **Header**: `X-API-Version: v1`
3. **Accept Header**: `Accept: application/vnd.api.v1+json`

### Version Configuration

```yaml
# Enable versioning on route
versioned: true
default_version: v1
```

## Security Best Practices

### Production Checklist

- [ ] Change default admin password
- [ ] Use strong database password
- [ ] Enable HTTPS/TLS
- [ ] Configure firewall rules
- [ ] Enable rate limiting on all public routes
- [ ] Enable WAF protection
- [ ] Set up monitoring and alerting
- [ ] Regular backup of database
- [ ] Rotate API keys periodically

### Environment Variables for Production

```bash
export SPRING_DATASOURCE_PASSWORD=strong_random_password
export JWT_SECRET=your_256_bit_secret_key
export REDIS_PASSWORD=redis_password
```

## Logging

### Log Levels

```yaml
logging:
  level:
    root: WARN
    id.nawala: INFO
    id.nawala.gateway: DEBUG  # For detailed gateway logs
```

### Request Logging

Enable detailed request logging:
```bash
POST /admin/api/logging/detailed?enabled=true
```
