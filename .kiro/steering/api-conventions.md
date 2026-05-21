---
inclusion: manual
---

# API Conventions

REST API design patterns and conventions for the Nitrotech API. For core coding rules, see `#core-rules`.

## URL Design

**Rule**: Use plural nouns with kebab-case for multi-word resources. All endpoints must be prefixed with `/api`.

```java
// GOOD - RESTful URL patterns
@RestController
@RequestMapping("/api/products")
public class ProductController { }

@RestController
@RequestMapping("/api/categories")
public class CategoryController { }

@RestController
@RequestMapping("/api/product-variants")  // Multi-word: kebab-case
public class ProductVariantController { }

// BAD - Anti-patterns
@RequestMapping("/api/product")              // Singular noun
@RequestMapping("/api/productCategories")    // camelCase
@RequestMapping("/api/product_categories")   // snake_case
@RequestMapping("/api/createProduct")        // Verb in URL
```

**Guidelines**:
- Always use plural nouns: `/products`, `/categories`, `/orders`
- Multi-word resources use kebab-case: `/product-variants`, `/order-items`
- No verbs in URLs - use HTTP methods (GET, POST, PUT, DELETE)
- All APIs prefixed with `/api`

## Request Validation

**Rule**: Validation messages must use sentence case (first word capitalized) with no period at the end.

```java
// GOOD - Proper validation messages
@NotBlank(message = "Name is required")
@Size(max = 255, message = "Name must be at most 255 characters")
@Email(message = "Email must be valid")
@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", 
         message = "Slug must be lowercase letters, numbers and hyphens")

// BAD - Incorrect formatting
@NotBlank(message = "name is required")                          // Lowercase
@Size(max = 255, message = "Name Must Be At Most 255 Characters")  // Title case
@Email(message = "email must be valid.")                         // Period at end
```

**Message Guidelines**:
- Sentence case: First word capitalized, rest lowercase
- No period at end
- Be specific and actionable
- Use field name as it appears in the request

## Error Codes

**Rule**: Error codes must use SCREAMING_SNAKE_CASE with format `ENTITY_ACTION` or `ENTITY_STATE`.

```java
// GOOD - Structured error codes
throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
throw new DomainException("ORDER_NOT_DELIVERED", 
    "You can only review after order is delivered") {};
throw new DomainException("INSUFFICIENT_STOCK", 
    "Insufficient stock. Available: " + available) {};

// BAD - Inconsistent formatting
throw new NotFoundException("ProductNotFound", ...);           // PascalCase
throw new ConflictException("product-slug-exists", ...);       // kebab-case
throw new DomainException("PRODUCT_SLUG_ALREADY_EXISTS", ...); // Too verbose
```

**Error Code Patterns**:
- Format: `ENTITY_ACTION` (e.g., `PRODUCT_NOT_FOUND`, `ORDER_CANCELLED`)
- Format: `ENTITY_STATE` (e.g., `INSUFFICIENT_STOCK`, `INVALID_TOKEN`)
- Be specific: `VARIANT_SKU_EXISTS` not just `SKU_EXISTS`
- Consistent entity names: `PRODUCT`, `BRAND`, `VARIANT`, `ORDER`, `CATEGORY`

## Request Records

**Rule**: Format request records with one annotation per line, vertically aligned, with fields on separate lines.

```java
// GOOD - Clean, readable formatting
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

// BAD - Cramped, hard to read
public record CreateBrandRequest(
        @NotBlank(message = "Name is required") @Size(max = 255) String name,
        @NotBlank String slug, String description, boolean active) {}
```

**Formatting Rules**:
- One annotation per line
- Align annotations vertically
- Field declaration on separate line after annotations
- One field per line with trailing comma
- Empty line between fields with annotations

## Boolean Fields in Requests

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

// Usage in service
public BrandData update(UpdateBrandCommand command) {
    BrandEntity entity = jpa.findActiveById(command.id()).orElseThrow();
    if (command.name() != null) entity.setName(command.name());
    if (command.active() != null) entity.setActive(command.active());
    return toData(jpa.save(entity));
}
```

**Why**: Update requests must distinguish between "set to false" and "don't change this field". Wrapper `Boolean` allows `null` to mean "no change".

## Controller Patterns

**Rule**: Always return `ResponseEntity<ApiResult<T>>` with appropriate HTTP status codes.

```java
// GOOD - Complete controller pattern
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
    public ResponseEntity<ApiResult<ProductData>> create(
            @Valid @RequestBody CreateProductRequest req) {
        ProductData data = useCase.execute(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.created(data));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<ProductData>> update(...) {
        return ResponseEntity.ok(ApiResult.ok(useCase.execute(command)));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        useCase.execute(id);
        return ResponseEntity.ok(
            ApiResult.ok(null, "Product deleted successfully"));
    }
}

