<p align="center">
  <img src="https://img.shields.io/badge/Nawala-API%20Gateway-blueviolet?style=for-the-badge&logo=spring&logoColor=white" alt="Nawala"/>
</p>

<h1 align="center">🌐 Nawala — Enterprise API Gateway & Management Platform</h1>

<p align="center">
  <strong>Open-source API Gateway with built-in WAF, OAuth2, Rate Limiting, Anomaly Detection, and End-to-End Encryption.</strong><br/>
  Built with Java 17 + Spring Boot 3 + Spring Cloud Gateway.
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#security">Security</a> •
  <a href="#contributing">Contributing</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.1-brightgreen?style=flat-square&logo=spring" alt="Spring Cloud"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Redis-7.x-red?style=flat-square&logo=redis&logoColor=white" alt="Redis"/>
</p>

<p align="center">
  <a href="https://saweria.co/rdpf">
    <img src="https://img.shields.io/badge/☕%20Support%20This%20Project-Donate%20via%20Saweria-orange?style=for-the-badge" alt="Donate"/>
  </a>
</p>

---

## 💡 What is Nawala?

**Nawala** (from Sanskrit: "komunikasi / pesan") is a full-featured, production-ready **API Gateway** and **API Management Platform** designed for startups, enterprises, and developers who need secure, scalable, and intelligent API routing without vendor lock-in.

Unlike cloud-only solutions (AWS API Gateway, Apigee, Kong Enterprise), Nawala runs **entirely self-hosted** — giving you full control over your data, traffic, and security policies.

| Problem | Nawala Solution |
|---------|----------------|
| Cloud API Gateways are expensive at scale | Self-hosted, zero licensing cost |
| Complex multi-tool setups | All-in-one platform |
| No visibility into API anomalies | Built-in anomaly detection |
| Sensitive data exposed in transit | AES-256-GCM end-to-end payload encryption |
| Backend URLs exposed to clients | URL masking / path rewriting |
| No easy API lifecycle management | Full web console with MVVM architecture |

---
## ✨ Features

### 🔐 Security & Authentication
- **Multi-Auth Support** — API Key, JWT Bearer Token, OAuth2 Client Credentials
- **API Key Management** — Generate, rotate (24h grace period), revoke, scoped (IP/method/route)
- **OAuth2 Server** — Client registration, token issue/refresh/revoke/introspect (RFC 6749)
- **Web Application Firewall (WAF)** — SQL injection, XSS, path traversal + custom rules
- **AES-256-GCM Encryption** — Database field encryption + end-to-end payload encryption
- **Internal API Security** — Shared-secret header validation for service-to-service calls

### 🚦 Traffic Management
- **Tiered Rate Limiting** — FREE / STARTER / PROFESSIONAL / ENTERPRISE / UNLIMITED
- **Multi-Window Rate Limit** — Per-minute, per-hour, per-day sliding windows
- **Circuit Breaker** — CLOSED → OPEN → HALF_OPEN with configurable thresholds
- **Load Balancer** — Round-robin + canary routing with weighted distribution
- **Response Caching** — Configurable TTL per route
- **Request/Response Transformation** — Header injection, body transformation pipeline

### 🧠 Intelligence & Monitoring
- **Anomaly Detection** — Spike detection, brute force detection, unusual hour analysis
- **Auto-Block** — Threats automatically blocked at gateway level
- **Health Monitor** — Scheduled health checks with UP/DOWN/DEGRADED tracking
- **Real-time Analytics** — Traffic, response times, status distribution, geo, hourly patterns
- **Structured Logging** — JSON format, separated files (app/error/security/access/health)

### 🔌 Extensibility
- **Plugin System** — JavaScript hooks (PRE_REQUEST, POST_RESPONSE, ERROR_HANDLER, SCHEDULER)
- **Webhooks** — Event notifications with HMAC signing and exponential backoff retry
- **Mock/Sandbox** — Create mock endpoints for development and testing
- **API Documentation** — Built-in OpenAPI spec hosting with publish/unpublish workflow

### 🎯 Competitive Comparison

