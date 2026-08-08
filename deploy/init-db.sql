-- Nawala Gateway Platform Database Initialization
-- This script runs automatically on first Docker startup

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(500) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(500),
    phone VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN DEFAULT true,
    can_manage_routes BOOLEAN DEFAULT false,
    can_manage_keys BOOLEAN DEFAULT false,
    can_view_analytics BOOLEAN DEFAULT false,
    can_manage_users BOOLEAN DEFAULT false,
    can_manage_waf BOOLEAN DEFAULT false,
    theme_preference VARCHAR(20) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT NOW(),
    last_login_at TIMESTAMP
);

-- Privileges table
CREATE TABLE IF NOT EXISTS privileges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- User privileges mapping
CREATE TABLE IF NOT EXISTS user_privileges (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    privilege_id BIGINT REFERENCES privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, privilege_id)
);

-- API Routes
CREATE TABLE IF NOT EXISTS api_routes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    method VARCHAR(50) NOT NULL,
    path VARCHAR(500) NOT NULL,
    masked_path VARCHAR(500),
    target_url VARCHAR(1000) NOT NULL,
    auth_required BOOLEAN NOT NULL DEFAULT false,
    rate_limit_enabled BOOLEAN NOT NULL DEFAULT false,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    active BOOLEAN NOT NULL DEFAULT true,
    payload_encryption BOOLEAN NOT NULL DEFAULT false,
    health_check_url VARCHAR(500),
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',
    last_health_check TIMESTAMP,
    last_response_time_ms INT,
    load_balanced BOOLEAN NOT NULL DEFAULT false,
    load_balancer_strategy VARCHAR(20) DEFAULT 'ROUND_ROBIN',
    current_target_index INT DEFAULT 0,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Route targets for load balancing
