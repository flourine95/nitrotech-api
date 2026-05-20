---
inclusion: auto
---

# Core Coding Rules

Essential rules for all code changes. Load `#java-standards`, `#database-standards`, `#api-conventions` for details.

## Critical Rules

### Never use fully qualified names in code
```java
# BAD
public java.util.List<Product> findAll() { }

# GOOD
import java.util.List;
public List<Product> findAll() { }
```

### Always check instanceof before casting
```java
# BAD
Double rating = (Double) result[0];

# GOOD
if (result[0] instanceof Number number) {
    double rating = number.doubleValue();
}
```

### Use @Transactional for multi-step operations
```java
@Transactional
public void create(Command cmd) {
    repo.save(entity);
    saveRelated(entity.getId());
}
```

### Use batch queries, avoid N+1
```java
# BAD
for (Product p : products) {
    reviewRepo.findByProductId(p.getId());
}

# GOOD
List<Long> ids = products.stream().map(Product::getId).toList();
reviewRepo.findByProductIdIn(ids);
```

### Always include soft delete filter
```java
@Query("SELECT e FROM Entity e WHERE e.id = :id AND e.deletedAt IS NULL")
```

### Use proper logging
```java
# BAD
System.out.println("DEBUG: " + msg);

# GOOD
log.debug("Processing: {}", msg);
```

### Extract complex conditions to predicate methods
```java
# BAD - Complex lambda in filter
.filter(e -> !e.isUsed() && e.getExpiresAt().isAfter(LocalDateTime.now()))

# GOOD - Extract to entity predicate method
.filter(UserTokenEntity::isValid)

# Entity
public boolean isValid() {
    return !used && !isExpired();
}

public boolean isExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
}
```

### Use method references over lambdas
```java
# BAD
.map(e -> toDto(e))
.filter(item -> item != null)

# GOOD
.map(this::toDto)
.filter(Objects::nonNull)
```

## Pre-Commit Checklist

- [ ] No FQN in code
- [ ] Proper null/instanceof checks
- [ ] @Transactional on multi-step operations
- [ ] Batch queries used (no N+1)
- [ ] Soft delete filter included
- [ ] Proper logging (no System.out, no over-logging)
- [ ] Complex conditions extracted to entity predicate methods
- [ ] Method references used instead of simple lambdas
- [ ] Private mapper methods for simple DTOs (not MapStruct)
- [ ] Code compiles without warnings
