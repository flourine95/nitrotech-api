---
inclusion: manual
---

# Java Coding Standards

Java conventions for nitrotech-api. Core rules in `#core-rules`.

## Lombok Usage

### Entities - Use @Getter @Setter @NoArgsConstructor

```java
# GOOD - Current project pattern
@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}

# BAD - Never use @Data on entities
@Entity
@Data  // Causes LazyInitializationException, infinite loops
public class ProductEntity { }

# BAD - Never use @EqualsAndHashCode or @ToString on entities
@Entity
@Getter @Setter
@EqualsAndHashCode  // Breaks with lazy loading
@ToString  // Triggers lazy loading
public class ProductEntity { }
```

**Rules:**
- Entities: `@Getter @Setter @NoArgsConstructor` only
- Services/UseCases: `@RequiredArgsConstructor` for DI
- DTOs/Records: No Lombok needed
- Never `@Data`, `@EqualsAndHashCode`, `@ToString` on entities

---

## Dependency Injection

### Use Constructor Injection via @RequiredArgsConstructor

```java
# GOOD - Current project pattern
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
}

# BAD - Field injection
@Service
public class CreateProductUseCase {
    @Autowired
    private ProductRepository productRepository;
}

# BAD - Setter injection
@Service
public class CreateProductUseCase {
    private ProductRepository productRepository;
    
    @Autowired
    public void setProductRepository(ProductRepository repo) {
        this.productRepository = repo;
    }
}
```

**Rules:**
- Use `@RequiredArgsConstructor` + `private final` fields
- Never use `@Autowired` on fields
- Constructor injection enables immutability and easier testing

---

## Logging

### Use @Slf4j and Parameterized Logging

```java
# GOOD - Current project pattern
@Slf4j
@Component
public class ProductDataSeeder {
    public void run(String... args) {
        if (productJpa.count() > 0) {
            log.info("Products already exist, skipping seed");
            return;
        }
        log.info("Seeding {} products...", TOTAL_PRODUCTS);
        log.debug("Processing product: {}", product.getName());
        log.error("Failed to seed: {}", e.getMessage());
    }
}

# BAD - System.out
System.out.println("DEBUG: " + msg);
System.err.println("ERROR: " + error);

# BAD - String concatenation
log.info("Processing " + product.getName());  // Creates string even if log disabled
```

**Rules:**
- Use `@Slf4j` annotation
- Use parameterized logging: `log.info("Message: {}", value)`
- Levels: `debug` (dev), `info` (important events), `warn` (recoverable), `error` (failures)
- Never use `System.out.println` or `System.err.println`
- **Don't over-log**: Only log important events, not every method call
- Avoid logging in simple CRUD operations (create, update, delete)
- Log at service/use case level for business operations, not in repositories

---

## Null Safety

### Always Check Before Casting or Accessing

```java
# GOOD - Safe type checking
if (reviewStats != null && reviewStats.length >= 2) {
    if (reviewStats[0] instanceof Number number) {
        double rawRating = number.doubleValue();
        rating = rawRating > 0 ? roundRating(rawRating) : null;
    }
}

# BAD - Unsafe casting
Double rating = (Double) reviewStats[0];

# GOOD - Null-safe property access
if (command.name() != null) entity.setName(command.name());
if (command.price() != null) entity.setPrice(command.price());

# BAD - No null check
entity.setName(command.name());  // NPE if null
```

**Rules:**
- Check `instanceof` before casting
- Check null before accessing properties
- Check array length before accessing elements
- Prefer `null` over `0` for "no data" in API responses

---

## Predicate Methods

### Add Boolean Helper Methods to Entities

```java
# GOOD - Predicate methods
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
        return isPending() && createdAt.plusHours(24).isBefore(LocalDateTime.now());
    }
}

# Usage in Service
@Service
public class CancelOrderUseCase {
    public void execute(Long id) {
        OrderEntity order = orderRepository.findById(id).orElseThrow(...);
        if (!order.canBeCancelled()) {
            throw new DomainException("ORDER_CANNOT_CANCEL", "Cannot cancel order");
        }
        orderRepository.updateStatus(id, "cancelled");
    }
}

# BAD - Complex conditions in Service
if ((order.getStatus().equals("pending") || order.getStatus().equals("confirmed")) 
    && order.getCreatedAt().plusHours(24).isAfter(LocalDateTime.now())) {
    // ...
}
```

**Naming conventions:**
- `is*` - State checks: `isPending()`, `isActive()`, `isDeleted()`, `isExpired()`, `isValid()`
- `has*` - Validation: `hasStock()`, `hasPrice()`, `hasChildren()`
- `can*` - Permissions/Rules: `canBeCancelled()`, `canBeEdited()`

**Rules:**
- Pure functions (no side effects)
- Return boolean, never null
- Combine simple predicates into complex ones
- Keep entity methods simple (no repository calls)
- Use in Stream filters with method references: `.filter(Entity::isValid)`

**Examples:**
```java
# Entity predicate methods
public boolean isExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
}

public boolean isValid() {
    return !used && !isExpired();
}

# Usage in repository with method reference
return jpa.findByToken(token)
    .filter(UserTokenEntity::isValid)
    .map(this::toDto);

# Usage in service
if (!token.isValid()) {
    throw new InvalidTokenException();
}
```

---

## Method Naming

### Use Clear, Specific Names

```java
# GOOD - Clear intent
private ProductData toListData(ProductEntity e) { }
private ProductData toDetailData(ProductEntity e) { }
private boolean isNotFound(Long id) { }
private String getFailureReason(Long id) { }

# BAD - Ambiguous
private ProductData toData(ProductEntity e) { }
private ProductData convert(ProductEntity e) { }
private boolean check(Long id) { }
```

**Rules:**
- Use verb prefixes: `get`, `find`, `create`, `update`, `delete`, `to`, `from`, `is`, `has`, `can`
- Distinguish similar methods with suffixes: `toListData`, `toDetailData`
- Boolean methods start with `is`, `has`, `can`, `should`

---

## Exception Handling

### Use Custom Exceptions with Error Codes

```java
# GOOD - Current project pattern
throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
throw new DomainException("ORDER_NOT_DELIVERED", 
    "You can only review after order is delivered") {};

# BAD - Generic exceptions
throw new RuntimeException("Not found");
throw new Exception("Error");
```

**Rules:**
- Use custom exceptions: `NotFoundException`, `ConflictException`, `DomainException`
- Provide error codes (SCREAMING_SNAKE_CASE)
- Provide meaningful messages
- Use `{}` only for `DomainException` (abstract class)

---

## Import Organization

### Use Imports, Not FQN

```java
# GOOD
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public List<Product> findAll() {
    Map<Long, String> map = new HashMap<>();
}

# BAD
public java.util.List<Product> findAll() {
    java.util.Map<Long, String> map = new java.util.HashMap<>();
}

# ACCEPTABLE - FQN only for conflicts
import java.util.Date;
private Date utilDate;
private java.sql.Date sqlDate;  // Conflict resolved
```

**Rules:**
- Never use FQN in code (except conflicts)
- Wildcard imports acceptable but discouraged
- Group: Java standard → Third-party → Project
- Remove unused imports
