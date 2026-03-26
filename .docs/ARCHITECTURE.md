# Kiến trúc Domain-Oriented cho Spring Boot 4 + Java 21

## Tổng quan

Dự án áp dụng **Domain-Oriented Layered Architecture** — biến thể thực dụng của Clean Architecture / Hexagonal Architecture, được tối ưu cho Spring Boot 4 và Java 21.

Mục tiêu: Tách biệt hoàn toàn business logic (Domain) khỏi framework/database (Infrastructure).

---

## Cấu trúc thư mục

```
src/main/java/com/nitrotech/api/
│
├── domain/                          # Domain Layer — Java thuần, KHÔNG có Spring annotation
│   └── {module}/
│       ├── dto/                     # Data Transfer Objects (Java records)
│       ├── repository/              # Repository Interfaces (chỉ interface)
│       ├── usecase/                 # Use Cases (business logic)
│       └── exception/               # Domain Exceptions
│
├── infrastructure/                  # Infrastructure Layer — Framework/Database
│   ├── persistence/
│   │   ├── entity/                  # JPA Entities (@Entity)
│   │   ├── repository/              # Spring Data JPA repos + Repository implementations
│   │   └── mapper/                  # Entity ↔ Domain DTO mappers
│   └── external/                    # External service integrations (HTTP clients, etc.)
│
├── application/                     # Application Layer — HTTP
│   └── {module}/
│       ├── controller/              # REST Controllers
│       └── request/                 # HTTP Request/Response DTOs
│
└── shared/                          # Shared Kernel
    ├── config/                      # Spring configs (Security, OpenAPI, JPA, etc.)
    ├── exception/                   # Global exception handler (@RestControllerAdvice)
    └── util/                        # Utilities, helpers
```

---

## Luồng dữ liệu

```
HTTP Request
  → Controller (application layer)
    → Validate (Bean Validation / @Valid)
    → Map sang Domain DTO
    → Gọi UseCase (domain layer)
      → UseCase gọi Repository Interface
        → Repository Implementation (infrastructure layer)
          → JPA Entity / Spring Data
        ← Trả Domain DTO
      ← UseCase xử lý business logic
    ← Controller format response
  ← HTTP Response
```

---

## Nguyên tắc THÉP

### Domain Layer
- ✅ Java thuần — KHÔNG import bất kỳ annotation Spring nào
- ✅ Chứa toàn bộ business logic
- ✅ Chỉ định nghĩa Repository Interface, KHÔNG có implementation
- ✅ Dùng `record` Java 21 cho DTOs (immutable, compact)
- ❌ KHÔNG import `jakarta.persistence.*`
- ❌ KHÔNG dùng `@Component`, `@Service`, `@Repository`, `@Autowired`
- ❌ KHÔNG phụ thuộc vào Spring context

### Infrastructure Layer
- ✅ Nơi DUY NHẤT chứa JPA Entities
- ✅ Implement Repository Interfaces từ Domain
- ✅ Xử lý database queries, caching, external APIs
- ✅ Dùng Mapper để convert Entity ↔ Domain DTO

### Application Layer
- ✅ Chỉ làm 3 việc: Validate → DTO → UseCase → Response
- ✅ Dùng `@RestController`, `@Valid`, `ResponseEntity`
- ❌ KHÔNG viết business logic
- ❌ KHÔNG gọi JPA trực tiếp
- ❌ KHÔNG query database

---

## Ví dụ hoàn chỉnh — Module Category

### 1. Domain Layer

**DTO (Java Record)**
```java
// domain/category/dto/CreateCategoryCommand.java
package com.nitrotech.api.domain.category.dto;

public record CreateCategoryCommand(
    String name,
    String slug,
    String description,
    Long parentId,
    boolean isVisible
) {}
```

**Repository Interface**
```java
// domain/category/repository/CategoryRepository.java
package com.nitrotech.api.domain.category.repository;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;

import java.util.Optional;

public interface CategoryRepository {
    CategoryData create(CreateCategoryCommand command);
    Optional<CategoryData> findById(Long id);
    boolean exists(Long id);
    boolean isDescendantOf(Long potentialDescendantId, Long ancestorId);
}
```

**UseCase (Business Logic)**
```java
// domain/category/usecase/CreateCategoryUseCase.java
package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;

public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryData execute(CreateCategoryCommand command) {
        if (command.parentId() != null && !categoryRepository.exists(command.parentId())) {
            throw CategoryNotFoundException.withId(command.parentId());
        }
        return categoryRepository.create(command);
    }
}
```

