# E-commerce Test Cases

This document defines functional backend test cases for the current NitroTech API e-commerce implementation.

Scope:

- `auth`
- `product`
- `cart`
- `order`
- `payment`
- `shipping`
- `inventory`

Document basis:

- Current source code in the repository
- [.docs/ECOMMERCE-BUSINESS-FLOW.md](D:/VSI/CODE/CD-WEB/DA/nitrotech/nitrotech-api/.docs/ECOMMERCE-BUSINESS-FLOW.md)
- [.docs/MODULE-STATUS-ANALYSIS.md](D:/VSI/CODE/CD-WEB/DA/nitrotech/nitrotech-api/.docs/MODULE-STATUS-ANALYSIS.md)

Rules for this document:

- Only test cases for flows visible in the current source code are included
- No target-state or future features are assumed
- Test cases are written from a backend functional and e-commerce verification perspective

## Priority legend

- `P0`: Critical business flow or high financial or operational risk
- `P1`: Main business flow or major customer-facing behavior
- `P2`: Important supporting flow
- `P3`: Nice-to-have or lower operational impact

## Auth

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `AUTH-001` | `auth` | Xác minh đăng ký tài khoản mới thành công | Email chưa tồn tại trong hệ thống | 1. Gọi API đăng ký với `name`, `email`, `password` hợp lệ 2. Kiểm tra response 3. Kiểm tra user mới được tạo | Response trả `201 Created`; trả dữ liệu user cơ bản; user được tạo với trạng thái chưa active; token verify email được tạo | `P0` |
| `AUTH-002` | `auth` | Xác minh đăng ký thất bại khi email đã tồn tại | Đã có user với email đó | 1. Gọi API đăng ký bằng email đã tồn tại | Response lỗi conflict hoặc domain error theo handler; không tạo user mới | `P0` |
| `AUTH-003` | `auth` | Xác minh đăng nhập thành công với tài khoản active | User tồn tại, password đúng, trạng thái `active` | 1. Gọi API login với email và password đúng 2. Kiểm tra session được tạo | Response `200 OK`; trả profile và quyền; session security context được lưu | `P0` |
| `AUTH-004` | `auth` | Xác minh đăng nhập thất bại khi sai mật khẩu | User tồn tại, trạng thái `active` | 1. Gọi API login với password sai | Response lỗi invalid credentials; không tạo session | `P0` |
| `AUTH-005` | `auth` | Xác minh đăng nhập thất bại khi tài khoản chưa active | User tồn tại, password đúng, trạng thái `inactive` | 1. Gọi API login | Response lỗi account not active | `P0` |
| `AUTH-006` | `auth` | Xác minh lấy thông tin `me` khi chưa đăng nhập | Không có session | 1. Gọi API `/api/auth/me` | Response thành công với dữ liệu user rỗng theo behavior hiện tại | `P2` |
| `AUTH-007` | `auth` | Xác minh lấy thông tin `me` khi đã đăng nhập | Có session hợp lệ | 1. Login 2. Gọi API `/api/auth/me` | Response trả đúng user hiện tại | `P1` |
| `AUTH-008` | `auth` | Xác minh cập nhật profile thành công | User đã đăng nhập | 1. Gọi API update profile với `name`, `phone`, `avatar` 2. Gọi lại `me` | Profile được cập nhật đúng | `P2` |
| `AUTH-009` | `auth` | Xác minh đổi mật khẩu thành công | User đã đăng nhập, biết mật khẩu hiện tại | 1. Gọi API change-password với current password đúng 2. Logout 3. Login bằng mật khẩu mới | Login bằng mật khẩu mới thành công; mật khẩu cũ không dùng được | `P1` |
| `AUTH-010` | `auth` | Xác minh quên mật khẩu với email tồn tại | User tồn tại | 1. Gọi API forgot-password với email hợp lệ 2. Kiểm tra token reset được tạo | Response thành công; token reset được tạo; email reset được gửi qua email sender | `P1` |
| `AUTH-011` | `auth` | Xác minh reset mật khẩu thành công với token hợp lệ | Có token reset chưa dùng và chưa hết hạn | 1. Gọi API reset-password với token hợp lệ và mật khẩu mới 2. Login lại bằng mật khẩu mới | Password được cập nhật; token được đánh dấu đã dùng hoặc không còn hiệu lực | `P1` |
| `AUTH-012` | `auth` | Xác minh verify email thành công với token hợp lệ | Có verification token hợp lệ | 1. Gọi API verify-email với token 2. Login bằng tài khoản đó | User được active; login thành công nếu password đúng | `P1` |
| `AUTH-013` | `auth` | Xác minh resend verification hoạt động | User chưa active | 1. Gọi API resend-verification với email đó | Response thành công; token verify mới được tạo hoặc email xác thực được gửi lại theo logic hiện tại | `P2` |
| `AUTH-014` | `auth` | Xác minh logout current session thành công | User đã đăng nhập | 1. Gọi API logout 2. Gọi lại endpoint cần auth | Session hiện tại bị hủy; endpoint cần auth trả unauthorized | `P1` |
| `AUTH-015` | `auth` | Xác minh logout-all hủy các session của cùng user | User có nhiều session hoạt động | 1. Đăng nhập trên nhiều session 2. Gọi logout-all từ một session 3. Kiểm tra các session còn lại | Tất cả session của user bị vô hiệu hóa theo logic hiện tại | `P2` |

