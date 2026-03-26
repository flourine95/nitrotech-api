# Coding Standards — Nitrotech API

## Thư viện & Dependencies

| Thư viện | Version | Mục đích |
|----------|---------|---------|
| MapStruct | 1.6.3 | Compile-time type-safe mapping Entity ↔ DTO |
| jjwt | 0.12.6 | JWT generation & validation |
| Lombok | (Spring Boot managed) | Chỉ dùng cho JPA Entity |

### Lombok — dùng có chọn lọc

```java
// ✅ Entity — dùng Lombok (record không dùng được với Hibernate)
@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class CategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}

// ✅ DTO/Command — dùng record, không cần Lombok
public record CreateCategoryCommand(String name, String slug) {}
```

Tránh `@Data` trên Entity — nó generate `equals/hashCode` dựa trên tất cả fields, gây vấn đề với Hibernate lazy loading và circular reference.

### MapStruct — Entity ↔ DTO mapping

```java
// infrastructure/persistence/mapper/CategoryMapper.java
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryData toData(CategoryEntity entity);

    @Mapping(target = "id", ignore = true)
    CategoryEntity toEntity(CreateCategoryCommand command);
}
```

MapStruct generate code tại compile-time — không dùng reflection, không ảnh hưởng runtime performance.

Lưu ý: `mapstruct-processor` phải khai báo SAU `lombok` trong `annotationProcessor` để tránh conflict (đã xử lý trong `build.gradle` với `lombok-mapstruct-binding`).

### JWT — jjwt 0.12.x

```java
// shared/util/JwtUtil.java
@Component
public class JwtUtil {

    private final SecretKey key = Jwts.SIG.HS256.key().build();

    public String generate(String subject) {
        return Jwts.builder()
            .subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(key)
            .compact();
    }

    public String extractSubject(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
}
```

---

## Java 21 Features được khuyến khích dùng

### Records cho DTOs
```java
// ✅ Dùng record cho immutable DTOs
public record CreateCategoryCommand(String name, String slug, Long parentId) {}

// ❌ Không dùng class thông thường cho DTOs
public class CreateCategoryCommand {
    private String name;
    // ...
}
```

### Pattern Matching
```java
// ✅ Pattern matching instanceof
if (exception instanceof CategoryNotFoundException e) {
    return ResponseEntity.status(422).body(e.getMessage());
}
```

### Sealed Classes cho Domain Exceptions (tùy chọn)
```java
public sealed class DomainException extends RuntimeException
    permits CategoryNotFoundException, ProductNotFoundException {}
```

### Text Blocks cho SQL/JSON trong tests
```java
String sql = """
    SELECT * FROM categories
    WHERE parent_id = :parentId
    AND is_visible = true
    """;
```

---

## Naming Conventions

| Loại | Convention | Ví dụ |
|------|-----------|-------|
| UseCase | `{Action}{Module}UseCase` | `CreateCategoryUseCase` |
| Command DTO | `{Action}{Module}Command` | `CreateCategoryCommand` |
| Response DTO | `{Module}Data` | `CategoryData` |
| Repository Interface | `{Module}Repository` | `CategoryRepository` |
| Repository Impl | `{Module}RepositoryImpl` | `CategoryRepositoryImpl` |
| JPA Entity | `{Module}Entity` | `CategoryEntity` |
| JPA Repo | `{Module}JpaRepository` | `CategoryJpaRepository` |
| Controller | `{Module}Controller` | `CategoryController` |
| Request DTO | `{Action}{Module}Request` | `CreateCategoryRequest` |
| Config | `{Module}Config` | `CategoryConfig` |
| Mapper | `{Module}Mapper` | `CategoryMapper` |
| Exception | `{Module}{Reason}Exception` | `CategoryNotFoundException` |

---

## Package Structure

```
com.nitrotech.api
├── domain
│   └── {module}          # lowercase, singular
│       ├── dto
│       ├── repository
│       ├── usecase
│       └── exception
├── infrastructure
│   ├── persistence
│   │   ├── entity
│   │   ├── repository
│   │   └── mapper
│   └── external
│       └── {service}
├── application
│   └── {module}
│       ├── controller
│       └── request
└── shared
    ├── config
    ├── exception
    └── util
```

---

## Response Format chuẩn

### Success
```json
{
  "data": { ... },
  "message": "Created successfully"
}
```

### Error
```json
{
  "error": "Category with ID 5 not found.",
  "code": "CATEGORY_NOT_FOUND",
  "status": 404
}
```

### Paginated
```json
{
  "data": [ ... ],
  "meta": {
    "page": 1,
    "size": 20,
    "total": 100
  }
}
```

---

## HTTP Status Codes

| Tình huống | Status |
|-----------|--------|
| Tạo thành công | 201 Created |
| Lấy/Cập nhật thành công | 200 OK |
| Xóa thành công | 204 No Content |
| Không tìm thấy | 404 Not Found |
| Validation lỗi | 422 Unprocessable Entity |
| Unauthorized | 401 Unauthorized |
| Forbidden | 403 Forbidden |

---

## Database Migrations (Flyway)

Đặt file trong `src/main/resources/db/migration/`

Naming convention: `V{version}__{description}.sql`

```
V1__create_categories_table.sql
V2__create_products_table.sql
V3__create_orders_table.sql
```

---

## Dependency Injection

UseCase là Java thuần — đăng ký qua `@Configuration`, không dùng `@Service`:

```java
@Configuration
public class CategoryConfig {

    @Bean
    public CreateCategoryUseCase createCategoryUseCase(CategoryRepository repo) {
        return new CreateCategoryUseCase(repo);
    }

    @Bean
    public UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository repo) {
        return new UpdateCategoryUseCase(repo);
    }
}
```

Repository Implementation dùng `@Repository` (Spring stereotype):

```java
@Repository
public class CategoryRepositoryImpl implements CategoryRepository {
    // ...
}
```

---

## Testing Strategy

### Unit Test — UseCase (Domain Layer)
```java
@Test
void shouldThrowWhenParentNotFound() {
    CategoryRepository mockRepo = mock(CategoryRepository.class);
    when(mockRepo.exists(99L)).thenReturn(false);

    CreateCategoryUseCase useCase = new CreateCategoryUseCase(mockRepo);
    CreateCategoryCommand command = new CreateCategoryCommand("Test", "test", null, 99L, true);

    assertThrows(CategoryNotFoundException.class, () -> useCase.execute(command));
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
                {"name": "Electronics", "slug": "electronics", "isVisible": true}
            """))
            .andExpect(status().isCreated());
    }
}
```