**Domain Exception**
```java
// domain/category/exception/CategoryNotFoundException.java
package com.nitrotech.api.domain.category.exception;

public class CategoryNotFoundException extends RuntimeException {

    private CategoryNotFoundException(String message) {
        super(message);
    }

    public static CategoryNotFoundException withId(Long id) {
        return new CategoryNotFoundException("Category with ID " + id + " not found.");
    }
}
```

---

### 2. Infrastructure Layer

**JPA Entity**
```java
// infrastructure/persistence/entity/CategoryEntity.java
package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String slug;
    private String description;
    private Long parentId;
    private boolean isVisible;
}
```

**Spring Data JPA Repository**
```java
// infrastructure/persistence/repository/CategoryJpaRepository.java
package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {}
```

**Repository Implementation**
```java
// infrastructure/persistence/repository/CategoryRepositoryImpl.java
package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import com.nitrotech.api.infrastructure.persistence.mapper.CategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;
    private final CategoryMapper mapper;

    public CategoryRepositoryImpl(CategoryJpaRepository jpaRepository, CategoryMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CategoryData create(CreateCategoryCommand command) {
        CategoryEntity entity = mapper.toEntity(command);
        return mapper.toData(jpaRepository.save(entity));
    }

    @Override
    public Optional<CategoryData> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toData);
    }

    @Override
    public boolean exists(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean isDescendantOf(Long potentialDescendantId, Long ancestorId) {
        // implement tree traversal logic
        return false;
    }
}
```

---

### 3. Application Layer

**Controller**
```java
// application/category/controller/CategoryController.java
package com.nitrotech.api.application.category.controller;

import com.nitrotech.api.application.category.request.CreateCategoryRequest;
import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.usecase.CreateCategoryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;

    public CategoryController(CreateCategoryUseCase createCategoryUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
    }

    @PostMapping
    public ResponseEntity<CategoryData> create(@Valid @RequestBody CreateCategoryRequest request) {
        CreateCategoryCommand command = new CreateCategoryCommand(
            request.name(), request.slug(), request.description(),
            request.parentId(), request.isVisible()
        );
        return ResponseEntity.status(201).body(createCategoryUseCase.execute(command));
    }
}
```

**Request DTO**
```java
// application/category/request/CreateCategoryRequest.java
package com.nitrotech.api.application.category.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
    @NotBlank String name,
    @NotBlank String slug,
    String description,
    Long parentId,
    boolean isVisible
) {}
```

---

### 4. Shared Config — Đăng ký UseCase Bean

Vì UseCase là Java thuần (không có `@Service`), cần đăng ký thủ công qua `@Configuration`:

```java
// shared/config/CategoryConfig.java
package com.nitrotech.api.shared.config;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.domain.category.usecase.CreateCategoryUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoryConfig {

    @Bean
    public CreateCategoryUseCase createCategoryUseCase(CategoryRepository categoryRepository) {
        return new CreateCategoryUseCase(categoryRepository);
    }
}
```

---

## Checklist khi tạo module mới

- [ ] Tạo `record` DTO trong `domain/{module}/dto/`
- [ ] Tạo Repository Interface trong `domain/{module}/repository/`
- [ ] Tạo UseCase classes trong `domain/{module}/usecase/`
- [ ] Tạo Domain Exceptions trong `domain/{module}/exception/`
- [ ] Tạo JPA Entity trong `infrastructure/persistence/entity/`
- [ ] Tạo Spring Data JPA repo trong `infrastructure/persistence/repository/`
- [ ] Tạo Repository Implementation trong `infrastructure/persistence/repository/`
- [ ] Tạo Mapper trong `infrastructure/persistence/mapper/`
- [ ] Tạo Controller trong `application/{module}/controller/`
- [ ] Tạo Request DTOs trong `application/{module}/request/`
- [ ] Đăng ký UseCase beans trong `shared/config/{Module}Config.java`
- [ ] Tạo Flyway migration trong `resources/db/migration/`

---

## Modules cần implement

| Module | Mô tả |
|--------|-------|
| Category | Tree structure, circular reference validation |
| Product | Belongs to Category, has variants, specs JSON |
| Order | Aggregate root, OrderItems, transaction management |
| Banner | Active scope, date range, position filtering |
| Inventory | Stock management, adjustment, low stock alerts |
| User/Auth | Authentication, Authorization, JWT/Sanctum |

---

## Tham chiếu lý thuyết

- **Layered Architecture** (Domain → Application → Infrastructure)
- **Domain-Driven Design** (DDD) principles
- **Dependency Inversion Principle** (DIP) — Domain không phụ thuộc Infrastructure
- **Clean Architecture** — Dependency Rule: outer layers phụ thuộc inner layers, không ngược lại
- **Hexagonal Architecture** — Ports (interfaces) & Adapters (implementations)