## Product

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `PROD-001` | `product` | Xác minh xem danh sách sản phẩm public | Có sản phẩm active và visible | 1. Gọi API list products không cần auth | Chỉ trả sản phẩm public đang hiển thị; response có phân trang | `P0` |
| `PROD-002` | `product` | Xác minh xem chi tiết sản phẩm theo `id` | Có sản phẩm visible | 1. Gọi API product detail bằng `id` | Trả đúng sản phẩm | `P1` |
| `PROD-003` | `product` | Xác minh xem chi tiết sản phẩm theo `slug` | Có sản phẩm visible với slug hợp lệ | 1. Gọi API product detail bằng `slug` | Trả đúng sản phẩm | `P1` |
| `PROD-004` | `product` | Xác minh lọc sản phẩm theo category | Có nhiều sản phẩm thuộc category khác nhau | 1. Gọi API list với query `category` | Chỉ trả sản phẩm thuộc category yêu cầu | `P1` |
| `PROD-005` | `product` | Xác minh lọc sản phẩm theo brand | Có nhiều brand | 1. Gọi API list với query `brand` | Chỉ trả sản phẩm thuộc brand được chọn | `P1` |
| `PROD-006` | `product` | Xác minh lọc sản phẩm theo khoảng giá | Có sản phẩm ở nhiều mức giá | 1. Gọi API list với `minPrice` và `maxPrice` | Chỉ trả sản phẩm nằm trong khoảng giá | `P1` |
| `PROD-007` | `product` | Xác minh tìm kiếm sản phẩm theo từ khóa | Có sản phẩm có tên hoặc dữ liệu phù hợp từ khóa | 1. Gọi API list với `search` | Kết quả tìm kiếm phù hợp điều kiện hiện tại của repository | `P1` |
| `PROD-008` | `product` | Xác minh API facets trả tập filter hiện có | Có dữ liệu category, brand, badge, price range | 1. Gọi API `/api/products/facets` | Trả cấu trúc facets hiện tại theo filter đầu vào | `P2` |
| `PROD-009` | `product` | Xác minh API related products | Có sản phẩm liên quan trong dữ liệu | 1. Gọi API related bằng `productId` | Trả danh sách related products với giới hạn đúng | `P2` |
| `PROD-010` | `product` | Xác minh sản phẩm không public không xuất hiện ở luồng public | Có sản phẩm inactive hoặc deleted | 1. Gọi list public 2. Gọi detail public với slug hoặc id của sản phẩm không visible | Sản phẩm đó không xuất hiện trong list; detail trả not found hoặc không truy cập được | `P0` |

