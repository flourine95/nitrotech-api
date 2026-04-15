# Nitrotech — Hệ thống thương mại điện tử

## Tên đồ án

**Nitrotech** — Xây dựng hệ thống thương mại điện tử với kiến trúc Backend-for-Frontend (BFF)

---

## Mô tả tóm tắt

Nitrotech là hệ thống thương mại điện tử hoàn chỉnh gồm backend REST API và frontend web admin, hỗ trợ quản lý sản phẩm, đơn hàng, khách hàng và vận hành cửa hàng trực tuyến.

Backend được xây dựng theo kiến trúc **Domain-Oriented Layered Architecture**, tách biệt rõ ràng giữa business logic và infrastructure. Frontend Next.js đóng vai trò **BFF (Backend-for-Frontend)** — làm trung gian giữa browser và Spring Boot API, giúp ẩn token khỏi client và đơn giản hóa auth flow.

Hệ thống hỗ trợ đầy đủ các nghiệp vụ:
- Quản lý danh mục sản phẩm dạng cây với kéo thả
- Quản lý sản phẩm, biến thể, tồn kho
- Giỏ hàng, đặt hàng, theo dõi trạng thái đơn hàng
- Mã khuyến mãi, đánh giá sản phẩm, danh sách yêu thích
- Xác thực email, quản lý phiên đăng nhập đa thiết bị
- Upload ảnh lên Cloudinary

---

## Công nghệ sử dụng

### Backend

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| Java | 21 | Ngôn ngữ chính |
| Spring Boot | 4.0 | Framework chính |
| Spring Security | 6 | Xác thực, phân quyền |
| Spring Session Redis | — | Quản lý session server-side |
| Spring Data JPA | — | ORM, truy vấn DB |
| PostgreSQL | 17 | Cơ sở dữ liệu chính |
| Redis | 7 | Session store, cache |
| Flyway | — | Database migration |
| Lombok | — | Giảm boilerplate code |
| MapStruct | 1.6 | Object mapping |
| Springdoc OpenAPI | 3.0 | Tự động sinh API docs |
| Cloudinary | — | Lưu trữ và quản lý ảnh |
| Docker Compose | — | Môi trường phát triển |

### Frontend (Next.js BFF)

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| Next.js | 16 | Framework React, đóng vai trò BFF |
| React | 19 | UI library |
| TypeScript | — | Type safety |
| @dnd-kit | — | Kéo thả danh mục |
| Tailwind CSS | — | Styling |

### Công cụ & DevOps

| Công nghệ | Mục đích |
|---|---|
| Gradle | Build tool |
| Docker | Container hóa |
| Python | Script tự động sinh API docs |
| Git | Version control |

---

## Kiến trúc hệ thống

```
Browser
  ↓
Next.js 16 (BFF — Route Handlers)
  ↓
Spring Boot API
  ↓
PostgreSQL + Redis
```

- **Browser** chỉ giao tiếp với Next.js — không biết Spring Boot tồn tại
- **Next.js** forward request, quản lý SESSION cookie, xử lý auth
- **Spring Boot** xử lý business logic, lưu session vào Redis
- **PostgreSQL** lưu dữ liệu chính
- **Redis** lưu session và cache

---

## Các module chính

| Module | Chức năng |
|---|---|
| Auth | Đăng ký, đăng nhập, xác thực email, quản lý session |
| Category | Danh mục dạng cây, kéo thả sắp xếp |
| Brand | Thương hiệu sản phẩm |
| Product | Sản phẩm, biến thể, hình ảnh |
| Inventory | Quản lý tồn kho |
| Cart | Giỏ hàng |
| Order | Đặt hàng, theo dõi trạng thái |
| Promotion | Mã khuyến mãi |
| Review | Đánh giá sản phẩm |
| Wishlist | Danh sách yêu thích |
| Banner | Banner quảng cáo |
| Address | Địa chỉ giao hàng |
| Upload | Upload ảnh lên Cloudinary |

---

## Chức năng chi tiết

### Auth — Xác thực & Tài khoản
| Endpoint | Chức năng |
|---|---|
| POST /api/auth/register | Đăng ký tài khoản, gửi email xác thực |
| POST /api/auth/login | Đăng nhập, tạo session server-side |
| POST /api/auth/logout | Đăng xuất thiết bị hiện tại |
| POST /api/auth/logout-all | Đăng xuất tất cả thiết bị |
| POST /api/auth/verify-email | Xác thực email bằng token |
| POST /api/auth/resend-verification | Gửi lại email xác thực |
| POST /api/auth/forgot-password | Yêu cầu reset mật khẩu |
| POST /api/auth/reset-password | Reset mật khẩu, invalidate tất cả session |
| GET /api/auth/me | Lấy thông tin profile |
| PUT /api/auth/profile | Cập nhật tên, số điện thoại, avatar |
| PUT /api/auth/change-password | Đổi mật khẩu |

### Category — Danh mục sản phẩm
| Endpoint | Chức năng |
|---|---|
| GET /api/categories | Danh sách flat hoặc tree, filter, search, pagination |
| GET /api/categories/{id} | Chi tiết danh mục |
| POST /api/categories | Tạo danh mục, hỗ trợ nested (parentId) |
| PUT /api/categories/{id} | Cập nhật, validate circular reference |
| DELETE /api/categories/{id} | Soft delete, block nếu còn children |
| PATCH /api/categories/{id}/restore | Restore, check slug conflict |
| DELETE /api/categories/{id}/permanent | Hard delete, block nếu có products/children |
| PATCH /api/categories/move | Kéo thả: reorder và move cross-parent trong 1 request |

