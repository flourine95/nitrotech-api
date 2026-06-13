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
resend:
  api-key: your-resend-api-key

mail:
  from: NitroTech <noreply@send.yourdomain.com>

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
- `RESEND_API_KEY` - Resend API key
- `MAIL_FROM` - Sender email address
- `FRONTEND_URL` - Frontend URL for email links
- `PUBLIC_API_URL` - Public API base URL used for third-party callbacks
- `CORS_ALLOWED_ORIGINS` - Comma-separated frontend domains
- `CLOUDINARY_CLOUD_NAME` - Cloudinary cloud name
- `CLOUDINARY_API_KEY` - Cloudinary API key
- `CLOUDINARY_API_SECRET` - Cloudinary API secret
- `GHN_API_URL` - GHN API base URL
- `GHN_TOKEN` - GHN API token
- `GHN_SHOP_ID` - GHN shop ID
- `GHN_CLIENT_ID` - GHN client ID used when requesting webhook setup

Optional:

- `SERVER_PORT` - Application port (default: 8080)

## GHN Webhook Setup

GHN order status callbacks are received at:

```text
${PUBLIC_API_URL}/api/webhooks/shipping/ghn
```

For local-only testing, post a GHN-style payload directly to:

```text
http://localhost:8080/api/webhooks/shipping/ghn
```

For staging or production, GHN configures the webhook on their side. Send GHN/account manager:

```text
ClientID: <GHN_CLIENT_ID>
URL Webhook: <PUBLIC_API_URL>/api/webhooks/shipping/ghn
Environment: Staging or Production
Company name: <company name>
```

Example:

```text
ClientID: 123456
URL Webhook: https://api-staging.example.com/api/webhooks/shipping/ghn
Environment: Staging
Company name: NitroTech
```

The endpoint expects GHN's `POST` JSON payload with fields such as `OrderCode`, `Status`, `Type`, `Warehouse`, and `Time`, and returns HTTP 200 when processed.

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