## Cart

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `CART-001` | `cart` | Xác minh lấy giỏ hàng trống cho user mới | User đã đăng nhập, chưa có cart hoặc cart chưa có item | 1. Gọi API get cart | Hệ thống tự tạo cart nếu cần; trả giỏ hàng rỗng và summary mặc định | `P1` |
| `CART-002` | `cart` | Xác minh thêm variant vào giỏ hàng thành công | User đã đăng nhập; variant tồn tại; tồn kho đủ | 1. Gọi API add-to-cart với `variantId`, `quantity` hợp lệ 2. Gọi lại get cart | Item được thêm vào cart; summary cập nhật | `P0` |
| `CART-003` | `cart` | Xác minh thêm cùng variant làm tăng số lượng thay vì tạo dòng mới | User đã đăng nhập; variant đã có trong cart; tồn kho đủ cho tổng số lượng mới | 1. Add variant lần 1 2. Add cùng variant lần 2 | Cart vẫn chỉ có một dòng item cho variant đó; quantity là tổng mới | `P0` |
| `CART-004` | `cart` | Xác minh thêm vào giỏ thất bại khi variant không tồn tại | User đã đăng nhập; variantId không hợp lệ | 1. Gọi add-to-cart với variant không tồn tại | Response not found hoặc lỗi domain tương ứng | `P0` |
| `CART-005` | `cart` | Xác minh thêm vào giỏ thất bại khi tồn kho không đủ | User đã đăng nhập; variant tồn tại; số lượng yêu cầu vượt tồn kho | 1. Gọi add-to-cart với quantity vượt stock | Response lỗi `INSUFFICIENT_STOCK`; cart không bị thay đổi sai | `P0` |
| `CART-006` | `cart` | Xác minh cập nhật số lượng item thành công | User đã đăng nhập; item đã có trong cart; tồn kho đủ | 1. Gọi update cart item với quantity mới 2. Gọi get cart | Quantity được cập nhật đúng | `P1` |
| `CART-007` | `cart` | Xác minh cập nhật số lượng thất bại khi vượt tồn kho | User đã đăng nhập; item đã có trong cart; quantity mới vượt stock | 1. Gọi update cart item | Response lỗi `INSUFFICIENT_STOCK`; quantity cũ giữ nguyên | `P0` |
| `CART-008` | `cart` | Xác minh xóa một item khỏi giỏ | User đã đăng nhập; cart có item | 1. Gọi delete item theo `variantId` 2. Gọi get cart | Item bị xóa khỏi cart; summary cập nhật | `P1` |
| `CART-009` | `cart` | Xác minh clear cart | User đã đăng nhập; cart có nhiều item | 1. Gọi clear cart 2. Gọi lại get cart | Tất cả item bị xóa; cart rỗng | `P1` |
| `CART-010` | `cart` | Xác minh summary hiện tại chưa tính discount và shipping fee | User đã đăng nhập; cart có item | 1. Gọi get cart | `discountAmount = 0`, `shippingFee = 0`, `finalTotal = subtotal` theo behavior hiện tại | `P1` |