| Feature | Kong | AWS API GW | Apigee | **Nawala** |
|---------|:----:|:----------:|:------:|:----------:|
| Self-hosted + Free | ✅ | ❌ | ❌ | ✅ |
| Built-in WAF | ❌ | Separate | ❌ | ✅ |
| Anomaly Detection | ❌ | ❌ | ❌ | ✅ |
| Payload Encryption (E2E) | ❌ | ❌ | ❌ | ✅ |
| URL Masking | ❌ | ❌ | ❌ | ✅ |
| Plugin System (JS) | Lua | ❌ | ❌ | ✅ |
| Full Web Console | Paid | AWS Console | Paid | ✅ |
| Canary Routing | Paid | ❌ | Paid | ✅ |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         CLIENTS                              │
│              (Mobile / Web / IoT / Services)                  │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼ :9090
┌─────────────────────────────────────────────────────────────┐
│                    NAWALA GATEWAY                             │
│  ┌────────┐ ┌────────┐ ┌──────────┐ ┌────────────────────┐ │
│  │  WAF   │ │  Auth  │ │  Rate    │ │ Circuit Breaker    │ │
│  │ Filter │ │Filters │ │ Limiter  │ │ + Load Balancer    │ │
│  └────────┘ └────────┘ └──────────┘ └────────────────────┘ │
│  ┌────────┐ ┌────────┐ ┌──────────┐ ┌────────────────────┐ │
│  │URL Mask│ │Payload │ │  Cache   │ │ Analytics+Anomaly  │ │
│  │ Filter │ │Encrypt │ │  Filter  │ │ Recorder           │ │
│  └────────┘ └────────┘ └──────────┘ └────────────────────┘ │
└─────────────────────────────┬───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
       ┌────────────┐ ┌────────────┐ ┌────────────┐
       │ Backend  1 │ │ Backend  2 │ │ Backend  N │
       └────────────┘ └────────────┘ └────────────┘

              ┌───────────────┘
              ▼ :8080
┌─────────────────────────────────────────────────────────────┐
│                    NAWALA PLATFORM                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │          Web Console (Thymeleaf + Modern UI)           │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ Dashboard│Routes│API Keys│OAuth│Analytics│WAF│Plugins  │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────────┐   │
│  │ MySQL 8.0  │  │ Redis 7.x  │  │ Structured Logging  │   │
│  │(Encrypted) │  │(Rate Limit)│  │ (JSON, separated)   │   │
│  └────────────┘  └────────────┘  └─────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.2.5 |
| Gateway | Spring Cloud Gateway (Reactive/WebFlux) |
| Frontend | Thymeleaf + Custom CSS + Vanilla JS |
| Database | MySQL 8.0 (XAMPP compatible) |
| Cache | Redis 7.x |
| Security | Spring Security 6 + BCrypt(12) + AES-256-GCM |
| Build | Maven (Multi-module) |
| Logging | Logback + SLF4J (JSON structured) |
| Pattern | MVVM (Model-View-ViewModel) |

---

## 🚀 Quick Start

### Prerequisites

| Software | Version | Required |
|----------|---------|----------|
| Java JDK | 17+ | ✅ |
| Maven | 3.8+ | ✅ |
| MySQL | 8.0+ (or XAMPP) | ✅ |
| Redis | 7.x | ✅ |

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/nawala-api-gateway.git
cd nawala-api-gateway

# 2. Create MySQL database
mysql -u root -e "CREATE DATABASE nawala_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Start Redis
redis-server

# 4. Build the project
./mvnw clean package -DskipTests

# 5. Start Platform (port 8080)
java -jar platform/target/nawala-platform-1.0.0.jar

# 6. Start Gateway (port 9090) - separate terminal
java -jar gateway/target/nawala-gateway-1.0.0.jar
```

### Windows (XAMPP)

```powershell
# Start MySQL from XAMPP Control Panel, then:
& "C:\xampp\mysql\bin\mysql.exe" -u root -e "CREATE DATABASE nawala_db;"

# Build & Run
.\mvnw.cmd clean package -DskipTests
Start-Process java -ArgumentList "-jar","platform\target\nawala-platform-1.0.0.jar"
Start-Process java -ArgumentList "-jar","gateway\target\nawala-gateway-1.0.0.jar"
```

### Access Points

| Service | URL | Description |
|---------|-----|-------------|
| Platform Console | http://localhost:8080 | Web management UI |
## ⚙️ Configuration

### Platform (`platform/src/main/resources/application.properties`)

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/nawala_db?useSSL=false&serverTimezone=Asia/Jakarta
spring.datasource.username=root
spring.datasource.password=

# Encryption (generate your own 32-byte base64 key!)
nawala.encryption.key=YOUR_BASE64_AES_256_KEY_HERE

# Internal API security
nawala.internal.secret=YOUR_STRONG_SECRET_HERE
```

### Gateway (`gateway/src/main/resources/application.properties`)

```properties
nawala.gateway.platform-url=http://localhost:8080
nawala.gateway.internal-secret=YOUR_STRONG_SECRET_HERE
nawala.gateway.jwt.secret=YOUR_BASE64_JWT_SECRET
nawala.gateway.payload-key=YOUR_BASE64_AES_256_KEY_HERE
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Generate Secure Keys

```bash
# AES-256 key (32 bytes, base64)
openssl rand -base64 32

# JWT secret (64 bytes, base64)
openssl rand -base64 64

