-- Nawala Platform Database Schema
-- Full schema for all features

CREATE DATABASE IF NOT EXISTS nawala_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nawala_db;

-- Spring Security Remember-Me token store
CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(500) NOT NULL,
    full_name VARCHAR(500),
    phone VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME
);

-- API Routes
CREATE TABLE IF NOT EXISTS api_routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    method VARCHAR(50) NOT NULL,
    path VARCHAR(500) NOT NULL,
    masked_path VARCHAR(500),
    target_url VARCHAR(1000) NOT NULL,
    auth_required BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    payload_encryption BOOLEAN NOT NULL DEFAULT FALSE,
    health_check_url VARCHAR(500),
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',
    last_health_check DATETIME,
    last_response_time_ms INT,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- API Keys with scoping/quota
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    prefix VARCHAR(20),
    user_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at DATETIME,
    request_count BIGINT DEFAULT 0,
    daily_quota BIGINT DEFAULT 0,
    daily_usage BIGINT DEFAULT 0,
    monthly_quota BIGINT DEFAULT 0,
    monthly_usage BIGINT DEFAULT 0,
    allowed_ips VARCHAR(2000),
    allowed_routes VARCHAR(2000),
    allowed_methods VARCHAR(200),
    previous_key_hash VARCHAR(255),
    rotation_grace_until DATETIME,
    last_quota_reset DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- API Analytics
CREATE TABLE IF NOT EXISTS api_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT,
    api_key_prefix VARCHAR(20),
    source_ip VARCHAR(45),
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INT,
    response_time_ms BIGINT,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    country VARCHAR(100),
    city VARCHAR(100),
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analytics_route_time (route_id, recorded_at),
    INDEX idx_analytics_api_key (api_key_prefix, recorded_at)
);

-- WAF Rules
CREATE TABLE IF NOT EXISTS waf_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    pattern TEXT,
    action VARCHAR(20) NOT NULL,
    target_field VARCHAR(30),
    route_id BIGINT,
    priority INT DEFAULT 100,
    active BOOLEAN DEFAULT TRUE,
    description VARCHAR(500),
    match_count BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Webhooks
CREATE TABLE IF NOT EXISTS webhooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(1000) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    secret VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    max_retries INT DEFAULT 3,
    last_triggered_at DATETIME,
    last_status VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Webhook Deliveries
CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    http_status INT,
    response_body TEXT,
    duration_ms BIGINT,
    attempt_number INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING',
    delivered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    next_retry_at DATETIME,
    FOREIGN KEY (webhook_id) REFERENCES webhooks(id)
);

-- API Mocks
CREATE TABLE IF NOT EXISTS api_mocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INT DEFAULT 200,
    response_body TEXT,
    content_type VARCHAR(100) DEFAULT 'application/json',
    delay_ms INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Route Targets (Load Balancer)
CREATE TABLE IF NOT EXISTS route_targets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    target_url VARCHAR(1000) NOT NULL,
    weight INT DEFAULT 100,
    strategy VARCHAR(20) DEFAULT 'ROUND_ROBIN',
    healthy BOOLEAN DEFAULT TRUE,
    active BOOLEAN DEFAULT TRUE,
    consecutive_failures INT DEFAULT 0,
    last_health_check DATETIME,
    last_response_time_ms BIGINT,
    canary BOOLEAN DEFAULT FALSE,
    canary_percentage INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES api_routes(id)
);

-- Route Transformations
CREATE TABLE IF NOT EXISTS route_transformations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    phase VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    `key` VARCHAR(200),
    value VARCHAR(500),
    config TEXT,
    priority INT DEFAULT 100,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES api_routes(id)
);

-- OAuth2 Clients
CREATE TABLE IF NOT EXISTS oauth2_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL,
    grant_types VARCHAR(200) DEFAULT 'client_credentials',
    scopes VARCHAR(200) DEFAULT 'read',
    redirect_uris VARCHAR(2000),
    access_token_ttl INT DEFAULT 3600,
    refresh_token_ttl INT DEFAULT 86400,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- OAuth2 Tokens
CREATE TABLE IF NOT EXISTS oauth2_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    access_token VARCHAR(512) NOT NULL UNIQUE,
    refresh_token VARCHAR(512) UNIQUE,
    scopes VARCHAR(200),
    access_token_expires_at DATETIME,
    refresh_token_expires_at DATETIME,
    revoked BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token_access (access_token),
    INDEX idx_token_refresh (refresh_token),
    FOREIGN KEY (client_id) REFERENCES oauth2_clients(id)
);

-- API Documentation
CREATE TABLE IF NOT EXISTS api_docs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    version VARCHAR(20) DEFAULT '1.0.0',
    open_api_spec LONGTEXT,
    description TEXT,
    published BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (route_id) REFERENCES api_routes(id),
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Plugins
CREATE TABLE IF NOT EXISTS plugins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    hook_type VARCHAR(30) NOT NULL,
    script LONGTEXT,
    route_id BIGINT,
    priority INT DEFAULT 100,
    active BOOLEAN DEFAULT TRUE,
    execution_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_execution_time_ms BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_executed_at DATETIME,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Audit Log
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id, created_at),
    INDEX idx_audit_action (action, created_at)
);

-- Rate Limit Tiers
CREATE TABLE IF NOT EXISTS rate_limit_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    requests_per_minute INT NOT NULL DEFAULT 60,
    requests_per_hour INT NOT NULL DEFAULT 1000,
    requests_per_day INT NOT NULL DEFAULT 10000,
    burst_size INT DEFAULT 10,
    description VARCHAR(200),
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Threat Intelligence (blocked IPs)
CREATE TABLE IF NOT EXISTS threat_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    threat_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    reason VARCHAR(500),
    blocked BOOLEAN DEFAULT TRUE,
    expires_at DATETIME,
    hit_count BIGINT DEFAULT 0,
    first_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_threat_ip (ip_address)
);

-- Insert default rate limit tiers
INSERT IGNORE INTO rate_limit_tiers (name, requests_per_minute, requests_per_hour, requests_per_day, burst_size, description) VALUES
('FREE', 30, 500, 5000, 5, 'Free tier - basic access'),
('STARTER', 60, 2000, 20000, 10, 'Starter tier - small projects'),
('PROFESSIONAL', 120, 5000, 50000, 20, 'Professional tier - production use'),
('ENTERPRISE', 600, 30000, 300000, 50, 'Enterprise tier - high volume'),
('UNLIMITED', 99999, 999999, 9999999, 100, 'Unlimited - internal/admin');
