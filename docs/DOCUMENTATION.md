# Nawala API Gateway Platform - Complete Documentation

> **Nawala** is an API Gateway management platform that provides routing, security, monitoring, and comprehensive API management features.

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Installation & Setup](#installation--setup)
3. [Authentication & User Management](#authentication--user-management)
4. [API Routes Management](#api-routes-management)
5. [API Key Management](#api-key-management)
6. [OAuth2 Client Credentials](#oauth2-client-credentials)
7. [Rate Limiting & Tiers](#rate-limiting--tiers)
8. [WAF (Web Application Firewall)](#waf-web-application-firewall)
9. [Threat Detection & Blocking](#threat-detection--blocking)
10. [Circuit Breaker](#circuit-breaker)
11. [Load Balancer](#load-balancer)
12. [URL Masking](#url-masking)
13. [Payload Encryption](#payload-encryption)
14. [Response Caching](#response-caching)
15. [Request/Response Transformation](#requestresponse-transformation)
16. [Webhooks](#webhooks)
17. [Mock Endpoints](#mock-endpoints)
18. [Plugins (Custom Scripts)](#plugins-custom-scripts)
19. [API Documentation](#api-documentation)
20. [Analytics & Monitoring](#analytics--monitoring)
21. [Health Monitoring](#health-monitoring)
22. [Audit Logging](#audit-logging)
23. [Admin Panel](#admin-panel)
24. [Deployment](#deployment)
25. [Troubleshooting](#troubleshooting)
26. [Internal API Reference](#internal-api-reference)

---

## System Architecture

```
┌──────────────────────────────────────────────────────────┐
│                       CLIENT                              │
└──────────────────────┬────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│              NAWALA GATEWAY (port 9090)                    │
│  Filters: WAF → ThreatBlock → URLMask → RateLimit →      │
│  ApiKeyAuth → JwtAuth → Cache → LoadBalancer →            │
│  CircuitBreaker → Transformation → PayloadEncrypt →       │
│  AnalyticsRecorder → AnomalyRecorder                      │
└──────────────────────┬────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│             NAWALA PLATFORM (port 8080)                    │
│  Web UI (Thymeleaf) + REST API + Internal API             │
│  Database: MySQL | Cache: Redis | Logging: Logback        │
└──────────────────────────────────────────────────────────┘
```

| Component | Port | Description |
|-----------|------|-------------|
| Platform | 8080 | Web UI + Management REST API |
| Gateway | 9090 | API Gateway with filter pipeline |
| MySQL | 3306 | Primary database |
| Redis | 6379 | Cache & rate limiting (optional for local) |

---

## Installation & Setup

### Prerequisites
- Java 17 (OpenJDK)
- MySQL 8.x
- Redis (optional for local development)
- Maven 3.9+

### Database Setup

```sql
CREATE DATABASE nawala_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nawala'@'localhost' IDENTIFIED BY 'YourPassword123';
GRANT ALL PRIVILEGES ON nawala_db.* TO 'nawala'@'localhost';
FLUSH PRIVILEGES;
```

### Platform Configuration (`platform/src/main/resources/application.properties`)
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/nawala_db?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
nawala.encryption.key=ieUfr//mm5I3ZRW39jagiPW9KG8SOiPgvQZULD00LW4=
```

### Gateway Configuration (`gateway/src/main/resources/application.properties`)
```properties
server.port=9090
spring.data.redis.host=localhost
spring.data.redis.port=6379
nawala.gateway.jwt.secret=TmF3YWxhR2F0ZXdheVNlY3JldEtleUZvckpXVFRva2VuU2lnbjEyMzQ=
nawala.gateway.platform-url=http://localhost:8080
nawala.gateway.internal-secret=NawalaInternalSecretKey2024!
nawala.gateway.payload-key=ieUfr//mm5I3ZRW39jagiPW9KG8SOiPgvQZULD00LW4=
```

### Build
```bat
cd D:\nawala
build-platform.bat

## Authentication & User Management

### Login
- **URL:** `/login`
- **Default Admin:** `admin` / `admin123`
- Session timeout: 30 minutes, HTTP-only cookie

### Register
- **URL:** `/register`
- Fields: Username, Email, Full Name, Password, Confirm Password
- New users are automatically assigned the `USER` role

### Profile
- **URL:** `/profile`
- Edit name, email, change password (requires current password)

### Roles

| Role | Access |
|------|--------|
| `ADMIN` | All features + admin panel |
| `USER` | Dashboard, routes, API keys, advanced features |

### Security
- Password: BCrypt hashed
- Email & name: Encrypted (AES-256) in database
- Session: HTTP-only cookie, 30-minute timeout
- Login attempts: logged to audit

---

## API Routes Management

### Access
- **URL:** `/routes`

### Creating a New Route
1. Click **New Route** → fill in the form:
   - **Name:** Route name (e.g., "User Service")
   - **Method:** GET / POST / PUT / DELETE / PATCH
   - **Path:** Public path (e.g., `/api/v1/users`)
   - **Target URL:** Backend URL (e.g., `http://internal:3000/users`)
   - **Auth Required:** Check if API Key/JWT is required
   - **Rate Limit Enabled:** Enable + set requests per minute
2. Click **Save**

### Example Request via Gateway
```bash
# Public route
curl http://localhost:9090/api/v1/public/data

# With API Key
curl -H "X-API-Key: nwl_xxxx" http://localhost:9090/api/v1/users

# With JWT
curl -H "Authorization: Bearer <token>" http://localhost:9090/api/v1/users
```

### Actions
| Action | Description |
|--------|-------------|
| Edit | Modify route configuration |
| Toggle | Enable/disable route |
| Delete | Permanently remove route |

### Route Properties
- `maskedPath` — Public URL (URL Masking)
- `targetUrl` — Actual backend URL
- `authRequired` — Requires API Key/JWT
- `rateLimitEnabled` + `rateLimitPerMinute` — Rate limit settings
- `payloadEncryption` — E2E payload encryption
- `healthStatus` — Health check status

---

## API Key Management

### Access
- **URL:** `/api-keys`

### Generate API Key
1. Fill in the form:
   - **Name:** Key label (e.g., "Mobile App Production")
   - **Expiration Days:** Validity period (blank = no expiry)
   - **Daily Quota:** Daily limit (0 = unlimited)
   - **Monthly Quota:** Monthly limit (0 = unlimited)
   - **Allowed IPs:** IP whitelist (comma-separated, blank = all)
   - **Allowed Routes:** Route path whitelist (blank = all)
   - **Allowed Methods:** Method whitelist (blank = all)
2. Click **Generate Key**
3. **IMPORTANT:** Copy the displayed key — it cannot be viewed again!

### Key Format
```
nwl_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Usage
```bash
curl -H "X-API-Key: nwl_your_key_here" http://localhost:9090/api/v1/resource
```

### Rotate Key
- Generates a new key; the old key remains valid for **24 hours** (grace period)
- Use for rotation without downtime

### Revoke Key
- Key is immediately invalidated; cannot be undone

---

## OAuth2 Client Credentials

### Access UI
- **URL:** `/oauth-clients`

### Register Client
1. Fill in: Name, Grant Types (`client_credentials`), Scopes (`read,write`), Redirect URIs
2. Copy the `client_id` and `client_secret`

### Obtain Token
```bash
curl -X POST http://localhost:8080/oauth/token \
  -d "grant_type=client_credentials" \
  -d "client_id=YOUR_CLIENT_ID" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "scope=read write"
```
Response:
```json
{"access_token":"xxx","token_type":"Bearer","expires_in":3600,"refresh_token":"yyy","scope":"read write"}
```

### Refresh Token

## WAF (Web Application Firewall)

### Access
- **URL:** `/waf`

### Built-in Protection (Automatically Active)

| Attack | Patterns Blocked |
|--------|-----------------|
| SQL Injection | `UNION SELECT`, `DROP TABLE`, `' OR 1=1`, `; --` |
| XSS | `<script>`, `javascript:`, `onerror=`, `alert()` |
| Path Traversal | `../`, `..\\`, `%2e%2e` |

### Custom WAF Rule
1. Fill in: Name, Rule Type (`REGEX`/`EXACT`/`CONTAINS`), Pattern, Action (`BLOCK`/`LOG`/`CHALLENGE`), Target Field (`HEADER`/`BODY`/`QUERY`/`PATH`), Route ID (optional), Priority
2. Click **Create Rule**

### Response When Blocked
```json
{"error":"Blocked by WAF","reason":"SQL_INJECTION"}
```
HTTP `403` + Header `X-WAF-Block: SQL_INJECTION`

---

## Threat Detection & Blocking

### Access
- **URL:** `/threats`

### How It Works
1. Gateway records every request to the anomaly detection system
2. Platform analyzes patterns (traffic spikes, excessive errors, brute-force)
3. Anomalous sources are automatically blocked
4. `ThreatBlockFilter` checks every incoming request

### Dashboard
- Unresolved Threats, Critical Threats, Blocked Sources, Threats 24h

### Resolve Threat
- Click **Resolve** on events that have been handled

### Response When Blocked
```json
{"error":"Forbidden","message":"Your access has been temporarily blocked due to suspicious activity."}
```

---

## Circuit Breaker

### How It Works

| State | Behavior |
|-------|----------|
| CLOSED | Requests forwarded normally |
| OPEN | Requests rejected with 503 without calling backend |
| HALF-OPEN | Some requests allowed through for testing |

### Transitions
- CLOSED → (failure threshold exceeded) → OPEN
- OPEN → (timeout elapsed) → HALF-OPEN
- HALF-OPEN → (success) → CLOSED / (failure) → OPEN

### Response When Open
```
HTTP 503 Service Unavailable
Header: X-Circuit-State: OPEN
```

Backend returning 5xx = failure recorded. 2xx/3xx/4xx = success recorded.

---

## Load Balancer

### How It Works
- Distributes traffic to multiple backend targets per route
- Strategy: **Round-Robin** and **Canary**
- Health-aware: only sends to healthy targets

### Setup Multi-Target
Routes support multiple targets via `RouteTarget`:
- **URL:** Backend target URL
- **Weight:** Distribution weight (1-100)
- **Healthy:** Health status
- **Canary:** Mark as canary deployment
- **Canary Percentage:** % traffic to canary (e.g., 5%)

### Canary Deployment
```
95% traffic → Production (v1.0)
 5% traffic → Canary (v1.1)
```
Gradually increase the percentage if canary is stable.

---

## URL Masking

### Purpose
Hides the actual backend URL from clients.

### Example

| Client Request | Masked Path | Real Backend |
|----------------|-------------|--------------|
| `GET /public/v1/users` | `/public/v1/users/**` | `http://internal:3000/api/users` |

### Setup
When creating a route, fill in the **Masked Path** field. Clients call the masked path, and the gateway rewrites it to the actual path + target URL.

### Benefits
- Backend URLs are not exposed to clients
- Backend can be changed without changing the public URL
- Abstraction layer for versioning

---

## Payload Encryption


## Request/Response Transformation

### Transformation Types

| Type | Phase | Description |
|------|-------|-------------|
| ADD_HEADER | REQUEST | Add header to request |
| REMOVE_HEADER | REQUEST | Remove header from request |
| ADD_HEADER | RESPONSE | Add header to response |
| REMOVE_HEADER | RESPONSE | Remove header from response |

### Example
Add a tracking header to all requests:
```
Phase: REQUEST | Type: ADD_HEADER | Key: X-Request-Source | Value: gateway
```

---

## Webhooks

### Access
- **URL:** `/webhooks`

### Creating a Webhook
1. Fill in:
   - **Name:** Label (e.g., "Slack Notification")
   - **Target URL:** Destination POST URL (e.g., `https://hooks.slack.com/xxx`)
   - **Event Type:** Trigger event:
     - `route.created` / `route.updated` / `route.deleted`
     - `key.generated` / `key.revoked`
     - `threat.detected` / `health.down`
   - **Secret:** HMAC secret for verification (optional)
2. Click **Create**

### Signature Verification
If secret is set, webhook sends header:
```
X-Webhook-Signature: sha256=HMAC_SHA256(payload, secret)
```

---

## Mock Endpoints

### Access
- **URL:** `/mocks`

### Creating a Mock
1. Fill in:
   - **Name:** Mock label
   - **Path:** Endpoint path (e.g., `/mock/v1/users`)
   - **Method:** GET / POST / PUT / DELETE
   - **Status Code:** HTTP status (e.g., 200)
   - **Response Body:** JSON/text response
   - **Content Type:** `application/json` (default)
   - **Delay (ms):** Latency simulation (0 = instant)
2. Click **Create**

### Access Mock via Gateway
```bash
curl http://localhost:9090/mock/v1/users
```
Gateway `MockEndpointFilter` intercepts and returns mock response without hitting backend.

---

## Plugins (Custom Scripts)

### Access
- **URL:** `/plugins`

### Creating a Plugin
1. Fill in:
   - **Name:** Plugin name
   - **Description:** Function description
   - **Hook Type:**
     - `PRE_REQUEST` — Before forwarding to backend
     - `POST_REQUEST` — After response from backend

## Analytics & Monitoring

### Access
- **URL:** `/analytics`

### Dashboard (Last 24 Hours)
- **Total Requests** — Request count
- **Average Response Time** — Average response time in ms
- **Error Rate** — Error percentage
- **Top Routes** — 10 most accessed routes
- **Status Distribution** — HTTP status code distribution (chart)
- **Hourly Traffic** — Traffic chart per hour
- **Geo Distribution** — Geographic distribution

### Recorded Data
Every request through the gateway is recorded: Route ID, API Key prefix, Source IP, Method, Path, Status code, Response time, Request/Response size.

---

## Health Monitoring

### Access
- **URL:** `/health`

### Status

| Status | Description |
|--------|-------------|
| UP | Backend responding normally |
| DOWN | Backend not responding |
| DEGRADED | Backend slow/intermittent errors |

Platform performs periodic health checks to all active backend routes.

---

## Audit Logging

### Access
- **URL:** `/admin/audit` (Admin only)

### What is Logged
- CREATE: Key generated, Route created
- ROTATE: Key rotated
- REVOKE: Key revoked
- UPDATE: Route/Profile updated
- DELETE: Route deleted
- LOGIN: Login success/failure

Info: User ID, Username, Action, Entity type & ID, Details, IP, Timestamp.

---

## Admin Panel

### Access
- **URL:** `/admin` (ADMIN role only)

### Features

| Menu | URL | Function |
|------|-----|----------|
| Dashboard | `/admin` | Overview users, tiers, recent audit |
| Users | `/admin/users` | Manage users, enable/disable accounts |
| Audit | `/admin/audit` | 200 most recent entries of all actions |
| Tiers | `/admin/tiers` | CRUD rate limit tiers |

---

## Deployment

### Deploy to Server (Production - Ubuntu)

```bash
# 1. Install dependencies
sudo apt-get install -y openjdk-17-jdk mysql-server redis-server nginx

# 2. Setup folders
sudo mkdir -p /opt/nawala/{logs/platform,logs/gateway,config,backup}
sudo chown -R $(whoami):$(whoami) /opt/nawala

# 3. Upload JAR from Windows
scp -i key.pem platform/target/nawala-platform-1.0.0.jar ubuntu@YOUR_IP:/opt/nawala/
scp -i key.pem gateway/target/nawala-gateway-1.0.0.jar ubuntu@YOUR_IP:/opt/nawala/

# 4. Start services
sudo systemctl start nawala-platform
sudo systemctl start nawala-gateway
```

### Deploy Local (Windows)
```bat
build-platform.bat     :: Build
deploy-local.bat       :: Start
stop-local.bat         :: Stop
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 8080 already in use | `netstat -aon \| findstr :8080` then `taskkill /F /PID <PID>` |
| DB connection error | Check MySQL is running, verify credentials in properties |
| Gateway can't connect to Platform | Ensure Platform is running, check `platform-url` and `internal-secret` |
| Build error | Check JAVA_HOME points to Java 17, use `settings.xml` |
| Login error 500 | Check MySQL, ensure `ddl-auto=update`, check `platform-err.log` |

---

## Internal API Reference

Endpoints used for Gateway ↔ Platform communication:

| Method | Endpoint | Function |
|--------|----------|----------|
| GET | `/internal/routes` | Fetch active routes |
| POST | `/internal/keys/validate` | Validate API Key |
| POST | `/internal/oauth/validate` | Validate OAuth Token |
| POST | `/internal/analytics/record` | Record analytics |
| GET | `/internal/waf/rules` | Fetch WAF rules |
| GET | `/internal/plugins/{hookType}` | Fetch plugins |
| POST | `/internal/anomaly/record` | Record anomaly |
| POST | `/internal/anomaly/check-block` | Check block status |
| GET | `/internal/health/summary` | Health summary |
| GET | `/internal/health/routes` | Health per route |
| GET | `/internal/rate-tiers` | Rate limit tiers |

Security header: `X-Internal-Secret: NawalaInternalSecretKey2024!`

---

*Nawala API Gateway Platform v1.0.0 — July 2026*

     - `PRE_RESPONSE` — Before sending to client
     - `ON_ERROR` — When an error occurs
   - **Script:** Logic code
   - **Route ID:** Optional (blank = all routes)
   - **Priority:** Execution order (lower = runs first)
2. Click **Create**

---

## API Documentation

### Access
- **Private:** `/docs` (requires login)
- **Public:** `/docs/public` (no login required)

### Creating Documentation
1. Fill in: Title, Route ID (optional), Version, OpenAPI Spec (YAML/JSON), Description
2. Click **Create**

### Publish/Unpublish
- **Publish:** Visible at `/docs/public`
- **Unpublish:** Only visible to logged-in users

---

### Purpose
End-to-end payload encryption using **AES-256-GCM**.

### How It Works
1. Client encrypts body: `key = SHA-256(apiKey + ":" + masterKey)`
2. Client sends with header `X-Payload-Encrypted: true`
3. Gateway decrypts before forwarding to backend
4. Payload format: `Base64(IV[12bytes] + Ciphertext)`

### Activation
1. Edit route → check **Payload Encryption**
2. Client sends header `X-Payload-Encrypted: true`
3. Client must encrypt body with AES-256-GCM

### Example Client (pseudocode)
```javascript
const key = sha256(apiKey + ":" + masterKey);
const iv = crypto.randomBytes(12);
const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
const encrypted = Buffer.concat([iv, cipher.update(body), cipher.final(), cipher.getAuthTag()]);
const payload = encrypted.toString('base64');
// Send payload as request body
```

---

## Response Caching

### How It Works
- Only **GET** requests are cached
- Cache key: `path + query string`
- TTL: 60 seconds (default)
- Max entries: 1000 (auto eviction)

### Cache Headers
- `X-Cache: HIT` — response from cache
- `X-Cache: MISS` — response from backend

### Bypass Cache
```
Cache-Control: no-cache
```

---

```bash
curl -X POST http://localhost:8080/oauth/token \
  -d "grant_type=refresh_token" -d "refresh_token=YOUR_REFRESH_TOKEN"
```

### Introspect (Validate)
```bash
curl "http://localhost:8080/oauth/introspect?token=YOUR_TOKEN"
```

### Revoke Token
```bash
curl -X POST http://localhost:8080/oauth/revoke -d "token=YOUR_TOKEN"
```

### Use with Gateway
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" http://localhost:9090/api/v1/resource
```

---

## Rate Limiting & Tiers

### How It Works
- Gateway applies rate limits per client (IP or API Key)
- Default: **60 requests/minute**, 1-minute window
- Response headers: `X-RateLimit-Limit` and `X-RateLimit-Remaining`

### When Limit is Reached
```json
{"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}
```
HTTP Status: `429`

### Creating a Tier (Admin Only)
- **URL:** `/admin/tiers`
- Fields: Name, Requests Per Minute/Hour/Day, Burst Size, Description

### Example Tiers

| Tier | /min | /hour | /day | Burst |
|------|------|-------|------|-------|
| FREE | 20 | 500 | 5000 | 5 |
| STANDARD | 60 | 2000 | 30000 | 10 |
| PREMIUM | 200 | 10000 | 100000 | 30 |
| ENTERPRISE | 1000 | 50000 | 500000 | 100 |

---

```

### Deploy & Run Local
```bat
deploy-local.bat       REM Start both services
stop-local.bat         REM Stop both services
```

Access: http://localhost:8080 | Login: `admin` / `admin123`

---
