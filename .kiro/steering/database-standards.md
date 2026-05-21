---
inclusion: manual
---

# Database & JPA Standards

PostgreSQL and JPA patterns for the Nitrotech API. For core coding rules, see `#core-rules`.

## Entity Design

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
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", 
                 message = "Slug must be lowercase letters, numbers and hyphens")
        String slug
) {}
```

**Why**: 
- Entities represent database structure, not business rules
- Validation rules may differ between create/update operations
- Request DTOs provide better error messages and flexibility
- Entities use `@Column` constraints for database-level enforcement

## Soft Delete Pattern

**Rule**: This project uses soft deletes. All queries must filter `deletedAt IS NULL` for active records.

```java
// GOOD - Queries with soft delete filter
@Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
Optional<CategoryEntity> findActiveById(@Param("id") Long id);

@Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
Page<ProductEntity> findAll(Pageable pageable);

@Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
       "FROM CategoryEntity c WHERE c.slug = :slug AND c.deletedAt IS NULL")
boolean existsActiveBySlug(@Param("slug") String slug);

// BAD - Missing soft delete filter
@Query("SELECT c FROM CategoryEntity c WHERE c.id = :id")
Optional<CategoryEntity> findById(@Param("id") Long id);
```

**Naming Conventions**:
- `findActive*` - Returns only non-deleted records
- `findDeleted*` - Returns only soft-deleted records
- `existsActive*` - Checks existence of non-deleted records

**Soft Delete Operations**:
```java
// Soft delete - Set deletedAt timestamp
public void softDelete(Long id) {
    CategoryEntity entity = jpa.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
            "Category not found"));
    entity.setDeletedAt(LocalDateTime.now());
    jpa.save(entity);
}

// Restore - Clear deletedAt timestamp
public void restore(Long id) {
    CategoryEntity entity = jpa.findDeletedById(id)
        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
            "Deleted category not found"));
    entity.setDeletedAt(null);
    jpa.save(entity);
}

// Hard delete - Permanent removal (only for already soft-deleted records)
public void hardDelete(Long id) {
    jpa.deleteById(id);
}
```

## Batch Queries

**Rule**: Use `IN` clause for batch operations to avoid N+1 query problems.

```java
// GOOD - Batch query with IN clause
@Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NULL")
List<CategoryEntity> findAllActiveByIds(@Param("ids") List<Long> ids);

// Usage - Efficient batch loading
List<Long> productIds = products.stream()
    .map(Product::getId)
    .toList();
Map<Long, ReviewStats> statsMap = reviewRepo.getReviewStatsBatch(productIds);

// BAD - N+1 query problem
for (Product p : products) {
    ReviewStats stats = reviewRepo.getReviewStats(p.getId());  // Separate query each iteration
}
```

**Batch Update Operations**:
```java
@Modifying
@Query("UPDATE CategoryEntity c SET c.deletedAt = :now " +
       "WHERE c.id IN :ids AND c.deletedAt IS NULL")