// BAD - Missing wrappers
public ApiResult<ProductData> get(...) { }           // Missing ResponseEntity
public ResponseEntity<ProductData> create(...) { }   // Missing ApiResult
```

**HTTP Status Codes**:
- `200 OK` - Successful GET, PUT, DELETE: `ResponseEntity.ok()`
- `201 Created` - Successful POST: `ResponseEntity.status(HttpStatus.CREATED)`
- `204 No Content` - Successful DELETE with no body (alternative pattern)

**ApiResult Methods**:
- `ApiResult.ok(data)` - Single resource response
- `ApiResult.paged(page)` - Paginated list response
- `ApiResult.created(data)` - Created resource response

## Sortable Fields

**Rule**: Declare sortable fields as a constant `Set<String>` in the controller.

```java
// GOOD - Explicit sortable fields
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
        Pageable pageable = SortUtils.toPageable(
            page, size, sort, SORTABLE_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResult.paged(useCase.execute(filter, pageable)));
    }
}

// BAD - No field validation
Pageable pageable = SortUtils.toPageable(page, size, sort, null, "createdAt");
```

**Guidelines**:
- Declare as `private static final Set<String>`
- Include only fields that exist in the entity
- Common fields: `id`, `name`, `slug`, `active`, `createdAt`, `updatedAt`
- Specify sensible default sort (usually `createdAt` descending)

## Bulk Operations

**Rule**: Use consistent naming for bulk operations with appropriate HTTP methods.

**URL Patterns**:
```
DELETE /api/brands/bulk              → bulkDelete() (soft delete)
DELETE /api/brands/bulk/permanent    → bulkHardDelete() (hard delete)
PATCH  /api/brands/bulk/restore      → bulkRestore()
PATCH  /api/categories/bulk/activate → bulkActivate()
PATCH  /api/categories/bulk/deactivate → bulkDeactivate()
```

**Class Naming**:
```java
// Request classes
BulkDeleteBrandRequest
BulkHardDeleteBrandRequest
BulkRestoreBrandRequest

// Use case classes
BulkDeleteBrandUseCase
BulkHardDeleteBrandUseCase
BulkRestoreBrandUseCase
```

**Conventions**:
- Prefix with `bulk` (lowercase in URLs, PascalCase in class names)
- `/bulk` for soft delete
- `/bulk/permanent` for hard delete
- `/bulk/restore` for restoring soft-deleted records
- `/bulk/activate` and `/bulk/deactivate` for status changes

## DTO Mapping Strategy

**Rule**: Use private methods for simple mappings, MapStruct for complex bidirectional mappings.

```java
// Use MapStruct for complex mappings
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AddressMapper {
    AddressData toData(AddressEntity entity);
    
    @Mapping(target = "id", ignore = true)
    AddressEntity toEntity(CreateAddressCommand command, Long userId);
    
    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget AddressEntity entity, 
                      UpdateAddressCommand command);
}

// Use private methods for simple mappings
@Repository
public class BrandRepositoryImpl implements BrandRepository {
    
    private BrandData toData(BrandEntity e) {
        return new BrandData(
            e.getId(), e.getName(), e.getSlug(), 
            e.getLogo(), e.getDescription(), e.isActive(), 
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
```

**When to use MapStruct**:
- Complex mappings with many fields (>5)
- Bidirectional mappings (entity ↔ DTO)
- Need `@MappingTarget` for update operations
- Nested object mappings

**When to use private methods**:
- Simple one-way mappings (entity → DTO)
- Repository implementations
- Custom transformation logic needed
- Few fields (<5)

**Method Naming**:
- `toData(Entity)` - Entity to DTO
- `toEntity(Command)` - Command to Entity
- `toListData(Entity)` - For list endpoint DTOs
- `toDetailData(Entity)` - For detail endpoint DTOs

## Pagination

**Rule**: Use Spring Data's `Page<T>` and `Pageable` for all paginated endpoints.

```java
// GOOD - Modern pagination pattern
@GetMapping
public ResponseEntity<ApiResult<List<ProductData>>> list(...) {
    Pageable pageable = SortUtils.toPageable(
        page, size, sort, SORTABLE_FIELDS, "createdAt");
    Page<ProductData> result = productRepository.findAll(filter, pageable);
    return ResponseEntity.ok(ApiResult.paged(result));
}

// BAD - Deprecated manual pagination
@GetMapping
public ResponseEntity<ApiResult<List<ProductData>>> list(...) {
    List<ProductData> items = productRepository.findAll(filter, page, size);
    long total = productRepository.count(filter);
    return ResponseEntity.ok(ApiResult.paginated(items, total, page, size));
}
```

**Guidelines**:
- Use `ApiResult.paged(page)` not `ApiResult.paginated(...)`
- Repository methods should return `Page<T>`
- Accept `Pageable` parameter in repository methods
- Single query gets both data and total count
