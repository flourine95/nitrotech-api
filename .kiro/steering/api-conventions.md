---
inclusion: manual
---

# API Conventions

REST API patterns for nitrotech-api. Core rules in `#core-rules`.

## URL Design

### Use Plural Nouns and Kebab-Case

```java
# GOOD - Current project pattern
@RestController
@RequestMapping("/api/products")
public class ProductController { }

@RestController
@RequestMapping("/api/categories")
public class CategoryController { }

@RestController
@RequestMapping("/api/product-variants")  // Multi-word: kebab-case
public class ProductVariantController { }

# BAD
@RequestMapping("/api/product")  // Singular
@RequestMapping("/api/productCategories")  // camelCase
@RequestMapping("/api/product_categories")  // snake_case
@RequestMapping("/api/createProduct")  // Verb in URL
```

**Rules:**
- Always use plural nouns: `/products`, `/categories`, `/orders`
- Use kebab-case for multi-word resources: `/product-variants`
- No verbs in URLs (use HTTP methods)
- Prefix all APIs with `/api`

---

## Request Validation

### Validation Messages Format

```java
# GOOD - Current project pattern
@NotBlank(message = "Name is required")
@Size(max = 255, message = "Name must be at most 255 characters")
@Email(message = "Email must be valid")
@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", 
         message = "Slug must be lowercase letters, numbers and hyphens")

# BAD
@NotBlank(message = "name is required")  // Lowercase
@Size(max = 255, message = "Name Must Be At Most 255 Characters")  // Title case
@Email(message = "email must be valid.")  // Period at end
```

**Rules:**
- Sentence case (first word capitalized)
- No period at end
- Be specific and actionable
- Use field name as it appears in request

---

## Error Codes

### Format: ENTITY_ACTION

```java
# GOOD - Current project pattern
throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
throw new DomainException("ORDER_NOT_DELIVERED", 
    "You can only review after order is delivered") {};
throw new DomainException("INSUFFICIENT_STOCK", 
    "Insufficient stock. Available: " + available) {};

# BAD
throw new NotFoundException("ProductNotFound", ...);  // PascalCase
throw new ConflictException("product-slug-exists", ...);  // kebab-case
throw new DomainException("PRODUCT_SLUG_ALREADY_EXISTS", ...);  // Too verbose
```

**Rules:**
- Use SCREAMING_SNAKE_CASE
- Format: `ENTITY_ACTION` or `ENTITY_STATE`
- Be specific: `VARIANT_SKU_EXISTS` not `SKU_EXISTS`
- Consistent entity names: PRODUCT, BRAND, VARIANT, ORDER, CATEGORY

---

## Request Records

### Annotation Formatting

```java
# GOOD - Current project pattern
public record CreateBrandRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", 
                 message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        String description,
        boolean active
) {}

# BAD
public record CreateBrandRequest(
        @NotBlank(message = "Name is required") @Size(max = 255) String name,
        @NotBlank String slug, String description, boolean active) {}
```

**Rules:**
- One annotation per line
- Align annotations vertically
- Field on separate line after annotations
- One field per line with trailing comma

---

## Boolean Fields

### Primitive for Create, Wrapper for Update

```java
# GOOD - Current project pattern
public record CreateBrandRequest(
        String name,
        boolean active  // Primitive - defaults to false
) {}

public record UpdateBrandRequest(
        String name,
        Boolean active  // Wrapper - null means don't update
) {}

# Usage in repository
public BrandData update(UpdateBrandCommand command) {
    BrandEntity entity = jpa.findActiveById(command.id()).orElseThrow(...);
    if (command.name() != null) entity.setName(command.name());
    if (command.active() != null) entity.setActive(command.active());
    return toData(jpa.save(entity));
}
```

**Rules:**
- Create requests: Use `boolean` (primitive)
- Update requests: Use `Boolean` (wrapper)
- Rationale: Updates must distinguish "set to false" from "don't change"

---

## Controller Patterns

### Return Type and Status Codes

```java
# GOOD - Current project pattern
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    @GetMapping
    public ResponseEntity<ApiResult<List<ProductData>>> list(...) {
        Page<ProductData> page = useCase.execute(filter, pageable);
        return ResponseEntity.ok(ApiResult.paged(page));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<ProductData>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(useCase.execute(id)));
    }
    
    @PostMapping
    public ResponseEntity<ApiResult<ProductData>> create(@Valid @RequestBody CreateProductRequest req) {
        ProductData data = useCase.execute(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<ProductData>> update(...) {
        return ResponseEntity.ok(ApiResult.ok(useCase.execute(command)));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        useCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Product deleted successfully"));
    }
}

# BAD
public ApiResult<ProductData> get(...) { }  // Missing ResponseEntity
public ResponseEntity<ProductData> create(...) { }  // Missing ApiResult wrapper
```

**Rules:**
- Always return `ResponseEntity<ApiResult<T>>`
- Use `ResponseEntity.ok()` for 200
- Use `ResponseEntity.status(HttpStatus.CREATED)` for 201
- ApiResult methods: `ok(data)`, `paged(page)`, `created(data)`

---

## Sortable Fields

### Declare as Constant

```java
# GOOD - Current project pattern
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "slug", "active", "createdAt", "updatedAt");
    
    @GetMapping
    public ResponseEntity<ApiResult<List<ProductData>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        Pageable pageable = SortUtils.toPageable(page, size, sort, 
            SORTABLE_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResult.paged(useCase.execute(filter, pageable)));
    }
}

# BAD
Pageable pageable = SortUtils.toPageable(page, size, sort, null, "createdAt");
```

