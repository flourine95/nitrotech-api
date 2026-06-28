# Module Status Analysis

This document analyzes the current backend module status of NitroTech API based on the source code that exists in the repository today.

Scope in this document:

- `auth`
- `product`
- `cart`
- `order`
- `payment`
- `shipping`
- `inventory`

Analysis principles:

- Only functions visible in the current source code are included
- No target-state assumptions are added
- Assessment is written from a BA and backend implementation perspective
- This is an as-is backend capability review

## Summary table

| Module | Chức năng đã có | Mức độ hoàn thiện | Điểm còn thiếu | Rủi ro nghiệp vụ | Đề xuất ưu tiên phát triển |
|------|------------------|-------------------|----------------|------------------|----------------------------|
| `auth` | Đăng ký, đăng nhập, logout, logout-all, quên mật khẩu, reset mật khẩu, xác thực email, gửi lại email xác thực, lấy profile, cập nhật profile, đổi mật khẩu, phân quyền theo role và permission | Khá hoàn thiện | Chưa thấy test riêng cho auth flow; chưa thấy social login; chưa thấy cơ chế khóa tài khoản theo hành vi; forgot-password hiện vẫn truy vấn user thật | Có nguy cơ lộ tồn tại email nếu behavior lỗi không được bao kín ở tầng ngoài; coverage kiểm thử cho auth chưa mạnh bằng order và shipping | Bổ sung test cho register, login, verify-email, forgot/reset password; rà lại behavior chống email enumeration; bổ sung audit cho auth nếu cần |
| `product` | Public listing, chi tiết sản phẩm, search, facets, related products, admin CRUD sản phẩm, CRUD variant, soft delete, restore, hard delete, filter theo category, brand, badge, price | Gần hoàn thiện | Chưa thấy test riêng cho product use case và repository trong phạm vi chính; chưa có pricing/promotion integration trực tiếp ở product; chưa có workflow publish sâu | Catalog là lõi bán hàng, thiếu test sâu có thể gây regression ở filter, visibility, variant và slug | Bổ sung test cho product query, variant lifecycle, slug uniqueness, soft delete scope và facet behavior |
| `cart` | Lấy giỏ hàng, thêm item theo variant, cập nhật số lượng, xóa item, clear cart, tự tạo cart theo user, kiểm tra tồn kho khi thêm hoặc tăng số lượng | MVP còn thiếu | Chưa có guest cart; chưa có merge cart sau login; summary chưa tính giảm giá và phí ship; chưa có apply promotion trong cart; chưa thấy test riêng | Dễ lệch kỳ vọng người dùng vì số tiền trong cart chưa phản ánh discount và shipping thật; thiếu guest cart ảnh hưởng conversion | Bổ sung test cart use case; thêm promotion preview; thêm shipping estimate; xem xét guest cart và merge cart |
| `order` | Đặt hàng từ cart, dùng địa chỉ lưu sẵn hoặc snapshot địa chỉ, danh sách đơn, chi tiết đơn, hủy đơn của customer, cập nhật trạng thái đơn, admin xem facets, scheduler hết hạn đơn pending, audit khi đổi trạng thái và hủy đơn | MVP còn thiếu nhưng nền tảng khá tốt | `promotionCode` mới được lưu chưa áp dụng; `shippingFee` đang bằng `0`; chưa hoàn kho khi hủy đơn hoặc hết hạn; chưa có return/refund business flow đầy đủ; online payment orchestration chưa gắn chặt vào checkout | Trừ kho ngay khi đặt đơn nhưng không hoàn kho khi cancel/expire có thể làm sai tồn kho; giá trị đơn chưa phản ánh phí ship và khuyến mãi thật | Ưu tiên hoàn kho khi cancel/expire; áp dụng promotion thật; tính phí ship ở checkout; bổ sung flow refund/return rõ ràng |
| `payment` | Payment provider abstraction, SePay provider, webhook nhận callback, verify callback, deduplicate transaction theo provider ref, log payment transaction, đối soát amount với order, auto confirm order khi thanh toán hợp lệ | Chưa hoàn thiện | Chưa có API khởi tạo thanh toán hoàn chỉnh cho checkout; `vnpay` và `momo` có trong validation nhưng chưa có provider implementation; chưa có refund flow; chưa có reconciliation chủ động | Người dùng có thể chọn payment method chưa được backend hỗ trợ đầy đủ; checkout online chưa trọn luồng từ tạo đơn đến trả payment URL/QR theo API công khai | Hoàn thiện payment initiation flow; hoặc siết validation chỉ cho provider đã hỗ trợ; bổ sung refund và reconciliation |
| `shipping` | Shipping provider abstraction, GHN, GHTK, tạo shipment từ admin, lưu shipment và shipment log, webhook cập nhật trạng thái, map trạng thái carrier sang trạng thái nội bộ, đồng bộ trạng thái order theo shipment | Gần hoàn thiện | Chưa có quote phí ship trước checkout; chưa có chọn service level cho khách; chưa có reverse logistics/return shipment flow; shipment creation chưa tự động từ checkout | Order total và shipping experience chưa đồng nhất vì phí ship không đi cùng luồng checkout; xử lý sau bán hàng còn thiếu | Bổ sung shipping quote trước checkout; persist shipping fee thực tế vào order; định nghĩa return shipment flow nếu cần |
| `inventory` | Xem tồn kho theo variant, xem low-stock, điều chỉnh tăng giảm tồn kho, set quantity, set low-stock threshold, kiểm tra đủ hàng khi add cart và checkout | MVP còn thiếu | Chưa có reservation/release stock theo lifecycle đơn hàng; chưa có lịch sử biến động kho; chưa có đa kho; chưa thấy test riêng | Sai lệch tồn kho là rủi ro lớn nhất, đặc biệt vì chưa có hoàn kho khi cancel/expire; thiếu stock ledger gây khó audit | Ưu tiên release stock khi hủy/hết hạn; thêm inventory movement log; bổ sung test inventory use case và integration với order |

