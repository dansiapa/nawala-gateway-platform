-- Service Discovery Registry
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

CREATE INDEX IF NOT EXISTS idx_service_registry_name ON service_registry(service_name);
CREATE INDEX IF NOT EXISTS idx_service_registry_healthy ON service_registry(healthy);

-- API Version Management
CREATE TABLE IF NOT EXISTS api_versions (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE CASCADE,
    version VARCHAR(20) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    deprecated BOOLEAN DEFAULT false,
    sunset_date DATE,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_api_versions_route ON api_versions(route_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_api_versions_unique ON api_versions(route_id, version);

-- GraphQL Endpoints
CREATE TABLE IF NOT EXISTS graphql_endpoints (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(200) NOT NULL UNIQUE,
    target_url VARCHAR(500) NOT NULL,
    auth_required BOOLEAN DEFAULT false,
    rate_limit_enabled BOOLEAN DEFAULT false,
    rate_limit_per_minute INT DEFAULT 100,
    introspection_enabled BOOLEAN DEFAULT true,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Add version support to api_routes
ALTER TABLE api_routes ADD COLUMN IF NOT EXISTS versioned BOOLEAN DEFAULT false;
ALTER TABLE api_routes ADD COLUMN IF NOT EXISTS default_version VARCHAR(20);
