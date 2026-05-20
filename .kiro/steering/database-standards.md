---
inclusion: manual
---

# Database & JPA Standards

PostgreSQL and JPA patterns for nitrotech-api. Core rules in `#core-rules`.

## Entity Design

### Validation Belongs in Request DTOs, Not Entities

```java
# GOOD - Current project pattern
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
    
    // No @NotBlank, @Size, @Email annotations
}

# Request DTO has validation
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

**Rules:**
- Never put `@NotBlank`, `@Size`, `@Email` on entities
- Validation annotations belong in Request DTOs (records)
- Entities define database constraints (`@Column(nullable = false)`)
- DTOs define business validation rules

---

## Query Patterns

### Always Include Soft Delete Filter

```java
# GOOD - Current project pattern
@Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
Optional<CategoryEntity> findActiveById(@Param("id") Long id);

@Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
Page<ProductEntity> findAll(Pageable pageable);

@Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
       "FROM CategoryEntity c WHERE c.slug = :slug AND c.deletedAt IS NULL")
boolean existsActiveBySlug(@Param("slug") String slug);

# BAD - Missing soft delete filter
@Query("SELECT c FROM CategoryEntity c WHERE c.id = :id")
Optional<CategoryEntity> findById(@Param("id") Long id);
```

**Rules:**
- All queries must filter `deletedAt IS NULL` for active records
- Name methods `findActive*`, `existsActive*` to indicate filtering
- Provide separate methods for deleted records: `findDeletedById`

---

## Batch Operations

### Use IN Clause for Multiple IDs

```java
# GOOD - Current project pattern
@Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NULL")
List<CategoryEntity> findAllActiveByIds(@Param("ids") List<Long> ids);

@Modifying
@Query("UPDATE CategoryEntity c SET c.deletedAt = :now " +
       "WHERE c.id IN :ids AND c.deletedAt IS NULL")
int bulkSoftDelete(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

# Usage - Avoid N+1
List<Long> productIds = products.stream().map(Product::getId).toList();
Map<Long, ReviewStats> statsMap = reviewRepo.getReviewStatsBatch(productIds);

# BAD - N+1 queries
for (Product p : products) {
    ReviewStats stats = reviewRepo.getReviewStats(p.getId());
}
```

**Rules:**
- Use `IN :ids` for batch queries
- Use `@Modifying` for bulk updates/deletes
- Collect IDs first, then batch query
- Return Map for O(1) lookup after batch query

---

## Transaction Management

### Use @Transactional for Multi-Step Operations

```java
# GOOD - Current project pattern
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

# BAD - Missing transaction
public ProductData create(CreateProductCommand command) {
    ProductEntity saved = productJpa.save(entity);
    saveImages(saved.getId(), command.images());  // Separate transaction!
}
```

**Rules:**
- Use `@Transactional` on Repository methods with multiple saves
- Use `@Transactional` on UseCase methods orchestrating multiple repositories
- Keep transactions short (no external API calls inside)
- Transactions rollback on unchecked exceptions automatically

---

## Soft Delete Implementation

### Standard Pattern

```java
# Entity
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

# Repository - Soft delete
public void softDelete(Long id) {
    CategoryEntity entity = jpa.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
    entity.setDeletedAt(LocalDateTime.now());
    jpa.save(entity);
}

# Repository - Restore
public void restore(Long id) {
    CategoryEntity entity = jpa.findDeletedById(id)
        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Deleted category not found"));
    entity.setDeletedAt(null);
    jpa.save(entity);
}

# Repository - Hard delete
public void hardDelete(Long id) {
    jpa.deleteById(id);  // Only for already soft-deleted records
}
```

**Rules:**
- Soft delete sets `deletedAt = LocalDateTime.now()`
- Restore sets `deletedAt = null`
- Hard delete only for already soft-deleted records
- Provide separate queries for active and deleted records

---

## Bulk Operations

### Efficient Batch Updates

```java
# GOOD - Current project pattern
@Modifying
@Query("UPDATE CategoryEntity c SET c.active = true " +
       "WHERE c.id IN :ids AND c.deletedAt IS NULL")
int bulkActivate(@Param("ids") List<Long> ids);

@Transactional
public List<Long> bulkSoftDelete(List<Long> ids) {
    List<Long> deletableIds = jpa.findAllActiveByIds(ids).stream()
        .filter(c -> !hasChildren(c.getId()))
        .map(CategoryEntity::getId)
        .toList();
    
    if (!deletableIds.isEmpty()) {
        jpa.bulkSoftDelete(deletableIds, LocalDateTime.now());
    }
    return deletableIds;
}
```

**Rules:**
- Use `@Modifying` + `@Query` for bulk updates
- Filter eligible IDs before bulk operation
- Return list of successfully processed IDs
- Use `@Transactional` on bulk operation methods

---

## Pagination

### Use Spring Data Page

```java
# GOOD - Current project pattern
@Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL")
Page<ProductEntity> findAll(Pageable pageable);

# Controller
Pageable pageable = SortUtils.toPageable(page, size, sort, SORTABLE_FIELDS, "createdAt");
Page<ProductData> result = productRepository.findAll(filter, pageable);
return ResponseEntity.ok(ApiResult.paged(result));

# BAD - Manual pagination (deprecated)
List<ProductData> items = productRepository.findAll(filter, page, size);
long total = productRepository.count(filter);
return ApiResult.paginated(items, total, page, size);  // Deprecated!
```

**Rules:**
- Return `Page<T>` from repository
- Accept `Pageable` parameter
- Use `ApiResult.paged(page)` not `ApiResult.paginated(...)`
- Single query gets both data and count

---

## JSON Columns (PostgreSQL)

### Use @JdbcTypeCode for JSONB

```java
# GOOD - Current project pattern
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb")
private Map<String, Object> specs;

@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb")
private Map<String, Object> attributes;
```

**Rules:**
- Use `@JdbcTypeCode(SqlTypes.JSON)` for JSON columns
- Specify `columnDefinition = "jsonb"` for PostgreSQL
- Use `Map<String, Object>` for flexible JSON data
- Hibernate handles serialization/deserialization automatically

---

## Recursive Queries (PostgreSQL)

### Use Native Queries for CTEs

```java
# GOOD - Current project pattern
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
    SELECT * FROM category_path ORDER BY depth
    """, nativeQuery = true)
List<Object[]> findCategoryPath(@Param("categoryId") Long categoryId);
```

**Rules:**
- Use `nativeQuery = true` for PostgreSQL-specific features
- Use CTEs for hierarchical data (categories, comments)
- Return `List<Object[]>` and map manually
- Always include `deleted_at IS NULL` in recursive queries

---

## Aggregate Queries

### Return Typed Results

```java
# GOOD - Current project pattern
/**
 * @return List of Object[] where each array contains:
 *         [0] Long productId
 *         [1] Double avgRating
 *         [2] Long reviewCount
 */
@Query("""
    SELECT r.productId, AVG(r.rating), COUNT(r.id)
    FROM ReviewEntity r
    WHERE r.productId IN :productIds AND r.status = 'approved'
    GROUP BY r.productId
    """)
List<Object[]> getReviewStatsBatch(@Param("productIds") List<Long> productIds);

# Usage with safe casting
for (Object[] row : results) {
    Long productId = ((Number) row[0]).longValue();
    Double avgRating = row[1] instanceof Number n ? n.doubleValue() : null;
    Long count = ((Number) row[2]).longValue();
}
```

**Rules:**
- Document return type in Javadoc for `Object[]` results
- Use safe casting with `instanceof`
- Consider creating DTO projection for complex aggregates
- Use `COALESCE` for null-safe aggregates
