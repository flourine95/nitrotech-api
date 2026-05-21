---
inclusion: manual
---

# Java Coding Standards

Comprehensive Java conventions for the Nitrotech API Spring Boot project. For quick reference, see `#core-rules`.

## Lombok Annotations

### Entity Classes
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

// BAD - These annotations break lazy loading
@Entity
@Getter @Setter
@EqualsAndHashCode  // Breaks with lazy-loaded associations
@ToString           // Triggers lazy loading, causes exceptions
public class ProductEntity { }
```

**Why**: JPA entities with lazy-loaded associations require careful handling. `@Data`, `@EqualsAndHashCode`, and `@ToString` can trigger lazy loading outside of transactions, causing exceptions.

### Service Classes
**Rule**: Use `@RequiredArgsConstructor` for dependency injection in services and use cases.

```java
// GOOD - Constructor injection via Lombok
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
}
```

### DTOs and Records
**Rule**: Do not use Lombok on record classes. Records are already immutable with auto-generated methods.

**Summary**:
- **Entities**: `@Getter @Setter @NoArgsConstructor` only
- **Services/UseCases**: `@RequiredArgsConstructor` for dependency injection
- **DTOs/Records**: No Lombok needed
- **Never**: `@Data`, `@EqualsAndHashCode`, `@ToString` on entities

## Dependency Injection

**Rule**: Always use constructor injection via `@RequiredArgsConstructor`. Never use field or setter injection.

```java
// GOOD - Constructor injection (immutable, testable)
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
}

// BAD - Field injection (not testable, mutable)
@Service
public class CreateProductUseCase {
    @Autowired
    private ProductRepository productRepository;
}

// BAD - Setter injection (mutable, verbose)
@Service
public class CreateProductUseCase {
    private ProductRepository productRepository;
    
    @Autowired
    public void setProductRepository(ProductRepository repo) {
        this.productRepository = repo;
    }
}
```

**Why**: Constructor injection ensures immutability, makes dependencies explicit, and enables easier unit testing without Spring context.

## Logging

**Rule**: Use `@Slf4j` with parameterized logging. Never use `System.out` or `System.err`.

```java
// GOOD - Parameterized logging
@Slf4j
@Component
public class ProductDataSeeder {
    public void run(String... args) {
        if (productJpa.count() > 0) {
            log.info("Products already exist, skipping seed");
            return;
        }
        log.info("Seeding {} products", TOTAL_PRODUCTS);
        log.debug("Processing product: {}", product.getName());
        log.error("Failed to seed products", e);
    }
}

// BAD - Console output
System.out.println("DEBUG: " + msg);
System.err.println("ERROR: " + error);

// BAD - String concatenation (inefficient)
log.info("Processing " + product.getName());
```

**Log Levels**:
- `debug` - Development/troubleshooting details
- `info` - Important business events (startup, major operations)
- `warn` - Recoverable issues (deprecated API usage, fallback behavior)
- `error` - Failures requiring attention

**Best Practices**:
- Don't over-log: Avoid logging every method call
- Skip logging in simple CRUD operations
- Log at service/use case level, not in repositories
- Use parameterized messages to avoid string concatenation overhead

## Null Safety

**Rule**: Always validate before casting or accessing potentially null values.

```java
// GOOD - Safe type checking with pattern matching
if (reviewStats != null && reviewStats.length >= 2) {
    if (reviewStats[0] instanceof Number number) {
        double rawRating = number.doubleValue();
        rating = rawRating > 0 ? roundRating(rawRating) : null;
    }
}

// BAD - Unsafe cast (throws ClassCastException)
Double rating = (Double) reviewStats[0];

// GOOD - Null-safe property updates
if (command.name() != null) entity.setName(command.name());
if (command.price() != null) entity.setPrice(command.price());

// BAD - No null check (throws NullPointerException)
entity.setName(command.name());
```

**Checklist**:
- Check `instanceof` before casting
- Check null before accessing properties
- Check array/collection bounds before accessing elements
- Prefer `null` over `0` for "no data" in API responses

## Entity Predicate Methods

**Rule**: Extract complex business logic into named boolean methods on entity classes.

```java
// GOOD - Clean predicate methods
@Entity
public class OrderEntity {
    private String status;
    private LocalDateTime createdAt;
    
