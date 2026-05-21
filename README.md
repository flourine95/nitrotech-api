# Nitrotech API

E-commerce REST API built with Spring Boot 4 and Java 21.

## Requirements

- Java 21+
- Docker & Docker Compose 24+

## Quick Start

### 1. Clone repository

```bash
git clone https://github.com/flourine95/nitrotech-api.git
cd nitrotech-api
```

### 2. Start infrastructure services

```bash
docker compose up -d
```

This starts PostgreSQL and Redis.

### 3. Configure environment

Copy the example configuration:

```bash
cp src/main/resources/application-dev.example.yaml src/main/resources/application-dev.yaml
```

Edit `application-dev.yaml` with your credentials:

```yaml
spring:
  mail:
    username: your-mailtrap-username
    password: your-mailtrap-password

cloudinary:
  cloud-name: your-cloud-name
  api-key: your-api-key
  api-secret: your-api-secret
```

### 4. Run application

```bash
./gradlew bootRun
```

Application runs at `http://localhost:8080` and redirects to Swagger UI.

## Configuration Files

| File | Committed | Purpose |
|------|-----------|---------|
| `application.yaml` | Yes | Base configuration with environment variables |
| `application-dev.yaml` | No | Local development with credentials |
| `application-dev.example.yaml` | Yes | Template for team |
| `application-prod.yaml` | No | Production with credentials |
| `application-test.yaml` | Yes | CI/CD with H2 in-memory database |

## Environment Variables (Production)

Required:

- `DB_URL` - PostgreSQL JDBC URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `REDIS_URL` - Redis URL
- `MAIL_HOST` - SMTP host
- `MAIL_USERNAME` - SMTP username
- `MAIL_PASSWORD` - SMTP password
- `MAIL_FROM` - Sender email address
- `FRONTEND_URL` - Frontend URL for email links
- `CORS_ALLOWED_ORIGINS` - Comma-separated frontend domains
- `CLOUDINARY_CLOUD_NAME` - Cloudinary cloud name
- `CLOUDINARY_API_KEY` - Cloudinary API key
- `CLOUDINARY_API_SECRET` - Cloudinary API secret

Optional:

- `MAIL_PORT` - SMTP port (default: 587)
- `SERVER_PORT` - Application port (default: 8080)

## Project Structure

```
src/main/java/com/nitrotech/api/
├── application/      # HTTP layer (Controllers, Request DTOs)
├── domain/           # Business logic (Use Cases, Domain DTOs, Repository Interfaces)
├── infrastructure/   # Technical implementation (JPA Entities, Repository Implementations)
└── shared/           # Cross-cutting concerns (Config, Exception Handler, Utilities)
```

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Documentation

- [ARCHITECTURE.md](.docs/ARCHITECTURE.md) - System architecture and design patterns
- [CODING-STANDARDS.md](.docs/CODING-STANDARDS.md) - Coding conventions and best practices
- [DATABASE-DESIGN.md](.docs/DATABASE-DESIGN.md) - Database schema and design

## License

This project is licensed under the MIT License.