## Order

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `ORD-001` | `order` | Xác minh checkout thành công với địa chỉ snapshot | User đã đăng nhập; cart có item; tồn kho đủ | 1. Gọi create order với `shippingAddress` đầy đủ, payment method hợp lệ 2. Kiểm tra order response 3. Kiểm tra cart | Order được tạo ở trạng thái `pending`; items được snapshot từ cart; cart bị clear | `P0` |
| `ORD-002` | `order` | Xác minh checkout thành công với địa chỉ lưu sẵn | User đã đăng nhập; có `addressId` hợp lệ thuộc user; cart có item; tồn kho đủ | 1. Gọi create order với `addressId` 2. Kiểm tra shipping address trong order | Order dùng dữ liệu address của user làm snapshot | `P0` |
| `ORD-003` | `order` | Xác minh checkout thất bại khi cart rỗng | User đã đăng nhập; cart rỗng | 1. Gọi create order | Response lỗi `CART_EMPTY`; không tạo order | `P0` |
| `ORD-004` | `order` | Xác minh checkout thất bại khi item trong cart thiếu hàng | User đã đăng nhập; cart có item; một hoặc nhiều variant không đủ stock | 1. Gọi create order | Response lỗi `INSUFFICIENT_STOCK`; không tạo order; cart không bị clear | `P0` |
| `ORD-005` | `order` | Xác minh checkout thất bại khi `addressId` không tồn tại hoặc không thuộc user | User đã đăng nhập; cart có item; `addressId` sai | 1. Gọi create order chỉ với `addressId` sai | Response `ADDRESS_NOT_FOUND`; không tạo order | `P0` |
| `ORD-006` | `order` | Xác minh order list của customer chỉ trả đơn của chính user | Có nhiều order của nhiều user | 1. Login user A 2. Gọi list orders | Chỉ trả đơn thuộc user A | `P1` |
| `ORD-007` | `order` | Xác minh customer xem chi tiết đơn của chính mình | User có order của chính mình | 1. Gọi get order theo `id` | Trả đúng order | `P1` |
| `ORD-008` | `order` | Xác minh customer không xem được đơn của user khác | Có order của user khác | 1. Login user A 2. Gọi get order của user B | Response not found hoặc access denied theo logic hiện tại | `P0` |
| `ORD-009` | `order` | Xác minh customer hủy đơn ở trạng thái `pending` | User có order `pending` của chính mình | 1. Gọi cancel order | Order chuyển sang `cancelled`; audit log được ghi | `P0` |
| `ORD-010` | `order` | Xác minh customer hủy đơn ở trạng thái `confirmed` | User có order `confirmed` của chính mình | 1. Gọi cancel order | Order chuyển sang `cancelled`; audit log được ghi | `P0` |
| `ORD-011` | `order` | Xác minh customer không hủy được đơn ở trạng thái `processing` trở đi | User có order `processing`, `shipped` hoặc `delivered` | 1. Gọi cancel order | Response lỗi `ORDER_CANNOT_CANCEL` | `P0` |
| `ORD-012` | `order` | Xác minh admin cập nhật trạng thái đơn theo flow hợp lệ | Có order ở trạng thái cho phép chuyển tiếp | 1. Gọi API admin update status theo các cặp hợp lệ như `pending -> confirmed`, `processing -> shipped` | Order đổi trạng thái thành công; audit log được ghi | `P1` |
| `ORD-013` | `order` | Xác minh admin cập nhật trạng thái đơn theo flow không hợp lệ bị từ chối | Có order ở trạng thái `pending` | 1. Gọi update status sang `shipped` | Response lỗi `INVALID_STATUS_TRANSITION` | `P1` |
| `ORD-014` | `order` | Xác minh order pending hết hạn sau 15 phút qua scheduler/use case | Có order `pending` quá thời gian timeout | 1. Chạy use case expire pending orders 2. Kiểm tra order | Order được chuyển `expired` theo logic hiện tại | `P0` |
| `ORD-015` | `order` | Xác minh checkout hiện tại lưu `promotionCode` nhưng chưa áp dụng discount | User đã đăng nhập; cart có item; gửi `promotionCode` | 1. Tạo order với promotionCode 2. Kiểm tra totals | `promotionCode` được lưu; `discountAmount = 0`; `finalAmount = totalAmount` nếu không có shipping fee | `P0` |
| `ORD-016` | `order` | Xác minh checkout hiện tại đang để `shippingFee = 0` | User đã đăng nhập; cart có item | 1. Tạo order 2. Kiểm tra amounts | `shippingFee = 0`; `finalAmount = totalAmount - discount + shipping` theo giá trị thực hiện tại | `P0` |