## Detailed module notes

## `auth`

### Current capability

Current source code supports these backend auth flows:

- Customer registration
- Login with Spring Security session
- Logout current session
- Logout all sessions by principal
- Forgot password
- Reset password by token
- Verify email by token
- Resend verification email
- Get current profile
- Update profile
- Change password

Current backend building blocks also include:

- `users`
- `user_tokens`
- role and permission model
- `UserPrincipal`
- `SecurityConfig`
- email sender integration through `ResendEmailSender`

### BA and backend assessment

This module is functionally broad enough for a normal account-based e-commerce MVP. The missing part is not the number of endpoints, but the lack of visible dedicated automated test coverage compared with order and shipping.

### Main concern

`ForgotPasswordUseCase` currently loads the user and throws `EmailNotFoundException` if absent. The controller returns a generic message, but the implementation should still be reviewed carefully to avoid inconsistent behavior across future callers.

## `product`

### Current capability

Current source code supports:

- Public product listing
- Public product detail
- Search endpoint
- Product facets for filtering
- Related products
- Admin CRUD for products
- Admin CRUD for variants
- Soft delete, restore, hard delete
- Filter by category, brand, badge, price range

It also has:

- product images
- product variants
- manual badge
- search index migrations
- repository and specification layer for richer querying

### BA and backend assessment

This is one of the strongest modules in the repo. It has both customer-facing and admin-facing APIs, and the persistence side is relatively rich. From a BA view, this is already a usable catalog backbone.

### Main concern

The module looks deep in implementation, but automated test visibility for core product flows is weaker than the amount of behavior suggests.

## `cart`

### Current capability

Current source code supports:

- Persistent cart per authenticated user
- Add variant to cart
- Increase quantity when same variant already exists
- Update item quantity
- Remove item
- Clear cart
- Return cart summary
- Validate stock before adding or increasing quantity

### BA and backend assessment

This is a working authenticated cart flow for an MVP. It is enough to support direct purchase from logged-in customers, but it is still operationally thin.

### Main concern

Cart totals do not yet represent final commercial value because:

- discount is always `0`
- shipping fee is always `0`

So the customer sees a subtotal-oriented cart, not a real pre-checkout commercial summary.

## `order`

### Current capability

Current source code supports:

- Place order from current cart
- Use saved address or checkout address snapshot
- Create order items from cart snapshot
- List own orders
- View own order detail
- Cancel own order
- Admin list and filter orders
- Admin view order facets
- Admin update order status
- Auto expire pending orders after 15 minutes