    public boolean isPending() {
        return "pending".equals(status);
    }
    
    public boolean isConfirmed() {
        return "confirmed".equals(status);
    }
    
    public boolean canBeCancelled() {
        return isPending() || isConfirmed();
    }
    
    public boolean isExpired() {
        return isPending() && 
               createdAt.plusHours(24).isBefore(LocalDateTime.now());
    }
}

// Usage in service
@Service
public class CancelOrderUseCase {
    public void execute(Long id) {
        OrderEntity order = orderRepository.findById(id).orElseThrow();
        if (!order.canBeCancelled()) {
            throw new DomainException("ORDER_CANNOT_CANCEL", 
                "Cannot cancel order") {};
        }
        orderRepository.updateStatus(id, "cancelled");
    }
}

// BAD - Complex inline conditions
if ((order.getStatus().equals("pending") || 
     order.getStatus().equals("confirmed")) && 
    order.getCreatedAt().plusHours(24).isAfter(LocalDateTime.now())) {
    // ...
}
```

**Naming Conventions**:
- `is*` - State checks: `isPending()`, `isActive()`, `isExpired()`, `isValid()`
- `has*` - Existence checks: `hasStock()`, `hasPrice()`, `hasChildren()`
- `can*` - Permission/capability checks: `canBeCancelled()`, `canBeEdited()`

**Guidelines**:
- Keep methods pure (no side effects)
- Always return boolean, never null
- Compose simple predicates into complex ones
- No repository calls inside entity methods
- Use with method references in streams: `.filter(Entity::isValid)`

## Method Naming

**Rule**: Use clear, descriptive method names with appropriate verb prefixes.

```java
// GOOD - Clear and specific
private ProductData toListData(ProductEntity e) { }
private ProductData toDetailData(ProductEntity e) { }
private boolean isNotFound(Long id) { }
private String getFailureReason(Long id) { }

// BAD - Ambiguous
private ProductData toData(ProductEntity e) { }
private ProductData convert(ProductEntity e) { }
private boolean check(Long id) { }
```

**Verb Prefixes**:
- `get/find` - Retrieve data
- `create/save` - Persist new data
- `update` - Modify existing data
- `delete/remove` - Remove data
- `to/from` - Convert/transform data
- `is/has/can` - Boolean checks

**Suffixes for Disambiguation**:
- `toListData` vs `toDetailData` - Different DTO representations
- `findActive` vs `findDeleted` - Different query scopes

## Exception Handling

**Rule**: Use custom domain exceptions with structured error codes and messages.

```java
// GOOD - Structured exceptions
throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
throw new DomainException("ORDER_NOT_DELIVERED", 
    "You can only review after order is delivered") {};

// BAD - Generic exceptions
throw new RuntimeException("Not found");
throw new Exception("Error");
```

**Exception Types**:
- `NotFoundException` - Resource not found (404)
- `ConflictException` - Business rule violation (409)
- `DomainException` - General domain logic error (400)

**Error Code Format**:
- Use SCREAMING_SNAKE_CASE
- Pattern: `ENTITY_ACTION` or `ENTITY_STATE`
- Examples: `PRODUCT_NOT_FOUND`, `ORDER_CANNOT_CANCEL`, `INSUFFICIENT_STOCK`

**Note**: `DomainException` is abstract, so instantiate with `{}` at the end.

## Import Organization

**Rule**: Always use explicit imports. Never use fully qualified names (FQN) in code.

```java
// GOOD - Explicit imports
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public List<Product> findAll() {
    Map<Long, String> map = new HashMap<>();
}

// BAD - Fully qualified names in code
public java.util.List<Product> findAll() {
    java.util.Map<Long, String> map = new java.util.HashMap<>();
}

// ACCEPTABLE - FQN only for name conflicts
import java.util.Date;
private Date utilDate;
private java.sql.Date sqlDate;  // Conflict resolved with FQN
```

**Import Grouping** (top to bottom):
1. Java standard library (`java.*`, `javax.*`)
2. Third-party libraries (Spring, Lombok, etc.)
3. Project packages (`com.nitrotech.api.*`)

**Best Practices**:
- Remove unused imports
- Avoid wildcard imports (`import java.util.*`) when possible
- Use FQN only when absolutely necessary (name conflicts)