## Payment

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `PAY-001` | `payment` | Xác minh webhook SePay xác nhận đơn `pending` khi amount khớp | Có order `pending` với payment method `sepay`; chưa có transaction ref trùng | 1. Gửi webhook SePay hợp lệ với amount đúng và mã đơn đúng 2. Kiểm tra payment transaction 3. Kiểm tra order | Payment transaction được lưu với trạng thái `paid`; order chuyển từ `pending` sang `confirmed` | `P0` |
| `PAY-002` | `payment` | Xác minh webhook SePay không confirm đơn nếu amount không khớp | Có order `pending`; transaction ref chưa tồn tại | 1. Gửi webhook SePay inbound nhưng amount khác `finalAmount` | Payment transaction được lưu với trạng thái `mismatch`; order không chuyển `confirmed` | `P0` |
| `PAY-003` | `payment` | Xác minh webhook SePay không confirm đơn nếu transfer là outgoing | Có order `pending`; transaction ref chưa tồn tại | 1. Gửi webhook với `transferType = out` | Payment transaction được lưu `mismatch`; order không được confirm | `P1` |
| `PAY-004` | `payment` | Xác minh duplicate webhook SePay bị bỏ qua | Đã có transaction cùng `provider` và `providerRef` | 1. Gửi lại webhook y hệt lần trước | Response thành công nhưng transaction mới không được tạo; order không bị cập nhật lặp | `P0` |
| `PAY-005` | `payment` | Xác minh webhook SePay bị từ chối khi API key sai | Có payload webhook hợp lệ về mặt format nhưng header auth sai | 1. Gửi webhook với Authorization sai | Response lỗi unauthorized webhook hoặc forbidden theo handler | `P0` |
| `PAY-006` | `payment` | Xác minh webhook SePay bị ignore khi không trích được order ID | Payload không chứa payment code hợp lệ | 1. Gửi webhook thiếu mã order trong content/code | Response báo ignored; không tạo payment transaction; không cập nhật order | `P1` |
| `PAY-007` | `payment` | Xác minh webhook SePay bị ignore khi order không tồn tại | Payload có mã order đúng format nhưng order không có trong DB | 1. Gửi webhook | Response báo ignored; không cập nhật order | `P1` |
| `PAY-008` | `payment` | Xác minh đơn không bị confirm lại nếu trạng thái không còn `pending` | Có order ở trạng thái `expired` hoặc `confirmed`; amount khớp | 1. Gửi webhook SePay hợp lệ | Transaction vẫn được lưu theo logic; order không chạy lại confirm nếu đã không còn `pending` | `P1` |

## Shipping

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `SHIP-001` | `shipping` | Xác minh admin tạo shipment thành công với order `confirmed` | Có order `confirmed`; chưa có shipment; provider hợp lệ | 1. Gọi API create shipment 2. Kiểm tra shipment record 3. Kiểm tra shipment log | Shipment được tạo; có tracking code; status khởi tạo đúng; có log ban đầu | `P0` |
| `SHIP-002` | `shipping` | Xác minh admin tạo shipment thành công với order `processing` | Có order `processing`; chưa có shipment | 1. Gọi API create shipment | Shipment được tạo thành công | `P1` |
| `SHIP-003` | `shipping` | Xác minh không tạo shipment khi order đang `pending` | Có order `pending`; chưa có shipment | 1. Gọi create shipment | Response lỗi `INVALID_ORDER_STATUS` | `P0` |
| `SHIP-004` | `shipping` | Xác minh không tạo shipment trùng cho cùng order | Đã có shipment của order đó | 1. Gọi create shipment lần 2 | Response lỗi `SHIPMENT_ALREADY_EXISTS` | `P0` |
| `SHIP-005` | `shipping` | Xác minh lấy shipment của order | Order đã có shipment | 1. Gọi API get order shipment | Trả đúng shipment và logs theo cấu trúc hiện tại | `P1` |
| `SHIP-006` | `shipping` | Xác minh webhook GHN cập nhật shipment sang trạng thái in-transit | Có shipment GHN tồn tại | 1. Gửi webhook GHN với status như `delivering` | Shipment được map sang status nội bộ đúng; cập nhật `shippedAt` nếu phù hợp; thêm shipment log | `P0` |
| `SHIP-007` | `shipping` | Xác minh webhook GHN delivered cập nhật order sang `delivered` | Có shipment GHN thuộc order đang `processing` hoặc `shipped` | 1. Gửi webhook GHN delivered 2. Kiểm tra shipment và order | Shipment thành `delivered`; order chuyển `delivered` | `P0` |
| `SHIP-008` | `shipping` | Xác minh webhook GHTK status `5` được map sang delivered | Có shipment GHTK tồn tại | 1. Gửi webhook GHTK với `status_id = 5` | Shipment thành `delivered`; `deliveredAt` được set; có shipment log | `P0` |
| `SHIP-009` | `shipping` | Xác minh webhook GHTK hoặc GHN thiếu tracking code hoặc status bị từ chối | Không cần dữ liệu đặc biệt | 1. Gửi webhook thiếu mã tracking hoặc thiếu status | Response lỗi `INVALID_SHIPPING_WEBHOOK` | `P1` |
| `SHIP-010` | `shipping` | Xác minh webhook shipping với shipment không tồn tại bị từ chối | Không có shipment khớp provider và tracking code | 1. Gửi webhook GHN hoặc GHTK với tracking code lạ | Response `SHIPMENT_NOT_FOUND` | `P1` |
| `SHIP-011` | `shipping` | Xác minh shipment in-transit có thể đẩy order từ `confirmed` sang `processing` | Có shipment thuộc order `confirmed` | 1. Gửi webhook shipping với trạng thái in-transit | Order được cập nhật sang `processing` | `P0` |

