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
resend:
  api-key: re_your_api_key    # Lấy tại resend.com
  from: onboarding@resend.dev # Hoặc email@yourdomain.com sau khi verify domain

app:
  frontend-url: http://localhost:3000  # URL frontend để gửi link email

cors:
  allowed-origins: http://localhost:3000  # Domain frontend được phép gọi API
```

| File | Commit | Mục đích |
|------|--------|----------|
| `application.yaml` | ✅ | Default chung, dùng biến môi trường |
| `application-dev.yaml` | ❌ | Local dev, chứa credentials |
| `application-prod.yaml` | ❌ | Production, chứa credentials |
| `application-test.yaml` | ✅ | CI/CD |
| `application-dev.example.yaml` | ✅ | Template cho team |

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
| `JWT_SECRET` | ✅ | Secret key, tối thiểu 32 ký tự |
| `RESEND_API_KEY` | ✅ | API key từ resend.com |
| `RESEND_FROM` | ✅ | Email gửi đi (đã verify domain) |
| `FRONTEND_URL` | ✅ | URL frontend (dùng trong email link) |
| `CORS_ALLOWED_ORIGINS` | ✅ | Domain frontend, phân cách bằng dấu phẩy |
| `COOKIE_DOMAIN` | ❌ | Domain cho cookie (để trống nếu same-domain) |
| `COOKIE_SAME_SITE` | ❌ | Lax (default) / Strict / None |
| `JWT_ACCESS_EXPIRATION_MS` | ❌ | Access token TTL ms (default: 900000 = 15 phút) |

---

## Authentication Flow

API hỗ trợ 2 client type qua header `X-Client-Type`:

**Web (default):**
- Refresh token → httpOnly cookie (browser tự gửi, JS không đọc được)
- Access token → response body, lưu trong memory (Zustand, không persist)
- Fetch phải có `credentials: "include"`

**Mobile:**
- Refresh token → response body, lưu trong Keychain (iOS) / EncryptedSharedPreferences (Android)
- Access token → response body

```
# Web
POST /api/auth/login
X-Client-Type: web
→ Set-Cookie: refreshToken=...; HttpOnly
→ body: { accessToken, tokenType, user }

# Mobile
POST /api/auth/login
X-Client-Type: mobile
→ body: { accessToken, refreshToken, tokenType, user }
```

---

## Build

```bash
./gradlew build -x test
```

JAR output: `build/libs/nitrotech-api-0.0.1-SNAPSHOT.jar`

---

## Cấu trúc dự án

```
src/main/java/com/nitrotech/api/
├── domain/           # Business logic
│   └── {module}/
│       ├── dto/          # Records — Commands, Results
│       ├── usecase/      # Business logic (@Service)
│       ├── repository/   # Interfaces
│       └── exception/    # Domain exceptions
├── infrastructure/   # Framework/Database layer
│   ├── persistence/  # JPA Entities, Repositories
│   ├── security/     # JWT, Redis token blacklist
│   └── mail/         # Resend email sender
├── application/      # HTTP layer
│   └── {module}/
│       ├── controller/   # REST Controllers
│       └── request/      # Request DTOs
└── shared/           # Shared kernel
    ├── config/       # Spring configs (Security, Redis, CORS)
    ├── exception/    # Global exception handler
    ├── response/     # ApiResponse wrapper
    └── util/         # CookieUtil, ...
```

Xem chi tiết kiến trúc: [.docs/ARCHITECTURE.md](.docs/ARCHITECTURE.md)

Xem coding standards: [.docs/CODING-STANDARDS.md](.docs/CODING-STANDARDS.md)

Xem database design: [.docs/DATABASE-DESIGN.md](.docs/DATABASE-DESIGN.md)

Xem API docs: [.docs/api/](.docs/api/)

---

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI spec: `http://localhost:8080/v3/api-docs`

File docs theo module (request/response thực tế):
```bash
# Capture docs từ server đang chạy
python scripts/capture_api_docs.py

# Gộp module auth thành 1 file để dùng với AI
python scripts/merge_api_docs.py auth
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

**Lỗi Flyway migration checksum mismatch**
```sql
-- Chạy trong DB
DELETE FROM flyway_schema_history WHERE version = '<version>';
```
Sau đó restart app.

**Lỗi Redis**
```bash
docker compose logs redis
```

**Java version không đúng**
```bash
java -version  # phải là 21+
```

**Email không gửi được**
- Kiểm tra `resend.api-key` trong `application-dev.yaml`
- Với `onboarding@resend.dev` chỉ gửi được đến email đã đăng ký Resend
- Verify domain tại resend.com/domains để gửi đến bất kỳ email nào

**Cookie không được set (web)**
- Đảm bảo fetch có `credentials: "include"`
- CORS `allowed-origins` phải khớp chính xác domain frontend (không dùng `*`)
- Local dev: `app.cookie.secure=false` (vì không có HTTPS)