# Internal shared secret
openssl rand -hex 32
```

---

## 🔒 Security

### Encryption Layers

| Layer | Algorithm | Purpose |
|-------|-----------|---------|
| Database Fields | AES-256-GCM | Encrypt PII (email, phone, URLs) |
| Passwords | BCrypt (cost=12) | Password hashing |
| API Keys | BCrypt + SecureRandom | Key storage |
| Payload (E2E) | AES-256-GCM | Request/response body encryption |
| Sessions | JSESSIONID + HttpOnly | Session management |

### WAF Rules (Default)

| Rule | Pattern | Action |
|------|---------|--------|
| SQL Injection | `UNION SELECT`, `DROP TABLE`, `' OR '` | BLOCK (403) |
| XSS | `<script>`, `javascript:`, `onerror=` | BLOCK (403) |
| Path Traversal | `../`, `..\\`, `%2e%2e` | BLOCK (403) |

### API Authentication Examples

```bash
# API Key
curl -H "X-API-Key: nwl_YOUR_KEY_HERE" http://localhost:9090/api/v1/users

# JWT Bearer
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:9090/api/v1/users

# OAuth2 Client Credentials
curl -X POST http://localhost:9090/oauth/token \
  -d "grant_type=client_credentials&client_id=ID&client_secret=SECRET"
## 📊 Logging

Nawala implements **ISO 8601 structured logging** with separated log files:

```
D:/nawala/logs/
├── platform/
│   ├── application.log      # General app logs (JSON)
│   ├── error.log            # ERROR level only
│   ├── security.log         # Auth, access control, threats
│   ├── access.log           # HTTP access logs
│   ├── health.log           # Health check results
│   └── archive/             # Rotated & compressed (.gz)
└── gateway/
    ├── application.log
    ├── error.log
    ├── security.log
    ├── access.log
    └── archive/
```

### Log Entry Example (JSON)

```json
{
  "@timestamp": "2024-01-15T10:30:45.123+07:00",
  "service": "nawala-platform",
  "level": "INFO",
  "logger": "SecurityLogger",
  "thread": "http-nio-8080-exec-1",
  "traceId": "abc123",
  "userId": "admin",
  "clientIp": "192.168.1.100",
  "message": "API key created name=prod-key owner=admin prefix=nwl_abc1"
}
```

---

## 📁 Project Structure

```
nawala-api-gateway/
├── pom.xml                          # Parent POM (multi-module)
├── platform/                        # Management Console
│   └── src/main/java/id/nawala/platform/
│       ├── config/                  # Security, Encryption, Scheduling
│       ├── controller/              # 14 MVC controllers
│       ├── exception/               # Global exception handling
│       ├── logging/                 # Security & Access loggers
│       ├── model/                   # 19 JPA entities
│       ├── repository/              # 18 Spring Data repositories
│       ├── service/impl/            # 15 service implementations
│       ├── util/                    # Encryption utilities
│       └── viewmodel/              # MVVM view models
├── gateway/                         # API Gateway (Reactive)
│   └── src/main/java/id/nawala/gateway/
│       ├── circuitbreaker/          # Circuit breaker registry
│       ├── config/                  # Gateway routing & security
│       ├── filter/                  # 17 gateway filters
│       └── logging/                 # Gateway logging
└── logs/                            # Separated log output
```

---

## 🛣️ Roadmap

- [ ] GraphQL Gateway support
- [ ] gRPC proxy
- [ ] Kubernetes Ingress Controller mode
- [ ] Admin REST API + CLI tool
- [ ] Prometheus + Grafana metrics export
- [ ] Multi-tenancy support
- [ ] Redis Cluster rate limiting
- [ ] API versioning management
- [ ] SDK generation (Java, Python, TypeScript)
- [ ] Docker Compose one-click deployment

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards

- Java 17+ features encouraged
- Sensitive fields must use `@Convert(converter = FieldEncryptor.class)`
- State-changing operations must log to `AuditService`
- Security events must use `SecurityLogger`

---

## ☕ Support This Project

If Nawala helps you or your organization, consider supporting development:

<p align="center">
  <a href="https://saweria.co/rdpf">
    <img src="https://img.shields.io/badge/☕%20Buy%20Me%20a%20Coffee-Support%20via%20Saweria-ff6f00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=white" alt="Support via Saweria"/>
  </a>
</p>

<p align="center">
  <a href="https://saweria.co/rdpf"><strong>👉 https://saweria.co/rdpf 👈</strong></a>
</p>

Your support helps fund:
- 🐛 Bug fixes and security patches
- ✨ New feature development
- 📖 Documentation improvements
- 🌍 Community support

---

## 📜 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 🏷️ Keywords

`api-gateway` `api-management` `spring-boot` `spring-cloud-gateway` `java` `microservices` `oauth2` `jwt` `rate-limiting` `waf` `web-application-firewall` `anomaly-detection` `api-security` `encryption` `circuit-breaker` `load-balancer` `api-key-management` `webhooks` `api-analytics` `self-hosted` `open-source` `enterprise` `api-platform` `reverse-proxy` `developer-tools` `devops` `backend` `middleware` `api-monitoring`

---

<p align="center">Made with ❤️ by <strong>NAWALA TEAM</strong> in Indonesia 🇮🇩</p>
<p align="center"><sub>Nawala — Secure Your APIs, Empower Your Platform.</sub></p>