## Inventory

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `INV-001` | `inventory` | Xác minh xem tồn kho theo variant | Có variant tồn tại | 1. Gọi API get inventory theo `variantId` | Trả đúng quantity, threshold, low stock flag | `P1` |
| `INV-002` | `inventory` | Xác minh xem danh sách low-stock | Có variant với quantity <= threshold | 1. Gọi API low-stock | Trả đúng các variant low-stock | `P1` |
| `INV-003` | `inventory` | Xác minh tăng tồn kho thành công | Có variant tồn tại | 1. Gọi adjust inventory với delta dương 2. Gọi lại get inventory | Quantity tăng đúng | `P1` |
| `INV-004` | `inventory` | Xác minh giảm tồn kho thành công khi đủ hàng | Có variant tồn tại với quantity đủ lớn | 1. Gọi adjust inventory với delta âm nhưng không làm quantity âm | Quantity giảm đúng | `P1` |
| `INV-005` | `inventory` | Xác minh giảm tồn kho thất bại khi quantity sẽ âm | Có variant tồn tại với quantity nhỏ | 1. Gọi adjust inventory với delta âm vượt quá lượng hiện có | Response lỗi `INSUFFICIENT_STOCK`; quantity không âm | `P0` |
| `INV-006` | `inventory` | Xác minh set quantity thành công | Có variant tồn tại | 1. Gọi set inventory quantity với số không âm | Quantity được set đúng | `P1` |
| `INV-007` | `inventory` | Xác minh set quantity thất bại khi quantity âm | Có variant tồn tại | 1. Gọi set quantity âm | Response lỗi `INVALID_QUANTITY` | `P1` |
| `INV-008` | `inventory` | Xác minh set threshold thành công | Có variant tồn tại | 1. Gọi set inventory có `lowStockThreshold` | Threshold được cập nhật đúng | `P2` |
| `INV-009` | `inventory` | Xác minh checkout làm giảm tồn kho | Có cart item và stock đủ; chưa đặt đơn | 1. Ghi nhận stock ban đầu 2. Tạo order 3. Gọi lại inventory | Stock của từng variant trong order bị giảm đúng theo quantity | `P0` |
| `INV-010` | `inventory` | Xác minh add-to-cart và checkout đều dùng kiểm tra tồn kho hiện tại | Có variant tồn kho nhỏ | 1. Add-to-cart với quantity trong ngưỡng 2. Update cart vượt ngưỡng hoặc checkout khi không đủ hàng | Hệ thống chặn ở bước có quantity vượt stock | `P0` |

## High-risk test scenarios

These scenarios target known gaps or fragile areas in the current implementation and should be treated as focused regression and business-risk checks.

