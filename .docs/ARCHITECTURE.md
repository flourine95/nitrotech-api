# Architecture — Nitrotech API

## Overview

The Nitrotech API follows a **Layered Architecture** pattern with clear separation of concerns across three main layers:

- **Application Layer** - HTTP/REST interface (Controllers, Request DTOs)
- **Domain Layer** - Business logic (Use Cases, Domain DTOs, Repository Interfaces)
- **Infrastructure Layer** - Technical implementation (JPA Entities, Repository Implementations, External Services)

This architecture ensures business logic remains independent of frameworks and infrastructure concerns.

---

## Project Structure

```
src/main/java/com/nitrotech/api/
│
├── application/                     # Application Layer — HTTP Interface
│   └── {module}/
│       ├── controller/              # REST Controllers (@RestController)
│       └── request/                 # HTTP Request DTOs (validation)
│
├── domain/                          # Domain Layer — Business Logic
│   └── {module}/
│       ├── dto/                     # Domain DTOs (Commands, Data, Filters)
│       ├── repository/              # Repository Interfaces (ports)
│       ├── usecase/                 # Use Cases (@Service)
│       └── exception/               # Domain Exceptions (optional)
│
├── infrastructure/                  # Infrastructure Layer — Technical Implementation
│   ├── persistence/
│   │   ├── entity/                  # JPA Entities (@Entity)
│   │   ├── repository/              # JPA Repositories + Implementations
│   │   ├── mapper/                  # Entity ↔ Domain DTO mappers
│   │   └── spec/                    # JPA Specifications for dynamic queries
│   ├── mail/                        # Email service implementation
│   ├── security/                    # Security filters, authentication
│   └── storage/                     # File storage (Cloudinary)
│
└── shared/                          # Shared Kernel — Cross-cutting concerns
    ├── config/                      # Spring configurations
    ├── exception/                   # Global exception handler
    ├── request/                     # Shared request DTOs (pagination)
    ├── response/                    # Shared response wrappers (ApiResult)
    ├── security/                    # Security utilities
    ├── seeder/                      # Database seeders
    └── util/                        # Utilities, helpers
```

---

## Data Flow

```
HTTP Request
  ↓
Controller (application layer)
  ├─→ Validate (@Valid)
  ├─→ Map Request → Command DTO
  └─→ Call UseCase
        ↓
UseCase (domain layer)
  ├─→ Business logic
  ├─→ Call Repository Interface
  │     ↓
  │   Repository Implementation (infrastructure layer)
  │     ├─→ Map Command → Entity
  │     ├─→ JPA operations
  │     └─→ Map Entity → Domain DTO
  │     ↓
  └─→ Return Domain DTO
        ↓
Controller
  ├─→ Wrap in ApiResult
  └─→ Return ResponseEntity
        ↓
HTTP Response
```

---

## Layer Responsibilities

### Application Layer

**Purpose**: HTTP interface and request/response handling

**Contains**:
- `@RestController` - REST endpoints
- Request DTOs with `@Valid` annotations
- Request → Command mapping
- Response formatting with `ApiResult`

**Rules**:
- ✅ Handle HTTP concerns (status codes, headers, validation)
- ✅ Map between HTTP DTOs and Domain DTOs
- ✅ Call Use Cases
- ❌ NO business logic
- ❌ NO direct database access
- ❌ NO JPA entities

**Example**:
```java
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    
    private final GetBrandsUseCase getBrandsUseCase;
    
    @GetMapping
    public ResponseEntity<ApiResult<List<BrandData>>> list(
            @Valid @ModelAttribute BrandListRequest request,
            @Valid @ModelAttribute PaginationRequest pagination
    ) {
        Pageable pageable = SortUtils.toPageable(
            pagination.getPage(), pagination.getSize(),
            pagination.getSort(), SORTABLE_FIELDS, "createdAt"
        );
        
        BrandFilter filter = request.toFilter();
        Page<BrandData> page = getBrandsUseCase.execute(filter, pageable);
        
        return ResponseEntity.ok(ApiResult.paged(page));
    }
}
```

---

### Domain Layer

**Purpose**: Business logic and domain rules

**Contains**:
- Use Cases (`@Service`) - Business operations
- Domain DTOs (`record`) - Commands, Data, Filters
- Repository Interfaces - Data access contracts
- Domain Exceptions - Business rule violations

