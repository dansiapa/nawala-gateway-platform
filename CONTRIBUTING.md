# Contributing to Nawala

Thank you for your interest in contributing to Nawala! 🎉

## How to Contribute

### Bug Reports

- Use GitHub Issues with the `bug` label
- Include: Java version, OS, steps to reproduce, expected vs actual behavior
- Include relevant log entries from `logs/` directory

### Feature Requests

- Use GitHub Issues with the `enhancement` label
- Describe the use case and expected behavior
- Reference similar features in other API gateways if applicable

### Pull Requests

1. Fork the repository
2. Create a feature branch from `develop`:
   ```bash
   git checkout develop
   git checkout -b feature/your-feature-name
   ```
3. Make your changes following code standards
4. Test thoroughly
5. Push and create a Pull Request to `develop` branch

### Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Production-ready releases (protected) |
| `develop` | Integration branch for features |
| `test` | QA and testing |
| `feature/*` | New features |
| `hotfix/*` | Critical bug fixes |

### Code Standards

- Java 17+ features are encouraged
- Follow existing naming conventions
- All sensitive data must use `@Convert(converter = FieldEncryptor.class)`
- State-changing operations must log to `AuditService`
- Security-related events must use `SecurityLogger`
- Write meaningful commit messages following conventional commits
- Add JavaDoc for public methods

### Development Setup

```bash
# Prerequisites: Java 17+, Maven 3.8+, MySQL 8.0, Redis 7.x

git clone https://github.com/dansiapa/nawala-gateway-platform.git
cd nawala-gateway-platform
git checkout develop

# Create database
mysql -u root -e "CREATE DATABASE nawala_db;"

# Build
./mvnw clean package -DskipTests

# Run
java -jar platform/target/nawala-platform-1.0.0.jar
java -jar gateway/target/nawala-gateway-1.0.0.jar
```

## Code of Conduct

Be respectful, inclusive, and constructive. We are all here to build great software together.

## Questions?

Open a GitHub Discussion or reach out to the NAWALA TEAM.