int bulkSoftDelete(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

@Modifying
@Query("UPDATE CategoryEntity c SET c.active = true " +
       "WHERE c.id IN :ids AND c.deletedAt IS NULL")
int bulkActivate(@Param("ids") List<Long> ids);
```

**Guidelines**:
- Collect IDs first, then perform single batch query
- Return `Map<Long, T>` for O(1) lookup after batch query
- Use `@Modifying` annotation for bulk updates/deletes
- Filter eligible records before bulk operations

## Transaction Management

**Rule**: Use `@Transactional` for operations involving multiple database writes or repository calls.

```java
// GOOD - Transaction for multi-step operation
@Repository
public class ProductRepositoryImpl {
    @Transactional
    public ProductData create(CreateProductCommand command) {
        ProductEntity entity = new ProductEntity();
        // ... set fields
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

// GOOD - Transaction for orchestrating multiple repositories
@Service
public class PlaceOrderUseCase {
    @Transactional
    public OrderData execute(CreateOrderCommand command) {
        OrderData order = orderRepository.place(data);
        inventoryRepository.decreaseStock(items);
        cartRepository.clearCart(command.userId());
        return order;
    }
}

// BAD - Missing transaction (each operation in separate transaction)
public ProductData create(CreateProductCommand command) {
    ProductEntity saved = productJpa.save(entity);
    saveImages(saved.getId(), command.images());  // Separate transaction!
}
```

**Transaction Guidelines**:
- Apply `@Transactional` at repository method level for multi-step saves
- Apply `@Transactional` at use case level when orchestrating multiple repositories
- Keep transactions short - no external API calls inside transactions
- Transactions automatically rollback on unchecked exceptions
- Read-only operations don't require `@Transactional`

## Bulk Operations

**Rule**: Filter eligible records before performing bulk operations, then return processed IDs.

```java
// GOOD - Safe bulk operation with filtering
@Transactional
public BulkResult bulkSoftDelete(List<Long> ids) {
    // Find eligible records
    List<CategoryEntity> categories = jpa.findAllActiveByIds(ids);
    
    // Filter based on business rules
    List<Long> deletableIds = categories.stream()
        .filter(c -> !hasChildren(c.getId()))
        .map(CategoryEntity::getId)
        .toList();
    
    List<Long> failedIds = ids.stream()
        .filter(id -> !deletableIds.contains(id))
        .toList();
    
    // Perform bulk operation
    if (!deletableIds.isEmpty()) {
        jpa.bulkSoftDelete(deletableIds, LocalDateTime.now());
    }
    
    return new BulkResult(deletableIds, failedIds, buildReasons(failedIds));
}
```

**Pattern**:
1. Fetch records by IDs
2. Filter based on business rules
3. Perform bulk operation on eligible IDs
4. Return success/failure lists with reasons

## Pagination

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

// BAD - Manual pagination (deprecated pattern)
List<ProductData> items = productRepository.findAll(filter, page, size);
long total = productRepository.count(filter);
return ApiResult.paginated(items, total, page, size);  // Deprecated!
```

**Benefits**:
- Single query returns both data and total count
- Built-in sorting support
- Consistent pagination metadata
- Use `ApiResult.paged(page)` not `ApiResult.paginated(...)`

## JSON Columns (PostgreSQL)

**Rule**: Use `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "jsonb"` for JSON columns.

```java
// GOOD - JSON column mapping
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb")
private Map<String, Object> specs;

@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb")
private Map<String, Object> attributes;
```

**Why**: 
- PostgreSQL's `jsonb` type provides efficient storage and indexing
- Hibernate automatically handles JSON serialization/deserialization
- `Map<String, Object>` provides flexibility for dynamic data

## Recursive Queries (PostgreSQL)

**Rule**: Use native queries with CTEs (Common Table Expressions) for hierarchical data.

```java
// GOOD - Recursive CTE for category hierarchy
@Query(value = """
    WITH RECURSIVE category_path AS (
        SELECT id, name, slug, active, parent_id, 0 as depth
        FROM categories
        WHERE id = :categoryId AND deleted_at IS NULL
        
        UNION ALL
        
        SELECT c.id, c.name, c.slug, c.active, c.parent_id, cp.depth + 1
        FROM categories c
        INNER JOIN category_path cp ON c.parent_id = cp.id
        WHERE c.deleted_at IS NULL
    )
    SELECT id, name, slug, active FROM category_path ORDER BY depth
    """, nativeQuery = true)
List<Object[]> findCategoryPath(@Param("categoryId") Long categoryId);
```

**Guidelines**:
- Use `nativeQuery = true` for PostgreSQL-specific features
- Always include `deleted_at IS NULL` in recursive queries
- Return `List<Object[]>` and map manually to DTOs
- Document the structure of `Object[]` in method comments

## Aggregate Queries

**Rule**: Document `Object[]` return types and use safe casting when processing results.

```java
// GOOD - Documented aggregate query
/**
 * Batch get review statistics for multiple products
 * Returns: [productId (Long), avgRating (Double), reviewCount (Long)]
 */
@Query("""
    SELECT r.productId, AVG(r.rating), COUNT(r.id)
    FROM ReviewEntity r
    WHERE r.productId IN :productIds 
      AND r.status = 'approved' 
      AND r.deletedAt IS NULL
    GROUP BY r.productId
    """)
List<Object[]> getReviewStatsBatch(@Param("productIds") List<Long> productIds);

// Usage with safe casting
for (Object[] row : results) {
    Long productId = ((Number) row[0]).longValue();
    Double avgRating = row[1] instanceof Number n ? n.doubleValue() : null;
    Long count = ((Number) row[2]).longValue();
}
```

**Best Practices**:
- Always document `Object[]` structure in method Javadoc
- Use `instanceof` pattern matching for safe casting
- Use `COALESCE` for null-safe aggregates in queries
- Consider DTO projections for complex aggregates

## Query Method Naming

**Naming Patterns**:
- `findActive*` - Returns non-deleted records
- `findDeleted*` - Returns soft-deleted records
- `existsActive*` - Checks existence (non-deleted)
- `countActive*` - Counts non-deleted records
- `*Batch` - Batch operation (e.g., `findByIdsBatch`)
- `bulk*` - Bulk update/delete operation

**Examples**:
```java
Optional<ProductEntity> findActiveById(Long id);
List<ProductEntity> findAllActiveByIds(List<Long> ids);
boolean existsActiveBySlug(String slug);
int countActiveVariants(Long productId);
List<Object[]> getReviewStatsBatch(List<Long> productIds);
int bulkSoftDelete(List<Long> ids, LocalDateTime now);
```
