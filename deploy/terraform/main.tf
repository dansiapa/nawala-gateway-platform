# Nawala Gateway Terraform Provider

terraform {
  required_providers {
    nawala = {
      source  = "nawala-team/nawala"
      version = "~> 1.0"
    }
  }
}

provider "nawala" {
  host     = var.nawala_host
  api_key  = var.nawala_api_key
}

# Variables
variable "nawala_host" {
  description = "Nawala Platform URL"
  type        = string
  default     = "http://localhost:8080"
}

variable "nawala_api_key" {
  description = "Nawala API Key"
  type        = string
  sensitive   = true
}

# API Route Resource
resource "nawala_api_route" "example" {
  name        = "example-api"
  path        = "/api/v1/example"
  target_url  = "http://backend:8080"
  methods     = ["GET", "POST"]
  
  rate_limit {
    enabled     = true
    per_minute  = 100
  }
  
  auth {
    type = "api_key"
  }
}

# API Key Resource
resource "nawala_api_key" "app_key" {
  name        = "my-app-key"
  description = "API key for my application"
  tier_id     = nawala_tier.standard.id
  
  allowed_ips = ["10.0.0.0/8"]
}

# Tier Resource
resource "nawala_tier" "standard" {
  name              = "standard"
  rate_limit        = 1000
  quota_per_day     = 10000
  description       = "Standard tier"
}

# OAuth Client Resource
resource "nawala_oauth_client" "app" {
  name          = "my-oauth-app"
  redirect_uris = ["https://app.example.com/callback"]
  grant_types   = ["authorization_code", "refresh_token"]
  scopes        = ["read", "write"]
}

# Outputs
output "api_route_id" {
  value = nawala_api_route.example.id
}

output "api_key" {
  value     = nawala_api_key.app_key.key
  sensitive = true
}
