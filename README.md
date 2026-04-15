# Nitrotech API

REST API xây dựng trên **Spring Boot 4** + **Java 21**, áp dụng kiến trúc **Domain-Oriented Layered Architecture**.

---

## Yêu cầu hệ thống

| Công cụ | Phiên bản |
|---------|-----------|
| Java | 21+ |
| Docker & Docker Compose | 24+ |
| Gradle | 8+ (wrapper đã có sẵn) |

---

## Cài đặt & Chạy dự án

### 1. Clone repository

```bash
git clone https://github.com/flourine95/nitrotech-api.git
cd nitrotech-api
```

### 2. Khởi động services

Dự án dùng Docker Compose để chạy PostgreSQL và Redis. Spring Boot DevTools tự động start containers khi chạy app.

Nếu muốn start thủ công:

```bash
docker compose up -d
```

Services mặc định:
- PostgreSQL: `localhost:5432` — database `nitrotech`, user `nitrotech`, password `nitrotech`
- Redis: `localhost:6379`

### 3. Cấu hình môi trường

Tạo file `application-dev.yaml` từ template:

```bash
cp src/main/resources/application-dev.example.yaml src/main/resources/application-dev.yaml
```

Chỉnh các giá trị cần thiết trong `application-dev.yaml`:

```yaml
spring:
  mail:
    username: your-mailtrap-username   # Lấy tại mailtrap.io → Sandboxes → SMTP Settings
    password: your-mailtrap-password

cloudinary:
  cloud-name: your-cloud-name          # Lấy tại cloudinary.com/console
  api-key: your-api-key
  api-secret: your-api-secret
```

| File | Commit | Mục đích |
|------|--------|----------|
| `application.yaml` | ✅ | Config chung, dùng biến môi trường |
| `application-dev.yaml` | ❌ | Local dev, chứa credentials |
| `application-dev.example.yaml` | ✅ | Template cho team |
| `application-prod.yaml` | ❌ | Production, chứa credentials |
| `application-test.yaml` | ✅ | CI/CD, dùng H2 in-memory |

### 4. Chạy ứng dụng

```bash
./gradlew bootRun
```

Ứng dụng chạy tại `http://localhost:8080` — tự redirect sang Swagger UI.

---

## Biến môi trường (Production)

| Biến | Bắt buộc | Mô tả |
|------|----------|-------|
| `DB_URL` | ✅ | JDBC URL PostgreSQL |
| `DB_USERNAME` | ✅ | Database username |
| `DB_PASSWORD` | ✅ | Database password |
| `REDIS_URL` | ✅ | Redis URL (redis://host:6379) |
| `MAIL_HOST` | ✅ | SMTP host |
| `MAIL_USERNAME` | ✅ | SMTP username |
| `MAIL_PASSWORD` | ✅ | SMTP password |
| `MAIL_FROM` | ✅ | Email gửi đi |
| `FRONTEND_URL` | ✅ | URL frontend (dùng trong email link) |
| `CORS_ALLOWED_ORIGINS` | ✅ | Domain frontend, phân cách bằng dấu phẩy |
| `CLOUDINARY_CLOUD_NAME` | ✅ | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | ✅ | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | ✅ | Cloudinary API secret |
| `MAIL_PORT` | ❌ | SMTP port (default: 587) |
| `SERVER_PORT` | ❌ | Port ứng dụng (default: 8080) |

---

## Cấu trúc dự án

```
src/main/java/com/nitrotech/api/
├── domain/           # Business logic (Java thuần, không có Spring annotation)
│   └── {module}/
│       ├── dto/          # Records — Commands, Results
│       ├── usecase/      # Business logic
│       ├── repository/   # Interfaces
│       └── exception/    # Domain exceptions
├── infrastructure/   # Framework/Database layer
│   ├── persistence/  # JPA Entities, Repositories, Mappers
│   ├── mail/         # SMTP email sender
│   └── storage/      # Cloudinary integration
├── application/      # HTTP layer
│   └── {module}/
│       ├── controller/   # REST Controllers
│       └── request/      # Request DTOs
└── shared/           # Shared kernel
    ├── config/       # Spring configs (Security, Redis, CORS, Async)
    ├── exception/    # Global exception handler
    ├── response/     # ApiResponse wrapper
    └── util/         # Utilities
```

Xem chi tiết kiến trúc: [.docs/ARCHITECTURE.md](.docs/ARCHITECTURE.md)

Xem coding standards: [.docs/CODING-STANDARDS.md](.docs/CODING-STANDARDS.md)

Xem database design: [.docs/DATABASE-DESIGN.md](.docs/DATABASE-DESIGN.md)

Xem API docs: [.docs/api/](.docs/api/)

---

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI spec: `http://localhost:8080/v3/api-docs`

Capture API docs từ server đang chạy:

```bash
python scripts/run.py
```

---

## Database Migrations

Dùng **Flyway**, migration files tại `src/main/resources/db/migration/`.

Naming: `V{version}__{description}.sql` — Flyway tự chạy khi app khởi động.

Reset DB (dev only):

```bash
docker compose down -v && docker compose up -d
```

---

## Troubleshooting

**Lỗi kết nối database**
```bash
docker compose ps
docker compose logs postgres
```

**Trùng port (5432 hoặc 6379 đã bị dùng)**

Tạo file `.env` ở root project:
```env
POSTGRES_PORT=5433
REDIS_PORT=6380
```
Sau đó cập nhật `application-dev.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/nitrotech
  data:
    redis:
      url: redis://localhost:6380
```

**Lỗi Flyway migration checksum mismatch**
```sql
DELETE FROM flyway_schema_history WHERE version = '<version>';
```
Sau đó restart app.

**Email không gửi được**
- Kiểm tra `spring.mail.username` và `password` trong `application-dev.yaml`
- Dùng Mailtrap sandbox để test local — không gửi email thật ra ngoài

**Java version không đúng**
```bash
java -version  # phải là 21+
```
