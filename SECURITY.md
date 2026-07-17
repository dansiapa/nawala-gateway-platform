# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | ✅ Active support  |

## Reporting a Vulnerability

If you discover a security vulnerability in Nawala, please report it responsibly:

1. **DO NOT** open a public GitHub issue for security vulnerabilities
2. Email: nawala.security@proton.me
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Response Timeline

- **24 hours** — Acknowledgment of report
- **72 hours** — Initial assessment
- **7 days** — Fix development (critical)
- **30 days** — Fix development (non-critical)

## Security Features

Nawala implements multiple layers of security:

- AES-256-GCM encryption for all sensitive database fields
- BCrypt (cost=12) for password hashing
- Shared-secret internal API protection
- Web Application Firewall (WAF)
- Anomaly detection with auto-blocking
- Rate limiting (multi-window)
- CSRF protection
- Session fixation protection
- Input validation on all endpoints

## Responsible Disclosure

We appreciate responsible disclosure and will credit researchers who report valid vulnerabilities (with permission) in our release notes.
