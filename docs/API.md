# Nawala Gateway - API Reference

## Base URLs

- **Platform API:** `http://localhost:8080/api`
- **Gateway Proxy:** `http://localhost:8081/gw`
- **Admin API:** `http://localhost:8081/admin/api`

## Authentication

### API Key
```bash
curl -H "X-API-Key: nwl_your_api_key" http://localhost:8081/gw/your-route
```

### OAuth2 Bearer Token
```bash
curl -H "Authorization: Bearer your_token" http://localhost:8081/gw/your-route
```

---

## Gateway Proxy Endpoints

### Proxy Request
```
[METHOD] /gw/{path}
```

All requests to `/gw/*` are proxied to configured backend routes.

**Headers:**
- `X-API-Key` - API key authentication
- `Authorization: Bearer {token}` - OAuth token
- `X-API-Version` - API version (e.g., `v1`, `v2`)

**Response Headers:**
- `X-Gateway-Request-Id` - Unique request ID
- `X-Gateway-Response-Time` - Processing time in ms
- `X-RateLimit-Limit` - Rate limit ceiling
- `X-RateLimit-Remaining` - Remaining requests
- `X-RateLimit-Reset` - Seconds until reset

---

## Admin API Endpoints

### Routes Management

#### Refresh Routes
```bash
POST /admin/api/routes/refresh

# Response
{"status": "refreshed"}
```

### Service Discovery

#### List Services
```bash
GET /admin/api/services

# Response
["user-service", "order-service"]
```

#### Get Service Instances
```bash
GET /admin/api/services/{name}

# Response
[
  {"id": 1, "url": "http://user-1:8080", "healthy": true},
  {"id": 2, "url": "http://user-2:8080", "healthy": true}
]
```

#### Register Service
```bash
POST /admin/api/services/register?name=my-service&url=http://localhost:3000

# Response
{"status": "registered"}
```

#### Heartbeat
```bash
POST /admin/api/services/heartbeat?name=my-service&url=http://localhost:3000

# Response
{"status": "ok"}
```

### Circuit Breaker

#### Get Status
```bash
GET /admin/api/circuit-breaker/{target}

# Response
{"target": "http://backend:8080", "status": "CLOSED"}
```

#### Reset Circuit
```bash
POST /admin/api/circuit-breaker/{target}/reset

# Response
{"status": "reset"}
```

### Cache Management

#### Get Stats
```bash
GET /admin/api/cache/stats

# Response
{"size": 150}
```

#### Clear Cache
```bash
POST /admin/api/cache/clear

# Response
{"status": "cleared"}
```

### WAF Management

#### Get Blocked IPs
```bash
GET /admin/api/waf/blocked-ips

# Response
["10.0.0.1", "192.168.1.100"]
```

#### Block IP
```bash
POST /admin/api/waf/block-ip?ip=10.0.0.1

# Response
{"status": "blocked"}
```

#### Unblock IP
```bash
POST /admin/api/waf/unblock-ip?ip=10.0.0.1

# Response
{"status": "unblocked"}
```

### Analytics

#### Get Real-time Stats
```bash
GET /admin/api/stats

# Response
{
  "totalRequests": 15420,
  "totalErrors": 23,
  "avgLatencyMs": 45,
  "successRate": 99.85
}
```

---

## Error Responses

### Standard Error Format
```json
{
  "error": "Error message",
  "requestId": "uuid"
}
```

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 400 | Bad Request |
| 401 | Unauthorized - Invalid or missing auth |
| 403 | Forbidden - WAF blocked or insufficient permissions |
| 404 | Not Found - No matching route |
| 429 | Too Many Requests - Rate limit exceeded |
| 502 | Bad Gateway - Backend error |
| 503 | Service Unavailable - Circuit breaker open |

---

## Rate Limiting

Rate limits are applied per client (IP or API key) per route.

**Response when limited:**
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 45
Retry-After: 45

{"error": "Rate limit exceeded"}
```

---

## Load Balancing

Supported strategies:
- `ROUND_ROBIN` - Distribute evenly (default)
- `RANDOM` - Random selection
- `WEIGHTED` - Based on target weights
- `LEAST_CONNECTIONS` - Least active connections
- `IP_HASH` - Sticky sessions by client IP