### Brand — Thương hiệu
| Endpoint | Chức năng |
|---|---|
| GET /api/brands | Danh sách, filter, search, sort, pagination |
| GET /api/brands/{id} | Chi tiết thương hiệu |
| POST /api/brands | Tạo thương hiệu |
| PUT /api/brands/{id} | Cập nhật |
| DELETE /api/brands/{id} | Soft delete |
| PATCH /api/brands/{id}/restore | Restore, check slug conflict |
| DELETE /api/brands/{id}/permanent | Hard delete, block nếu có products |

### Product — Sản phẩm & Biến thể
| Endpoint | Chức năng |
|---|---|
| GET /api/products | Danh sách, filter theo category/brand, search, pagination |
| GET /api/products/{id} | Chi tiết sản phẩm kèm variants và images |
| POST /api/products | Tạo sản phẩm kèm variants và images |
| PUT /api/products/{id} | Cập nhật sản phẩm |
| DELETE /api/products/{id} | Soft delete |
| POST /api/products/{id}/variants | Thêm biến thể |
| PUT /api/products/{id}/variants/{variantId} | Cập nhật biến thể |
| DELETE /api/products/{id}/variants/{variantId} | Xóa biến thể |

### Inventory — Tồn kho
| Endpoint | Chức năng |
|---|---|
| GET /api/inventory/variants/{variantId} | Xem tồn kho biến thể |
| GET /api/inventory/low-stock | Danh sách sắp hết hàng |
| PATCH /api/inventory/variants/{variantId}/adjust | Điều chỉnh tương đối (+/-) |
| PUT /api/inventory/variants/{variantId} | Set tuyệt đối số lượng và ngưỡng cảnh báo |

### Cart — Giỏ hàng
| Endpoint | Chức năng |
|---|---|
| GET /api/cart | Xem giỏ hàng |
| POST /api/cart/items | Thêm sản phẩm vào giỏ |
| PUT /api/cart/items/{variantId} | Cập nhật số lượng |
| DELETE /api/cart/items/{variantId} | Xóa item khỏi giỏ |
| DELETE /api/cart | Xóa toàn bộ giỏ |

### Order — Đơn hàng
| Endpoint | Chức năng |
|---|---|
| GET /api/orders | Danh sách đơn hàng, filter theo status |
| GET /api/orders/{id} | Chi tiết đơn hàng |
| POST /api/orders | Đặt hàng từ giỏ hàng |
| PATCH /api/orders/{id}/cancel | Hủy đơn hàng |
| PATCH /api/orders/{id}/status | Cập nhật trạng thái (Admin) |

### Promotion — Khuyến mãi
| Endpoint | Chức năng |
|---|---|
| GET /api/promotions/validate | Validate mã giảm giá trước checkout |
| GET /api/admin/promotions | Danh sách mã khuyến mãi (Admin) |
| POST /api/admin/promotions | Tạo mã khuyến mãi |
| PUT /api/admin/promotions/{id} | Cập nhật |
| PATCH /api/admin/promotions/{id}/status | Đổi trạng thái (draft/active/expired) |
| DELETE /api/admin/promotions/{id} | Xóa |

### Review — Đánh giá sản phẩm
| Endpoint | Chức năng |
|---|---|
| GET /api/products/{productId}/reviews | Danh sách review đã duyệt (Public) |
| POST /api/reviews | Tạo review (User) |
| GET /api/admin/reviews/pending | Review chờ duyệt (Admin) |
| PATCH /api/admin/reviews/{id}/approve | Duyệt review |
| PATCH /api/admin/reviews/{id}/reject | Từ chối review |

### Wishlist — Yêu thích
| Endpoint | Chức năng |
|---|---|
| GET /api/wishlist | Danh sách sản phẩm yêu thích |
| POST /api/wishlist/{productId} | Toggle thêm/xóa khỏi danh sách yêu thích |

### Banner — Quảng cáo
| Endpoint | Chức năng |
|---|---|
| GET /api/banners | Banner active theo vị trí (Public) |
| GET /api/banners/admin | Tất cả banner (Admin) |
| POST /api/banners | Tạo banner |
| PUT /api/banners/{id} | Cập nhật |
| DELETE /api/banners/{id} | Xóa |

### Address — Địa chỉ giao hàng
| Endpoint | Chức năng |
|---|---|
| GET /api/addresses | Danh sách địa chỉ của user |
| POST /api/addresses | Thêm địa chỉ mới |
| PUT /api/addresses/{id} | Cập nhật địa chỉ |
| PATCH /api/addresses/{id}/default | Đặt làm địa chỉ mặc định |
| DELETE /api/addresses/{id} | Xóa địa chỉ |

### Upload — Quản lý file
| Endpoint | Chức năng |
|---|---|
| POST /api/upload/sign | Tạo signature upload lên Cloudinary |
| GET /api/upload/folders | Danh sách folder trên Cloudinary |
| GET /api/upload/assets | Danh sách ảnh trong folder |

---

## Thống kê

- Tổng số module: 13
- Tổng số endpoint: 68
- Endpoint public: ~20
- Endpoint user (cần đăng nhập): ~25
- Endpoint admin: ~23
