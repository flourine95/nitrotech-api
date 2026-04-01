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
```

| File                           | Commit | Mục đích                            |
| --------------------------------| --------| -------------------------------------|
| `application.yaml`             | ✅      | Default chung, dùng biến môi trường |
| `application-dev.yaml`         | ❌      | Local dev, chứa credentials         |
| `application-prod.yaml`        | ❌      | Production, chứa credentials        |
| `application-test.yaml`        | ✅      | CI/CD                               |
| `application-dev.example.yaml` | ✅      | Template cho team                   |

### 4. Chạy ứng dụng

```bash
./gradlew bootRun
```

Ứng dụng chạy tại `http://localhost:8080` — tự redirect sang Swagger UI.

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
├── domain/           # Business logic (Java thuần, không Spring annotation)
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
│       └── request/      # Request DTOs (records)
└── shared/           # Shared kernel
    ├── config/       # Spring configs (Security, Redis, CORS)
    ├── exception/    # Global exception handler
    └── response/     # ApiResponse wrapper
```

Xem chi tiết kiến trúc: [.docs/ARCHITECTURE.md](.docs/ARCHITECTURE.md)

Xem coding standards: [.docs/CODING-STANDARDS.md](.docs/CODING-STANDARDS.md)

Xem database design: [.docs/DATABASE-DESIGN.md](.docs/DATABASE-DESIGN.md)

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
docker compose ps        # kiểm tra containers
docker compose logs postgres  # xem logs postgres
```

**Lỗi Flyway migration**
- Xem log: `./gradlew bootRun --debug`
- Reset: `docker compose down -v && docker compose up -d`

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