Current status model supports:

- `pending`
- `confirmed`
- `processing`
- `shipped`
- `delivered`
- `cancelled`
- `expired`
- `refunded`

### BA and backend assessment

This is a solid order lifecycle foundation and is better covered by tests than many other modules. However, the financial and inventory correctness of the flow is not fully complete yet.

### Main concerns

- Promotion code is stored but not applied
- Shipping fee is not calculated
- Stock is deducted immediately during order placement
- Stock is not restored when order is cancelled or expired

These gaps are material business risks, not just missing nice-to-have features.

## `payment`

### Current capability

Current source code supports:

- Provider abstraction for payments
- SePay provider implementation
- Payment webhook endpoint
- Authorization and payload verification in SePay flow
- Duplicate transaction detection
- Payment transaction persistence
- Order confirmation when valid payment matches pending order

### BA and backend assessment

Payment is partially implemented. The backend can process a successful SePay callback and move the order forward, but it does not yet show a complete checkout payment orchestration from the customer side.

### Main concerns

- No clear public payment-init API wired into order placement flow
- `vnpay` and `momo` are accepted at request validation level without actual provider implementation in source
- No refund flow
- No broader reconciliation or retry strategy is visible

This means payment exists as a technical callback module, but not yet as a fully complete online payment module.

## `shipping`

### Current capability

Current source code supports:

- Shipping provider abstraction
- GHN integration
- GHTK integration
- Admin shipment creation
- Shipment persistence
- Shipment logs
- Shipping webhook handling
- Carrier status mapping
- Order status synchronization from shipment updates

### BA and backend assessment

Shipping is one of the most mature operational modules in the repository. The test coverage around provider behavior and webhook handling is also relatively strong.

### Main concerns

- No shipping quotation in checkout
- No customer-side carrier/service selection
- No reverse logistics flow for return shipment
- Shipping is still an admin-side fulfillment action, not a seamless checkout-side logistics flow

## `inventory`

### Current capability

Current source code supports:

- Get inventory by variant
- List low-stock items
- Adjust stock by delta
- Set absolute stock quantity
- Set low-stock threshold
- Validate sufficient stock during add-to-cart and checkout

### BA and backend assessment

Inventory is usable as a simple stock control module, but not yet a mature inventory domain.

### Main concerns

- No stock reservation model
- No stock release on order cancel or expire
- No movement history or stock ledger
- No multi-warehouse model

For e-commerce operations, these are significant gaps because inventory accuracy affects both sales and customer trust.

## Test and stability signals

From the current repository state:

- Order use case tests are present and passing
- Payment webhook tests are present and passing
- Shipping use case and provider tests are present and passing
- Product, cart, inventory, and auth do not show the same visible level of targeted test coverage in the current repo scan

One broader repository note:

- Current test suite is not fully green overall because `SoftDeletedSlugRepositoryTest` fails in the environment used during analysis
- That failure appears infrastructure and test-environment related, not a direct indicator that the seven modules in this document are functionally broken

## Overall ranking

### Gần hoàn thiện

- `product`
- `shipping`
- `auth`

### MVP còn thiếu

- `order`
- `cart`
- `inventory`

### Chưa hoàn thiện

- `payment`

## Priority recommendations across modules

Suggested cross-module development order:

1. Fix inventory correctness around order cancel and expire
2. Complete checkout money logic: promotion and shipping fee
3. Complete payment initiation flow for supported online payment methods
4. Strengthen automated tests for auth, product, cart, and inventory
5. Add post-purchase and reverse-logistics capabilities where business requires them

## Source basis

This analysis is based on current source code in:

- `src/main/java/com/nitrotech/api/application/auth`
- `src/main/java/com/nitrotech/api/application/product`
- `src/main/java/com/nitrotech/api/application/cart`
- `src/main/java/com/nitrotech/api/application/order`
- `src/main/java/com/nitrotech/api/application/payment`
- `src/main/java/com/nitrotech/api/application/shipping`
- `src/main/java/com/nitrotech/api/application/inventory`
- `src/main/java/com/nitrotech/api/domain`
- `src/main/java/com/nitrotech/api/infrastructure`
- `src/main/resources/db/migration`

This document does not modify application code or runtime configuration.