**Rules:**
- Declare `SORTABLE_FIELDS` as `private static final Set<String>`
- Include only fields that exist and make sense for sorting
- Common fields: `id`, `name`, `slug`, `active`, `createdAt`, `updatedAt`
- Specify default sort field (usually `createdAt`)

---

## Bulk Operations

### Naming Convention

```java
# GOOD - Current project pattern
DELETE /api/brands/bulk              → bulkDelete()
DELETE /api/brands/bulk/permanent    → bulkHardDelete()
PATCH  /api/brands/bulk/restore      → bulkRestore()
PATCH  /api/categories/bulk/activate → bulkActivate()

# Request classes
BulkDeleteBrandRequest
BulkHardDeleteBrandRequest
BulkRestoreBrandRequest

# Use case classes
BulkDeleteBrandUseCase
BulkHardDeleteBrandUseCase
BulkRestoreBrandUseCase
```

**Rules:**
- Prefix with `bulk` (lowercase in URLs, PascalCase in classes)
- `/bulk` for soft delete
- `/bulk/permanent` for hard delete
- `/bulk/restore` for restore
- `/bulk/activate` and `/bulk/deactivate` for status changes

---

## Mapper Conventions

### When to Use MapStruct vs Private Methods

```java
# Use MapStruct for complex bidirectional mapping
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AddressMapper {
    AddressData toData(AddressEntity entity);
    
    @Mapping(target = "id", ignore = true)
    AddressEntity toEntity(CreateAddressCommand command, Long userId);
    
    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget AddressEntity entity, UpdateAddressCommand command);
}

# Use private methods for simple one-way mapping
@Repository
public class BrandRepositoryImpl implements BrandRepository {
    
    private BrandData toData(BrandEntity e) {
        return new BrandData(e.getId(), e.getName(), e.getSlug(), 
            e.getLogo(), e.getDescription(), e.isActive(), 
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
```

**When to use MapStruct:**
- Complex mappings with many fields
- Bidirectional mappings (entity ↔ DTO)
- Need `@MappingTarget` for updates
- Nested object mappings

**When to use private methods:**
- Simple one-way mappings (entity → DTO)
- Repository implementations
- Custom logic needed

**Naming:**
- `toData(Entity)` → Data
- `toEntity(Command)` → Entity
- `toListData(Entity)` → Data (for list endpoints)
- `toDetailData(Entity)` → Data (for detail endpoints)

---

## Extract Comparison Logic

### Delegate to Methods

```java
# BAD - Complex logic in loop
public ValidateDeleteResult execute(List<Long> ids) {
    for (Long id : ids) {
        if (!categoryRepository.existsById(id)) {
            cannotDelete.add(id);
            reasons.put(id, "Category not found or already deleted");
        } else if (categoryRepository.hasAnyChildren(id)) {
            cannotDelete.add(id);
            int childCount = categoryRepository.hasActiveChildren(id) ? 
                countChildren(id) : countAllChildren(id);
            reasons.put(id, "Has " + childCount + " children");
        } else {
            canDelete.add(id);
        }
    }
}

# GOOD - Extract to methods
public ValidateDeleteResult execute(List<Long> ids) {
    for (Long id : ids) {
        if (isNotFound(id)) {
            cannotDelete.add(id);
            reasons.put(id, "Category not found or already deleted");
        } else if (hasChildren(id)) {
            cannotDelete.add(id);
            reasons.put(id, getChildrenMessage(id));
        } else {
            canDelete.add(id);
        }
    }
}

private boolean isNotFound(Long id) {
    return !categoryRepository.existsById(id);
}

private boolean hasChildren(Long id) {
    return categoryRepository.hasAnyChildren(id);
}

private String getChildrenMessage(Long id) {
    int childCount = categoryRepository.hasActiveChildren(id) ? 
        countChildren(id) : countAllChildren(id);
    return "Has " + childCount + " children";
}
```

**Rules:**
- Extract complex conditions to predicate methods
- Extract error message building to separate methods
- Use Stream API when appropriate
- Consider batch validation for better performance

---

## Method References

### Prefer Over Simple Lambdas

```java
# GOOD - Current project pattern
List<Long> userIds = userJpa.findAll().stream()
        .map(UserEntity::getId)
        .toList();

List<Long> ids = items.stream()
        .filter(Objects::nonNull)
        .toList();

# BAD
List<Long> userIds = userJpa.findAll().stream()
        .map(u -> u.getId())
        .toList();

List<Long> ids = items.stream()
        .filter(id -> id != null)
        .toList();
```

**Rules:**
- Use method references when clearer than lambdas
- Common patterns: `Entity::getField`, `Objects::nonNull`, `String::toLowerCase`
- Keep lambdas for complex logic or multiple statements

---

## Deprecated APIs

### Migrate from ApiResult.paginated()

```java
# GOOD - Use ApiResult.paged()
@GetMapping
public ResponseEntity<ApiResult<List<ProductData>>> list(...) {
    Pageable pageable = SortUtils.toPageable(page, size, sort, SORTABLE_FIELDS, "createdAt");
    Page<ProductData> result = productRepository.findAll(filter, pageable);
    return ResponseEntity.ok(ApiResult.paged(result));
}

# BAD - Deprecated method
@GetMapping
public ResponseEntity<ApiResult<List<ProductData>>> list(...) {
    List<ProductData> items = productRepository.findAll(filter, page, size);
    long total = productRepository.count(filter);
    return ResponseEntity.ok(ApiResult.paginated(items, total, page, size));  // Deprecated!
}
```

**Rules:**
- Use `ApiResult.paged(page)` not `ApiResult.paginated(...)`
- Return `Page<T>` from repository
- Accept `Pageable` parameter
- Single query gets both data and count
