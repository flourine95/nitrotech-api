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
git clone <repository-url>
cd nitrotech-api
```

### 2. Khởi động database

Dự án dùng Docker Compose để chạy MySQL. Spring Boot DevTools sẽ tự động start container khi chạy app.

Nếu muốn start thủ công:

```bash
docker compose up -d
```

Database mặc định:
- Host: `localhost:3306`
- Database: `mydatabase`
- User: `myuser`
- Password: `secret`

### 3. Cấu hình môi trường

Tạo file `application-dev.yaml` từ template (file này đã được `.gitignore`):

```bash
cp src/main/resources/application-dev.example.yaml src/main/resources/application-dev.yaml
```

Sau đó chỉnh thông tin kết nối trong `application-dev.yaml` cho phù hợp với máy local.

| File | Commit | Mục đích |
|------|--------|---------|
| `application.yaml` | ✅ | Default chung, dùng biến môi trường |
| `application-dev.yaml` | ❌ | Local dev, chứa credentials |
| `application-prod.yaml` | ❌ | Production, chứa credentials |
| `application-test.yaml` | ✅ | CI/CD, dùng H2 in-memory |
| `application-dev.example.yaml` | ✅ | Template cho team |

### 4. Chạy ứng dụng

```bash
# Dev (mặc định)
./gradlew bootRun

# Chỉ định profile rõ ràng
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Hoặc trên Windows:

```bash
gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Ứng dụng sẽ chạy tại: `http://localhost:8080`

---

## Chạy theo môi trường

```bash
# Local dev
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production (Docker)
docker run -e SPRING_PROFILES_ACTIVE=prod \
           -e DB_URL=jdbc:mysql://prod-db:3306/nitrotech \
           -e DB_USERNAME=user \
           -e DB_PASSWORD=secret \
           nitrotech-api

# Test (tự load application-test.yaml, dùng H2)
./gradlew test
```

---

## API Documentation

Sau khi chạy ứng dụng, truy cập Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

---

## Build

```bash
# Build JAR
./gradlew build

# Build bỏ qua tests
./gradlew build -x test
```

JAR output: `build/libs/nitrotech-api-0.0.1-SNAPSHOT.jar`

---

## Chạy Tests

```bash
# Chạy tất cả tests
./gradlew test

# Chạy test cụ thể
./gradlew test --tests "com.nitrotech.api.domain.category.*"
```

---

## Chạy bằng Docker (Production)

```bash
# Build image
docker build -t nitrotech-api .

# Chạy với docker compose
docker compose -f compose.yaml up
```

---

## Cấu trúc dự án

```
src/main/java/com/nitrotech/api/
├── domain/           # Business logic (Java thuần, không Spring)
├── infrastructure/   # JPA Entities, Repository implementations
├── application/      # REST Controllers, Request DTOs
└── shared/           # Config, Exception handlers, Utilities
```

Xem chi tiết kiến trúc: [.docs/ARCHITECTURE.md](.docs/ARCHITECTURE.md)

Xem coding standards: [.docs/CODING-STANDARDS.md](.docs/CODING-STANDARDS.md)

---

## Database Migrations

Dự án dùng **Flyway** để quản lý schema. Migration files đặt tại:

```
src/main/resources/db/migration/
```

Naming convention: `V{version}__{description}.sql`

Flyway tự động chạy migrations khi ứng dụng khởi động.

---

## Troubleshooting

**Lỗi kết nối database**
- Kiểm tra Docker container đang chạy: `docker compose ps`
- Kiểm tra port 3306 không bị chiếm: `netstat -an | grep 3306`

**Lỗi Flyway migration**
- Xem log chi tiết: `./gradlew bootRun --debug`
- Reset database (dev only): xóa bảng `flyway_schema_history` và chạy lại

**Java version không đúng**
```bash
java -version  # phải là 21+
```
