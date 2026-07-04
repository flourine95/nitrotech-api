# Nitrotech API

Nitrotech API is the Spring Boot backend for an e-commerce platform that sells technology products such as laptops, PC parts, and accessories.

## Requirements

- Java 21+
- Docker and Docker Compose 24+

## Quick start

Clone the repository:

```bash
git clone https://github.com/flourine95/nitrotech-api.git
cd nitrotech-api
```

Start PostgreSQL and Redis:

```bash
docker compose up -d
```

Create a local development config:

```bash
cp src/main/resources/application-dev.example.yaml src/main/resources/application-dev.yaml
```

Update `application-dev.yaml` with local credentials:

```yaml
resend:
  api-key: your_resend_api_key

mail:
  from: NitroTech <noreply@send.yourdomain.com>

cloudinary:
  cloud-name: your_cloud_name
  api-key: your_cloudinary_api_key
  api-secret: your_cloudinary_api_secret
```

Run the application:

```bash
./gradlew bootRun
```

The API runs at `http://localhost:8080` and redirects to Swagger UI.

## Verification

Run the backend test suite:

```bash
./gradlew test
```

## Configuration files

| File | Committed | Purpose |
|------|-----------|---------|
| `application.yaml` | Yes | Base configuration with environment variables |
| `application-dev.yaml` | No | Local development credentials |
| `application-dev.example.yaml` | Yes | Local development template |
| `application-prod.yaml` | No | Production credentials |
| `application-test.yaml` | Yes | Test configuration with H2 |

## Environment variables

Database and cache:

- `DB_URL`: PostgreSQL JDBC URL
- `DB_USERNAME`: database username
- `DB_PASSWORD`: database password
- `REDIS_URL`: Redis URL

Mail and frontend links:

- `RESEND_API_KEY`: Resend API key
- `MAIL_FROM`: sender email address
- `FRONTEND_URL`: frontend URL for email links
- `CORS_ALLOWED_ORIGINS`: comma-separated frontend origins

Public API and media:

- `PUBLIC_API_URL`: public API base URL for third-party callbacks
- `CLOUDINARY_CLOUD_NAME`: Cloudinary cloud name
- `CLOUDINARY_API_KEY`: Cloudinary API key
- `CLOUDINARY_API_SECRET`: Cloudinary API secret

Shipping:

- `GHN_API_URL`: GHN API base URL
- `GHN_TOKEN`: GHN API token
- `GHN_SHOP_ID`: GHN shop ID
- `GHN_CLIENT_ID`: GHN client ID for webhook setup
- `GHTK_PICKUP_NAME`: GHTK pickup contact name
- `GHTK_PICKUP_TEL`: GHTK pickup phone
- `GHTK_PICKUP_ADDRESS`: GHTK pickup street address
- `GHTK_PICKUP_PROVINCE`: GHTK pickup province or city
- `GHTK_PICKUP_DISTRICT`: GHTK pickup district

Payment:

- `SEPAY_WEBHOOK_API_KEY`: SePay webhook API key
- `SEPAY_ACCOUNT_NUMBER`: bank account number used for SePay QR codes
- `SEPAY_BANK_NAME`: bank code or name used for SePay QR codes
- `SEPAY_PAYMENT_CODE_PREFIX`: payment code prefix, defaults to `NT`

Optional:

- `SERVER_PORT`: application port, defaults to `8080`
- `GHTK_PICKUP_ADDRESS_ID`: GHTK pickup warehouse or address ID
- `GHTK_PICKUP_WARD`: GHTK pickup ward

## Shipping webhooks

GHN order status callbacks are received at:

```text
${PUBLIC_API_URL}/api/webhooks/shipping/ghn
```

For local testing, post a GHN-style payload directly to:

```text
http://localhost:8080/api/webhooks/shipping/ghn
```

For staging or production, send GHN these values:

```text
ClientID: <GHN_CLIENT_ID>
URL Webhook: <PUBLIC_API_URL>/api/webhooks/shipping/ghn
Environment: Staging or Production
Company name: NitroTech
```

The endpoint expects GHN fields such as `OrderCode`, `Status`, `Type`, `Warehouse`, and `Time`, then returns HTTP 200 when the payload is processed.

## Project structure

```text
src/main/java/com/nitrotech/api/
├── application/      # HTTP layer: controllers and request DTOs
├── domain/           # Business logic: use cases, domain DTOs, repository ports
├── infrastructure/   # Technical adapters: JPA, providers, storage, mail
└── shared/           # Cross-cutting concerns: config, exceptions, security, seeders
```

## API documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Project documentation

- [Architecture](.docs/ARCHITECTURE.md): system layers, data flow, and module responsibilities
- [Coding standards](.docs/CODING-STANDARDS.md): Java, Spring, API, persistence, and testing conventions
- [Database design](.docs/DATABASE-DESIGN.md): schema notes and table relationships

## License

This project is licensed under the MIT License.
