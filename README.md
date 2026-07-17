<p align="center">
  <img src="docs/images/banner.svg" alt="Nawala Gateway Platform" width="100%"/>
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
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
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

<p align="center">
  <img src="docs/images/architecture.svg" alt="Nawala Architecture" width="100%"/>
</p>

### Request Processing Flow

<p align="center">
  <img src="docs/images/request-flow.svg" alt="Request Flow" width="100%"/>
</p>

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.2.5 |
| Gateway | Spring Cloud Gateway (Reactive/WebFlux) |
| Frontend | Thymeleaf + Custom CSS + Vanilla JS |
| Database | MySQL 8.0 |
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
| MySQL | 8.0+ | ✅ |
| Redis | 7.x | ✅ |
| Git | 2.x | ✅ |

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/dansiapa/nawala-gateway-platform.git
cd nawala-gateway-platform

# 2. Create MySQL database
mysql -u root -e "CREATE DATABASE nawala_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Start Redis
redis-server

# 4. Build the project
./mvnw clean package -DskipTests

# 5. Start Platform (Management Console)
java -jar platform/target/nawala-platform-1.0.0.jar

# 6. Start Gateway (separate terminal)
java -jar gateway/target/nawala-gateway-1.0.0.jar
```

### Service Endpoints

| Service | Port | Description |
|---------|------|-------------|
| Platform Console | `:8080` | Web management UI |
| API Gateway | `:9090` | Gateway routing endpoint |
| Default Admin | `admin` / `admin123` | Change immediately after first login! |

---

## ⚙️ Configuration

### Platform

```properties
spring.datasource.url=jdbc:mysql://<DB_HOST>:3306/nawala_db?useSSL=false&serverTimezone=Asia/Jakarta
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASS>
nawala.encryption.key=<YOUR_BASE64_AES_256_KEY>
nawala.internal.secret=<YOUR_INTERNAL_SECRET>
```

### Gateway

```properties
nawala.gateway.platform-url=http://<PLATFORM_HOST>:8080
nawala.gateway.internal-secret=<YOUR_INTERNAL_SECRET>
nawala.gateway.jwt.secret=<YOUR_BASE64_JWT_SECRET>
nawala.gateway.payload-key=<YOUR_BASE64_AES_256_KEY>
spring.data.redis.host=<REDIS_HOST>
spring.data.redis.port=6379
```

### Generate Secure Keys

```bash
openssl rand -base64 32   # AES-256 key
openssl rand -base64 64   # JWT secret
openssl rand -hex 32      # Internal secret
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

### API Authentication

```bash
# API Key
curl -H "X-API-Key: nwl_YOUR_KEY_HERE" http://<GATEWAY>:9090/api/v1/users

# JWT Bearer
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://<GATEWAY>:9090/api/v1/users

# OAuth2 Client Credentials
curl -X POST http://<GATEWAY>:9090/oauth/token \
  -d "grant_type=client_credentials&client_id=ID&client_secret=SECRET"
```

---

## 📊 Logging

Structured ISO 8601 JSON logging with separated files:

```
logs/
├── platform/
│   ├── application.log    # General (JSON)
│   ├── error.log          # ERROR only
│   ├── security.log       # Auth & threats
│   ├── access.log         # HTTP access
│   ├── health.log         # Health checks
│   └── archive/           # Rotated (.gz)
└── gateway/
    ├── application.log
    ├── error.log
    ├── security.log
    ├── access.log
    └── archive/
```

---

## 📁 Project Structure

```
nawala-gateway-platform/
├── pom.xml                    # Parent POM (multi-module)
├── platform/                  # Management Console
│   └── src/main/java/id/nawala/platform/
│       ├── config/            # Security, Encryption, Scheduling
│       ├── controller/        # 14 MVC controllers
│       ├── model/             # 19 JPA entities
│       ├── repository/        # 18 Spring Data repositories
│       ├── service/impl/      # 15 service implementations
│       ├── util/              # Encryption utilities
│       └── viewmodel/         # MVVM view models
├── gateway/                   # API Gateway (Reactive)
│   └── src/main/java/id/nawala/gateway/
│       ├── circuitbreaker/    # Circuit breaker registry
│       ├── config/            # Routing & security
│       ├── filter/            # 17 gateway filters
│       └── logging/           # Gateway logging
└── logs/                      # Separated log output
```

---

## 🛣️ Roadmap

- [ ] GraphQL Gateway support
- [ ] gRPC proxy
- [ ] Kubernetes Ingress Controller mode
- [ ] Admin REST API + CLI tool
- [ ] Prometheus + Grafana metrics export
- [ ] Multi-tenancy support
- [ ] Docker Compose one-click deployment

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## ☕ Support This Project

<p align="center">
  <a href="https://saweria.co/rdpf">
    <img src="https://img.shields.io/badge/☕%20Buy%20Me%20a%20Coffee-Support%20via%20Saweria-ff6f00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=white" alt="Support via Saweria"/>
  </a>
</p>

<p align="center">
  <a href="https://saweria.co/rdpf"><strong>👉 https://saweria.co/rdpf 👈</strong></a>
</p>

---

## 📜 License

Copyright © 2026 **NAWALA TEAM**. All rights reserved.

Licensed under the [MIT License](LICENSE). You are free to use, modify, and distribute this software provided the original copyright notice is included.

---

## 🏷️ Keywords

`api-gateway` `api-management` `spring-boot` `spring-cloud-gateway` `java` `microservices` `oauth2` `jwt` `rate-limiting` `waf` `web-application-firewall` `anomaly-detection` `api-security` `encryption` `circuit-breaker` `load-balancer` `api-key-management` `webhooks` `api-analytics` `self-hosted` `open-source` `enterprise` `reverse-proxy` `developer-tools` `devops` `api-monitoring`

---

<p align="center">Made with ❤️ by <strong>NAWALA TEAM</strong> in Indonesia 🇮🇩</p>
<p align="center"><sub>Nawala — Secure Your APIs, Empower Your Platform.</sub></p>
<p align="center"><sub>Copyright © 2026 NAWALA TEAM. Licensed under MIT.</sub></p>


