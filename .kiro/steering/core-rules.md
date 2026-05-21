---
inclusion: always
---

# Core Coding Rules

Essential rules for all code changes in the Nitrotech API project. These rules ensure code quality, maintainability, and consistency across the Spring Boot application.

## Code Style & Conventions

### Import Management
**Rule**: Always use explicit imports, never fully qualified names (FQN) in code.

```java
// BAD - Fully qualified name in code
public java.util.List<Product> findAll() { }

// GOOD - Proper import statement
import java.util.List;
public List<Product> findAll() { }
```

**Why**: Improves readability and follows Java conventions.

### Type Safety & Casting
**Rule**: Always use pattern matching with `instanceof` before casting. Never perform unsafe casts.

```java
// BAD - Unsafe cast
Double rating = (Double) result[0];

// GOOD - Pattern matching with instanceof
if (result[0] instanceof Number number) {
    double rating = number.doubleValue();
}
```

**Why**: Prevents ClassCastException at runtime and leverages modern Java features.

### Stream API Best Practices
**Rule**: Prefer method references over simple lambda expressions.

```java
// BAD - Unnecessary lambda
.map(e -> toDto(e))
.filter(item -> item != null)

// GOOD - Method references
.map(this::toDto)
.filter(Objects::nonNull)
```

**Rule**: Extract complex stream predicates to named entity methods.

```java
// BAD - Complex inline lambda
.filter(e -> !e.isUsed() && e.getExpiresAt().isAfter(LocalDateTime.now()))

// GOOD - Extracted predicate method
.filter(UserTokenEntity::isValid)

// In UserTokenEntity class
public boolean isValid() {
    return !used && !isExpired();
}

public boolean isExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
}
```

**Why**: Improves readability, testability, and reusability of business logic.

## Database & Persistence

### Transaction Management
**Rule**: Always annotate service methods with `@Transactional` when performing multiple database operations.

```java
@Transactional
public void create(CreateCommand cmd) {
    Entity entity = repo.save(toEntity(cmd));
    saveRelatedEntities(entity.getId());
}
```

**Why**: Ensures data consistency and proper rollback on failures.

### Query Optimization
**Rule**: Use batch queries to avoid N+1 query problems. Never query in loops.

```java
// BAD - N+1 query problem
for (Product p : products) {
    reviewRepo.findByProductId(p.getId());
}

// GOOD - Single batch query
List<Long> productIds = products.stream()
    .map(Product::getId)
    .toList();
List<Review> reviews = reviewRepo.findByProductIdIn(productIds);
```

**Why**: Dramatically improves performance by reducing database round trips.

### Soft Delete Pattern
**Rule**: Always include soft delete filter (`deletedAt IS NULL`) in custom JPQL queries.

```java
@Query("SELECT e FROM Entity e WHERE e.id = :id AND e.deletedAt IS NULL")
Optional<Entity> findActiveById(@Param("id") Long id);
```

**Why**: This project uses soft deletes. Queries must respect the soft delete pattern to avoid returning deleted records.

## Logging Standards

**Rule**: Use SLF4J logger with parameterized messages. Never use `System.out` or `System.err`.

```java
// BAD - Console output and string concatenation
System.out.println("DEBUG: " + msg);
log.info("User: " + user.getName() + " logged in");

// GOOD - Parameterized logging
log.debug("Processing message: {}", msg);
log.info("User {} logged in", user.getName());
```

**Why**: Proper logging enables production debugging and avoids unnecessary string concatenation overhead.

## DTO Mapping

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

**Why**: Keeps the codebase simple and avoids unnecessary dependencies for straightforward mappings.

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
- [ ] Code compiles without warnings or errors

## Related Documentation

For detailed standards, reference:
- `#java-standards` - Comprehensive Java coding standards
- `#database-standards` - Database design and query patterns
- `#api-conventions` - REST API design conventions