**Rules**:
- ✅ Contain ALL business logic
- ✅ Define repository interfaces (ports)
- ✅ Use domain DTOs (immutable records)
- ✅ Throw domain exceptions
- ❌ NO Spring Data annotations
- ❌ NO JPA entities
- ❌ NO HTTP concerns
- ❌ NO infrastructure dependencies

**Example**:
```java
// Use Case
@Service
@RequiredArgsConstructor
public class GetBrandUseCase {
    
    private final BrandRepository brandRepository;
    
    public BrandData execute(String idOrSlug) {
        try {
            Long id = Long.parseLong(idOrSlug);
            return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND",
                    "Brand with ID " + id + " not found"));
        } catch (NumberFormatException e) {
            return brandRepository.findBySlug(idOrSlug)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND",
                    "Brand with slug '" + idOrSlug + "' not found"));
        }
    }
}

// Repository Interface
public interface BrandRepository {
    Optional<BrandData> findById(Long id);
    Optional<BrandData> findBySlug(String slug);
    Page<BrandData> findAll(BrandFilter filter, Pageable pageable);
    BrandData create(CreateBrandCommand command);
    BrandData update(UpdateBrandCommand command);
    void delete(Long id);
}

// Domain DTO
public record BrandData(
    Long id,
    String name,
    String slug,
    String logo,
    String description,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

### Infrastructure Layer

**Purpose**: Technical implementation and external integrations

**Contains**:
- JPA Entities (`@Entity`) - Database mapping
- JPA Repositories (`JpaRepository`) - Spring Data
- Repository Implementations (`@Repository`) - Implement domain interfaces
- Mappers - Entity ↔ Domain DTO conversion
- Specifications - Dynamic query building
- External services - Email, storage, etc.

**Rules**:
- ✅ Implement repository interfaces from domain
- ✅ Use JPA entities for database operations
- ✅ Map between entities and domain DTOs
- ✅ Handle database queries and transactions
- ❌ NO business logic
- ❌ NO HTTP concerns

**Example**:
```java
// JPA Entity
@Entity
@Table(name = "brands")
@Getter @Setter @NoArgsConstructor
public class BrandEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    private String logo;
    private String description;
    private boolean active;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

// JPA Repository
public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
    @Query("SELECT b FROM BrandEntity b WHERE b.slug = :slug AND b.deletedAt IS NULL")
    Optional<BrandEntity> findActiveBySlug(@Param("slug") String slug);
}

// Repository Implementation
@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {
    
    private final BrandJpaRepository jpa;
    
    @Override
    public Optional<BrandData> findById(Long id) {
        return jpa.findById(id)
            .filter(e -> e.getDeletedAt() == null)
            .map(this::toData);
    }
    
    @Override
    public Optional<BrandData> findBySlug(String slug) {
        return jpa.findActiveBySlug(slug)
            .map(this::toData);
    }
    
    private BrandData toData(BrandEntity e) {
        return new BrandData(
            e.getId(), e.getName(), e.getSlug(),
            e.getLogo(), e.getDescription(), e.isActive(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
```

---

### Shared Layer

**Purpose**: Cross-cutting concerns and utilities

**Contains**:
- Spring configurations (`@Configuration`)
- Global exception handler (`@RestControllerAdvice`)
- Shared request/response DTOs
- Security utilities
- Database seeders
- Utility classes

**Example**:
```java
// Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(404, ex.getCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                field -> field.getDefaultMessage() != null 
                    ? field.getDefaultMessage() 
                    : "Invalid value"
            ));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse.withErrors(422, "VALIDATION_ERROR", 
                "Validation failed", errors));
    }
}

// Shared Response Wrapper
public record ApiResult<T>(
    T data,
    String message,
    PaginationMeta meta
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(data, null, null);
    }
    
    public static ApiResult<Void> ok(String message) {
        return new ApiResult<>(null, message, null);
    }
    
    public static <T> ApiResult<List<T>> paged(Page<T> page) {
        return new ApiResult<>(
            page.getContent(),
            null,
            new PaginationMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
            )
        );
    }
}
```

---

## Key Patterns

### Soft Delete Pattern

All entities support soft delete via `deletedAt` timestamp:

```java
// Entity
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