| ID | Module | Mục tiêu kiểm thử | Pre-condition | Steps | Expected result | Priority |
|------|--------|-------------------|---------------|-------|-----------------|----------|
| `RISK-001` | `order` | Xác minh promotion code hiện tại chưa được áp dụng vào discount | Có cart hợp lệ; gửi `promotionCode` bất kỳ | 1. Tạo order với `promotionCode` 2. Kiểm tra `discountAmount` và `finalAmount` | Order vẫn được tạo; `promotionCode` được lưu; `discountAmount = 0`; đây là gap hiện tại cần được ghi nhận, không phải bug nếu bám đúng code hiện tại | `P0` |
| `RISK-002` | `order` | Xác minh shipping fee hiện tại luôn bằng 0 khi checkout | Có cart hợp lệ | 1. Tạo order 2. Kiểm tra amounts | `shippingFee = 0`; đây là behavior hiện tại cần theo dõi như risk nghiệp vụ | `P0` |
| `RISK-003` | `payment` | Xác minh request checkout chấp nhận `vnpay` hoặc `momo` dù backend chưa có provider hoàn chỉnh | Có cart hợp lệ | 1. Tạo order với `paymentMethod = vnpay` 2. Tạo order với `paymentMethod = momo` 3. Kiểm tra luồng tiếp theo | Order có thể được tạo do request validation cho phép; nhưng không có flow provider hoàn chỉnh trong source hiện tại; ghi nhận risk mismatch giữa method được nhận và provider thực sự hỗ trợ | `P0` |
| `RISK-004` | `inventory` | Xác minh hủy đơn không hoàn kho trong trạng thái hiện tại | Có order vừa tạo từ cart và stock đã bị trừ | 1. Ghi nhận stock trước checkout 2. Tạo order 3. Hủy order ở trạng thái cho phép 4. Kiểm tra inventory | Stock không được cộng lại theo code hiện tại; đây là gap nghiệp vụ quan trọng | `P0` |
| `RISK-005` | `inventory` | Xác minh expire pending order không hoàn kho trong trạng thái hiện tại | Có order `pending` đã trừ kho lúc checkout | 1. Ghi nhận stock trước checkout 2. Tạo order 3. Chạy expire use case 4. Kiểm tra inventory | Order chuyển `expired`; stock không được cộng lại theo code hiện tại; đây là gap nghiệp vụ quan trọng | `P0` |
| `RISK-006` | `order` | Xác minh order total hiện tại không bao gồm quote ship và discount thực tế nên có thể lệch kỳ vọng thanh toán | Có cart hợp lệ | 1. Tạo order 2. So sánh subtotal và final amount | `finalAmount` hiện bằng tổng item nếu không có adjustment khác; risk là commercial total chưa đầy đủ | `P1` |
| `RISK-007` | `shipping` | Xác minh tạo shipment không tự động cập nhật shipping fee của order | Có order confirmed và tạo shipment thành công | 1. Tạo shipment 2. Kiểm tra order và shipment | Shipment có thể có fee riêng; order `shippingFee` không tự được đồng bộ lại trong checkout total hiện tại | `P1` |
| `RISK-008` | `payment` | Xác minh webhook paid đến sau khi order đã `expired` không confirm lại đơn | Có order đã `expired` | 1. Gửi webhook SePay amount đúng sau khi order expired | Transaction vẫn có thể được log; order không quay lại `confirmed`; cần kiểm tra rõ expectation vận hành | `P1` |

## Suggested execution order

Recommended first-pass execution order for backend functional QA:

1. `AUTH-001` to `AUTH-005`
2. `PROD-001` to `PROD-007`
3. `CART-002` to `CART-007`
4. `ORD-001` to `ORD-005`
5. `INV-009`, `INV-010`
6. `PAY-001` to `PAY-008`
7. `SHIP-001`, `SHIP-006`, `SHIP-007`, `SHIP-008`, `SHIP-011`
8. `ORD-009` to `ORD-014`
9. `RISK-001` to `RISK-008`

## Notes for testers

- Some test cases require direct DB inspection or repository-level verification because the current API surface does not expose all internal side effects
- Webhook tests should validate both the direct response and downstream order or shipment state changes
- High-risk scenarios are intentionally included even when the current result is a known gap, because they are important to verify and document consistently across environments

This document does not modify application code or runtime configuration.
