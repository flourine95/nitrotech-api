# Coding Standards — Nitrotech API

Comprehensive coding standards for the Nitrotech API Spring Boot project. This document consolidates all coding conventions, best practices, and guidelines.

---

## Table of Contents

1. [Core Rules](#core-rules)
2. [Java Standards](#java-standards)
3. [Database & JPA Standards](#database--jpa-standards)
4. [API Conventions](#api-conventions)
5. [Exception Handling](#exception-handling)
6. [Testing Strategy](#testing-strategy)

---

## Core Rules

Essential rules that apply to all code changes. These ensure code quality, maintainability, and consistency.

### Import Management

**Rule**: Always use explicit imports, never fully qualified names (FQN) in code.

```java
// GOOD - Proper import statement
import java.util.List;
public List<Product> findAll() { }

// BAD - Fully qualified name in code
public java.util.List<Product> findAll() { }
```

### Type Safety & Casting

**Rule**: Always use pattern matching with `instanceof` before casting.

```java
// GOOD - Pattern matching with instanceof
if (result[0] instanceof Number number) {
    double rating = number.doubleValue();
}

// BAD - Unsafe cast
Double rating = (Double) result[0];
```

### Stream API Best Practices

**Rule**: Prefer method references over simple lambda expressions.

```java
// GOOD - Method references
.map(this::toDto)
.filter(Objects::nonNull)

// BAD - Unnecessary lambda
.map(e -> toDto(e))
.filter(item -> item != null)
```

**Rule**: Extract complex stream predicates to named entity methods.

```java
// GOOD - Extracted predicate method
.filter(UserTokenEntity::isValid)

// In UserTokenEntity class
public boolean isValid() {
    return !used && !isExpired();
}

// BAD - Complex inline lambda
.filter(e -> !e.isUsed() && e.getExpiresAt().isAfter(LocalDateTime.now()))
```

### Logging Standards

**Rule**: Use SLF4J logger with parameterized messages. Never use `System.out` or `System.err`.

```java
// GOOD - Parameterized logging
log.debug("Processing message: {}", msg);
log.info("User {} logged in", user.getName());

// BAD - Console output and string concatenation
System.out.println("DEBUG: " + msg);
log.info("User: " + user.getName() + " logged in");
```

### DTO Mapping

**Rule**: Use private mapper methods for simple DTO conversions. Do not introduce MapStruct for basic mappings.

```java
// In service class
private ProductDto toDto(ProductEntity entity) {
    return ProductDto.builder()
        .id(entity.getId())
        .name(entity.getName())
        .price(entity.getPrice())
        .build();
}
```

---

## Java Standards

### Lombok Annotations

#### Entity Classes
**Rule**: Use only `@Getter`, `@Setter`, and `@NoArgsConstructor` on JPA entities.

```java
// GOOD - Correct entity pattern
@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}

// BAD - @Data causes issues with JPA
@Entity
@Data  // Causes LazyInitializationException and infinite loops
public class ProductEntity { }
```

**Why**: `@Data`, `@EqualsAndHashCode`, and `@ToString` can trigger lazy loading outside of transactions, causing exceptions.

#### Service Classes
**Rule**: Use `@RequiredArgsConstructor` for dependency injection.

```java
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
}
```

#### DTOs and Records
**Rule**: Do not use Lombok on record classes. Records are already immutable with auto-generated methods.

### Dependency Injection

**Rule**: Always use constructor injection via `@RequiredArgsConstructor`. Never use field or setter injection.

```java
// GOOD - Constructor injection (immutable, testable)
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;
}

// BAD - Field injection (not testable, mutable)
@Service
public class CreateProductUseCase {
    @Autowired
    private ProductRepository productRepository;
}
```

### Logging

**Rule**: Use `@Slf4j` with parameterized logging.

```java
@Slf4j
@Component
public class ProductDataSeeder {
    public void run(String... args) {
        log.info("Seeding {} products", TOTAL_PRODUCTS);
        log.debug("Processing product: {}", product.getName());
        log.error("Failed to seed products", e);
    }
}
```

**Log Levels**:
- `debug` - Development/troubleshooting details
- `info` - Important business events
- `warn` - Recoverable issues
- `error` - Failures requiring attention

### Null Safety

**Rule**: Always validate before casting or accessing potentially null values.

```java
// GOOD - Safe type checking with pattern matching
if (reviewStats != null && reviewStats.length >= 2) {
    if (reviewStats[0] instanceof Number number) {
        double rawRating = number.doubleValue();
        rating = rawRating > 0 ? roundRating(rawRating) : null;
    }
}

// GOOD - Null-safe property updates
if (command.name() != null) entity.setName(command.name());
if (command.price() != null) entity.setPrice(command.price());
```

### Entity Predicate Methods

**Rule**: Extract complex business logic into named boolean methods on entity classes.

```java
@Entity
public class OrderEntity {
    private String status;
    private LocalDateTime createdAt;
    
    public boolean isPending() {
        return "pending".equals(status);
    }
    
    public boolean canBeCancelled() {
        return isPending() || isConfirmed();
    }
    
    public boolean isExpired() {
        return isPending() && 
               createdAt.plusHours(24).isBefore(LocalDateTime.now());
    }
}
```

**Naming Conventions**:
- `is*` - State checks: `isPending()`, `isActive()`, `isExpired()`
- `has*` - Existence checks: `hasStock()`, `hasPrice()`
- `can*` - Permission checks: `canBeCancelled()`, `canBeEdited()`

---

## Database & JPA Standards

### Entity Design

**Rule**: Keep validation in Request DTOs, not in entity classes. Entities define database constraints only.

```java
// GOOD - Entity with database constraints only
@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    // No @NotBlank, @Size, @Email annotations here
}

// GOOD - Request DTO with business validation
public record CreateProductRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,
        
        @NotBlank(message = "Slug is required")
        String slug
) {}
```

### Soft Delete Pattern

**Rule**: This project uses soft deletes. All queries must filter `deletedAt IS NULL` for active records.

```java
// GOOD - Queries with soft delete filter
@Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
Optional<CategoryEntity> findActiveById(@Param("id") Long id);

// BAD - Missing soft delete filter
@Query("SELECT c FROM CategoryEntity c WHERE c.id = :id")
Optional<CategoryEntity> findById(@Param("id") Long id);
```

**Naming Conventions**:
- `findActive*` - Returns only non-deleted records
- `findDeleted*` - Returns only soft-deleted records
- `existsActive*` - Checks existence of non-deleted records

### Transaction Management

**Rule**: Use `@Transactional` for operations involving multiple database writes.

```java
// GOOD - Transaction for multi-step operation
@Repository
public class ProductRepositoryImpl {
    @Transactional
    public ProductData create(CreateProductCommand command) {
        ProductEntity saved = productJpa.save(entity);
        
        if (command.images() != null) {
            saveImages(saved.getId(), command.images());
        }
        if (command.variants() != null) {
            saveVariants(saved.getId(), command.variants());
        }
        return toData(saved);
    }
}
```

### Batch Queries

**Rule**: Use `IN` clause for batch operations to avoid N+1 query problems.

```java
// GOOD - Batch query with IN clause
@Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NULL")
List<CategoryEntity> findAllActiveByIds(@Param("ids") List<Long> ids);

// BAD - N+1 query problem
for (Product p : products) {
    ReviewStats stats = reviewRepo.getReviewStats(p.getId());
}
```

### Pagination

**Rule**: Use Spring Data's `Page<T>` and `Pageable` for all paginated queries.

```java
// GOOD - Modern pagination with Page
@Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL")
Page<ProductEntity> findAll(Pageable pageable);

// Controller usage
Pageable pageable = SortUtils.toPageable(
    page, size, sort, SORTABLE_FIELDS, "createdAt");
Page<ProductData> result = productRepository.findAll(filter, pageable);
return ResponseEntity.ok(ApiResult.paged(result));
```

---

## API Conventions

### URL Design

**Rule**: Use plural nouns with kebab-case for multi-word resources. All endpoints must be prefixed with `/api`.

```java
// GOOD - RESTful URL patterns
@RestController
@RequestMapping("/api/products")
public class ProductController { }

@RestController
@RequestMapping("/api/product-variants")  // Multi-word: kebab-case
public class ProductVariantController { }

// BAD - Anti-patterns
@RequestMapping("/api/product")              // Singular noun
@RequestMapping("/api/productCategories")    // camelCase
@RequestMapping("/api/createProduct")        // Verb in URL
```

### Request Validation

**Rule**: Validation messages must use sentence case (first word capitalized) with no period at the end.

```java
// GOOD - Proper validation messages
@NotBlank(message = "Name is required")
@Size(max = 255, message = "Name must be at most 255 characters")
@Email(message = "Email must be valid")

// BAD - Incorrect formatting
@NotBlank(message = "name is required")                          // Lowercase
@Size(max = 255, message = "Name Must Be At Most 255 Characters")  // Title case
@Email(message = "email must be valid.")                         // Period at end
```

### Error Codes

**Rule**: Error codes must use SCREAMING_SNAKE_CASE with format `ENTITY_ACTION` or `ENTITY_STATE`.

```java
// GOOD - Structured error codes
throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
throw new DomainException("INSUFFICIENT_STOCK", 
    "Insufficient stock. Available: " + available);

// BAD - Inconsistent formatting
throw new NotFoundException("ProductNotFound", ...);           // PascalCase
throw new ConflictException("product-slug-exists", ...);       // kebab-case
```

### Request Records

**Rule**: Format request records with one annotation per line, vertically aligned.

```java
// GOOD - Clean, readable formatting
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
```

### Boolean Fields in Requests

**Rule**: Use primitive `boolean` for create requests, wrapper `Boolean` for update requests.

```java
// GOOD - Correct boolean usage
public record CreateBrandRequest(
        String name,
        boolean active  // Primitive - defaults to false
) {}

public record UpdateBrandRequest(
        String name,
        Boolean active  // Wrapper - null means "don't update"
) {}
```

### Controller Patterns

**Rule**: Always return `ResponseEntity<ApiResult<T>>` with appropriate HTTP status codes.

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    @GetMapping
    public ResponseEntity<ApiResult<List<ProductData>>> list(...) {
        Page<ProductData> page = useCase.execute(filter, pageable);
        return ResponseEntity.ok(ApiResult.paged(page));
    }
    
    @PostMapping
    public ResponseEntity<ApiResult<ProductData>> create(
            @Valid @RequestBody CreateProductRequest req) {
        ProductData data = useCase.execute(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.created(data));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        useCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Product deleted successfully"));
    }
}
```

**HTTP Status Codes**:
- `200 OK` - Successful GET, PUT, DELETE
- `201 Created` - Successful POST
- `400 Bad Request` - Invalid parameters, business validation fail
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `405 Method Not Allowed` - Wrong HTTP method
- `409 Conflict` - Duplicate resource
- `415 Unsupported Media Type` - Wrong Content-Type
- `422 Unprocessable Entity` - Validation error, domain rule violation
- `500 Internal Server Error` - Unexpected error

**ApiResult Methods**:
- `ApiResult.ok(data)` - Single resource response
- `ApiResult.ok(message)` - Success message without data
- `ApiResult.paged(page)` - Paginated list response
- `ApiResult.created(data)` - Created resource response

---

## Exception Handling

### Custom Exceptions

The project uses a hierarchy of custom exceptions:

```java
// Base exceptions
DomainException (422)           // Abstract base for domain errors
├── NotFoundException (404)     // Resource not found
├── ConflictException (409)     // Duplicate resource
├── BadRequestException (400)   // Business validation fail
└── ForbiddenException (403)    // Access denied
```

### Exception Usage

```java
// NotFoundException - Resource not found
throw new NotFoundException("PRODUCT_NOT_FOUND", 
    "Product with ID " + id + " not found");

// ConflictException - Duplicate resource
throw new ConflictException("EMAIL_ALREADY_EXISTS", 
    "Email " + email + " is already registered");

// BadRequestException - Business validation fail
throw new BadRequestException("INVALID_SORT_FIELD", 
    "Sort field not allowed: " + field);

// ForbiddenException - Access denied
throw new ForbiddenException("ADDRESS_ACCESS_DENIED", 
    "This address does not belong to you");

// DomainException - General domain error
throw new DomainException("INSUFFICIENT_STOCK", 
    "Insufficient stock. Available: " + available) {};
```

### GlobalExceptionHandler

All exceptions are handled by `@RestControllerAdvice`:

| Exception | Status | Code | When |
|-----------|--------|------|------|
| NotFoundException | 404 | ENTITY_NOT_FOUND | Resource not found |
| NoResourceFoundException | 404 | ENDPOINT_NOT_FOUND | Endpoint not found |
| HttpMessageNotReadableException | 400 | INVALID_REQUEST_BODY | JSON malformed |
| MethodArgumentTypeMismatchException | 400 | INVALID_PARAMETER | Type mismatch |
| HttpRequestMethodNotSupportedException | 405 | METHOD_NOT_ALLOWED | Wrong HTTP method |
| HttpMediaTypeNotSupportedException | 415 | UNSUPPORTED_MEDIA_TYPE | Wrong Content-Type |
| MissingServletRequestParameterException | 400 | MISSING_PARAMETER | Missing required param |
| ConflictException | 409 | ENTITY_EXISTS | Duplicate resource |
| BadRequestException | 400 | VALIDATION_ERROR | Business validation fail |
| ForbiddenException | 403 | ACCESS_DENIED | Access denied |
| DomainException | 422 | DOMAIN_ERROR | Domain rule violation |
| MethodArgumentNotValidException | 422 | VALIDATION_ERROR | Validation annotation fail |
| Exception | 500 | INTERNAL_ERROR | Unexpected error |

---

## Testing Strategy

### Unit Test — UseCase (Domain Layer)
```java
@Test
void shouldThrowWhenParentNotFound() {
    CategoryRepository mockRepo = mock(CategoryRepository.class);
    when(mockRepo.exists(99L)).thenReturn(false);

    CreateCategoryUseCase useCase = new CreateCategoryUseCase(mockRepo);
    CreateCategoryCommand command = new CreateCategoryCommand(...);

    assertThrows(CategoryNotFoundException.class, 
        () -> useCase.execute(command));
}
```

### Integration Test — Repository
```java
@DataJpaTest
class CategoryRepositoryImplTest {
    @Autowired CategoryJpaRepository jpaRepository;

    @Test
    void shouldSaveAndFindCategory() {
        // ...
    }
}
```

### E2E Test — Controller
```java
@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void shouldCreateCategory() throws Exception {
        mockMvc.perform(post("/api/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Electronics", "slug": "electronics"}
            """))
            .andExpect(status().isCreated());
    }
}
```

---

## Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| UseCase | `{Action}{Module}UseCase` | `CreateCategoryUseCase` |
| Command DTO | `{Action}{Module}Command` | `CreateCategoryCommand` |
| Response DTO | `{Module}Data` | `CategoryData` |
| Repository Interface | `{Module}Repository` | `CategoryRepository` |
| Repository Impl | `{Module}RepositoryImpl` | `CategoryRepositoryImpl` |
| JPA Entity | `{Module}Entity` | `CategoryEntity` |
| JPA Repo | `{Module}JpaRepository` | `CategoryJpaRepository` |
| Controller | `{Module}Controller` | `CategoryController` |
| Request DTO | `{Action}{Module}Request` | `CreateCategoryRequest` |
| Exception | `{Module}{Reason}Exception` | `CategoryNotFoundException` |

---

## Validation Checklist

Before committing code, verify:

- [ ] No fully qualified names (FQN) in code - all types properly imported
- [ ] All casts use `instanceof` pattern matching
- [ ] `@Transactional` applied to multi-step database operations
- [ ] Batch queries used - no N+1 query problems
- [ ] Soft delete filter (`deletedAt IS NULL`) included in custom queries
- [ ] SLF4J logger used - no `System.out` or `System.err`
- [ ] Complex stream predicates extracted to entity methods
- [ ] Method references used instead of simple lambdas
- [ ] Private mapper methods used for DTOs
- [ ] Validation messages use sentence case without period
- [ ] Error codes use SCREAMING_SNAKE_CASE
- [ ] Code compiles without warnings or errors

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture and design patterns
- [DATABASE-DESIGN.md](./DATABASE-DESIGN.md) - Database schema and design