CREATE TABLE IF NOT EXISTS route_targets (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES api_routes(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    weight INT DEFAULT 50,
    healthy BOOLEAN DEFAULT true,
    active BOOLEAN DEFAULT true,
    consecutive_failures INT DEFAULT 0,
    last_health_check TIMESTAMP,
    last_response_time_ms BIGINT,
    canary BOOLEAN DEFAULT false,
    canary_percentage INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- API Keys
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    prefix VARCHAR(20) NOT NULL,
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP,
    request_count BIGINT DEFAULT 0,
    daily_quota BIGINT DEFAULT 0,
    daily_usage BIGINT DEFAULT 0,
    monthly_quota BIGINT DEFAULT 0,
    monthly_usage BIGINT DEFAULT 0,
    allowed_ips VARCHAR(1000),
    allowed_routes VARCHAR(1000),
    allowed_methods VARCHAR(200),
    previous_key_hash VARCHAR(255),
    rotation_grace_until TIMESTAMP,
    last_quota_reset TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    last_used_at TIMESTAMP
);

-- OAuth Clients
CREATE TABLE IF NOT EXISTS oauth2_clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    grant_types VARCHAR(200) DEFAULT 'client_credentials',
    scopes VARCHAR(500) DEFAULT 'read',
    redirect_uris VARCHAR(1000),
    access_token_ttl INT DEFAULT 3600,
    refresh_token_ttl INT DEFAULT 86400,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- OAuth Tokens
CREATE TABLE IF NOT EXISTS oauth2_tokens (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT REFERENCES oauth2_clients(id) ON DELETE CASCADE,
    access_token VARCHAR(500) NOT NULL UNIQUE,
    refresh_token VARCHAR(500),
    scopes VARCHAR(500),
    access_token_expires_at TIMESTAMP NOT NULL,
    refresh_token_expires_at TIMESTAMP,
    revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- WAF Rules
CREATE TABLE IF NOT EXISTS waf_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    pattern TEXT,
    action VARCHAR(20) DEFAULT 'BLOCK',
    target_field VARCHAR(50),
    route_id BIGINT,
    priority INT DEFAULT 100,
    active BOOLEAN DEFAULT true,
    description VARCHAR(500),
    match_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Webhooks
CREATE TABLE IF NOT EXISTS webhooks (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    secret VARCHAR(255),
    active BOOLEAN DEFAULT true,
    max_retries INT DEFAULT 3,
    last_triggered_at TIMESTAMP,
    last_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Plugins
CREATE TABLE IF NOT EXISTS plugins (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    hook_type VARCHAR(50) NOT NULL,
    script TEXT,
    route_id BIGINT,
    priority INT DEFAULT 100,
    active BOOLEAN DEFAULT true,
    execution_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_execution_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    last_executed_at TIMESTAMP
);

-- API Mocks
CREATE TABLE IF NOT EXISTS api_mocks (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(500) NOT NULL UNIQUE,
    method VARCHAR(10) NOT NULL,
    status_code INT DEFAULT 200,
    response_body TEXT,
    content_type VARCHAR(100) DEFAULT 'application/json',
    delay_ms INT DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Service Registry
CREATE TABLE IF NOT EXISTS service_registry (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    metadata TEXT,
    healthy BOOLEAN DEFAULT true,
    last_heartbeat TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(service_name, url)
);

-- Request Logs
CREATE TABLE IF NOT EXISTS request_logs (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE SET NULL,
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INT,
    latency_ms BIGINT,
    client_ip VARCHAR(50),
    api_key_id BIGINT REFERENCES api_keys(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- System Settings (for setup wizard)
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(2000),
    description VARCHAR(500),
    encrypted BOOLEAN DEFAULT false,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_api_routes_path ON api_routes(path);
CREATE INDEX IF NOT EXISTS idx_api_routes_active ON api_routes(active);
CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_oauth2_tokens_access ON oauth2_tokens(access_token);
CREATE INDEX IF NOT EXISTS idx_request_logs_created ON request_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_request_logs_route ON request_logs(route_id);
CREATE INDEX IF NOT EXISTS idx_service_registry_name ON service_registry(service_name);

-- Insert default privileges
INSERT INTO privileges (name, description) VALUES
    ('ROUTES_VIEW', 'View API routes'),
    ('ROUTES_MANAGE', 'Create, update, delete routes'),
    ('KEYS_VIEW', 'View API keys'),
    ('KEYS_MANAGE', 'Create, revoke API keys'),
    ('USERS_VIEW', 'View users'),
    ('USERS_MANAGE', 'Manage users and roles'),
    ('WAF_VIEW', 'View WAF settings'),
    ('WAF_MANAGE', 'Manage WAF rules'),
    ('ANALYTICS_VIEW', 'View analytics'),
    ('SETTINGS_MANAGE', 'Manage system settings')
ON CONFLICT (name) DO NOTHING;

-- NOTE: No default admin user - will be created via Setup Wizard on first run

-- Insert initial system settings (setup.completed = false triggers Setup Wizard)
INSERT INTO system_settings (setting_key, setting_value) VALUES
    ('setup.completed', 'false'),
    ('platform.name', 'Nawala Gateway'),
    ('platform.version', '1.0.0')
ON CONFLICT (setting_key) DO NOTHING;

-- Additional tables for JPA entities

-- Activity Logs
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    ip_address VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Anomaly Events
CREATE TABLE IF NOT EXISTS anomaly_events (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    source_ip VARCHAR(500),
    api_key_prefix VARCHAR(100),
    description VARCHAR(1000),
    request_count INT NOT NULL DEFAULT 0,
    target_path VARCHAR(500),
    resolved BOOLEAN DEFAULT false,
    auto_blocked BOOLEAN DEFAULT false,
    blocked_until TIMESTAMP,
    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50)
);

-- API Analytics
CREATE TABLE IF NOT EXISTS api_analytics (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT,
    api_key_prefix VARCHAR(20),
    source_ip VARCHAR(50),
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INT,
    response_time_ms BIGINT,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    country VARCHAR(50),
    city VARCHAR(100),
    recorded_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_analytics_route_time ON api_analytics(route_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_analytics_api_key ON api_analytics(api_key_prefix, recorded_at);

-- API Docs
CREATE TABLE IF NOT EXISTS api_docs (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE SET NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    version VARCHAR(20) DEFAULT '1.0.0',
    open_api_spec TEXT,
    description TEXT,
    published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Audit Log
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action, created_at);

-- Rate Limit Tiers
CREATE TABLE IF NOT EXISTS rate_limit_tiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    requests_per_minute INT NOT NULL DEFAULT 60,
    requests_per_hour INT NOT NULL DEFAULT 1000,
    requests_per_day INT NOT NULL DEFAULT 10000,
    burst_size INT DEFAULT 10,
    description VARCHAR(200),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Route Transformations
CREATE TABLE IF NOT EXISTS route_transformations (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES api_routes(id) ON DELETE CASCADE,
    phase VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    key VARCHAR(200),
    value VARCHAR(500),
    config TEXT,
    priority INT DEFAULT 100,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Webhook Deliveries
CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    http_status INT,
    response_body TEXT,
    duration_ms BIGINT,
    attempt_number INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING',
    delivered_at TIMESTAMP DEFAULT NOW(),
    next_retry_at TIMESTAMP
);