// Repository - Always filter deleted records
@Query("SELECT b FROM BrandEntity b WHERE b.id = :id AND b.deletedAt IS NULL")
Optional<BrandEntity> findActiveById(@Param("id") Long id);

// Soft delete operation
public void delete(Long id) {
    BrandEntity entity = jpa.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", 
            "Brand not found"));
    entity.setDeletedAt(LocalDateTime.now());
    jpa.save(entity);
}

// Restore operation
public void restore(Long id) {
    BrandEntity entity = jpa.findById(id)
        .filter(e -> e.getDeletedAt() != null)
        .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", 
            "Deleted brand not found"));
    entity.setDeletedAt(null);
    jpa.save(entity);
}
```

### Pagination Pattern

Use Spring Data's `Page<T>` and `Pageable`:

```java
// Controller
Pageable pageable = SortUtils.toPageable(
    page, size, sort, SORTABLE_FIELDS, "createdAt");
Page<BrandData> result = brandRepository.findAll(filter, pageable);
return ResponseEntity.ok(ApiResult.paged(result));

// Repository
@Query("SELECT b FROM BrandEntity b WHERE b.deletedAt IS NULL")
Page<BrandEntity> findAll(Pageable pageable);
```

### Validation Pattern

Validation at HTTP layer, not in entities:

```java
// Request DTO with validation
public record CreateBrandRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", 
                 message = "Slug must be lowercase letters, numbers and hyphens")
        String slug,

        String description,
        boolean active
) {}

// Entity - Only database constraints
@Entity
@Table(name = "brands")
public class BrandEntity {
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
}
```

### Exception Hierarchy

```
DomainException (abstract, 422)
├── NotFoundException (404)
├── ConflictException (409)
├── BadRequestException (400)
└── ForbiddenException (403)
```

All handled by `GlobalExceptionHandler` with consistent error responses.

---

## Module Checklist

When creating a new module:

- [ ] **Domain Layer**
  - [ ] Create DTOs in `domain/{module}/dto/`
  - [ ] Create Repository Interface in `domain/{module}/repository/`
  - [ ] Create Use Cases in `domain/{module}/usecase/`
  - [ ] Create Domain Exceptions (if needed) in `domain/{module}/exception/`

- [ ] **Infrastructure Layer**
  - [ ] Create JPA Entity in `infrastructure/persistence/entity/`
  - [ ] Create JPA Repository in `infrastructure/persistence/repository/`
  - [ ] Create Repository Implementation in `infrastructure/persistence/repository/`
  - [ ] Create Mapper (if complex) in `infrastructure/persistence/mapper/`
  - [ ] Create Specification (if needed) in `infrastructure/persistence/spec/`

- [ ] **Application Layer**
  - [ ] Create Controller in `application/{module}/controller/`
  - [ ] Create Request DTOs in `application/{module}/request/`

- [ ] **Database**
  - [ ] Create Flyway migration in `resources/db/migration/`

---

## Modules

| Module | Description |
|--------|-------------|
| Address | User shipping addresses with default address support |
| Auth | Authentication, registration, email verification, password reset |
| Banner | Promotional banners with position and date range filtering |
| Brand | Product brands with soft delete |
| Cart | Shopping cart with variant-based items |
| Category | Hierarchical category tree with drag-and-drop reordering |
| Inventory | Stock management with low stock alerts |
| Order | Order processing with status tracking |
| Product | Products with variants, images, and specifications |
| Promotion | Discount codes with usage tracking |
| Review | Product reviews with approval workflow |
| Upload | Cloudinary integration for image management |
| Wishlist | User wishlist functionality |

---

## Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 4.0 | Application framework |
| Spring Data JPA | - | ORM and data access |
| Spring Security | 6 | Authentication and authorization |
| Spring Session Redis | - | Session management |
| PostgreSQL | 17 | Primary database |
| Redis | 7 | Session store and cache |
| Flyway | - | Database migrations |
| Lombok | - | Reduce boilerplate |
| MapStruct | 1.6 | Object mapping (optional) |
| Springdoc OpenAPI | 3.0 | API documentation |
| Cloudinary | - | Image storage |

---

## Related Documentation

- [CODING-STANDARDS.md](./CODING-STANDARDS.md) - Coding conventions and best practices
- [DATABASE-DESIGN.md](./DATABASE-DESIGN.md) - Database schema and design
