# NitroTech E-commerce Business Flow

This document describes the current e-commerce business flow implemented in the NitroTech API codebase. It reflects the backend behavior that exists today, including supported paths, business rules, and known gaps that may require further development.

This is an as-is document, not a target-state design.

## Scope

The document covers these customer and operations flows:

- Browse products
- Add to cart
- Checkout
- Payment
- Shipping
- Cancel order
- Complete order

Related modules in the current backend:

- `auth`
- `product`
- `cart`
- `order`
- `payment`
- `shipping`
- `inventory`
- `address`

## Business actors

- Customer: browses products, manages cart, places and cancels own orders
- Admin or staff: manages catalog, updates order status, creates shipments
- Payment provider: sends payment webhook callbacks
- Shipping provider: sends shipment status webhooks

## High-level order lifecycle

Current order statuses in the backend:

- `pending`
- `confirmed`
- `processing`
- `shipped`
- `delivered`
- `cancelled`
- `expired`
- `refunded`

Current supported status transitions:

- `pending` -> `confirmed`
- `pending` -> `cancelled`
- `confirmed` -> `processing`
- `confirmed` -> `cancelled`
- `processing` -> `shipped`
- `shipped` -> `delivered`
- `delivered` -> `refunded`

Important note:

- Shipment webhooks can also move the order to `processing` or `delivered`
- Pending orders are automatically expired after 15 minutes by scheduler logic

## 1. Browse products

### Business goal

Customers can discover active products, view details, search, filter, and see related products.

### Main API behavior

Public product browsing is available without authentication.

Supported actions:

- View product list
- View product detail by `id` or `slug`
- Search product picker results
- View product filter facets
- View related products

### Business rules

- Only visible products are returned to the public flow
- Product list supports search, category, brand, badge, and price range filters
- Product detail can be loaded by numeric `id` or public `slug`
- Related products are suggested from the catalog layer

### Data shown to customer

Product data currently includes:

- Product name and slug
- Description and short description
- Thumbnail and images
- Brand and category
- Variant list
- Variant price
- Variant attributes
- Manual merchandising badge

### Current limitations

- No recommendation, personalization, or recently viewed logic is implemented
- No customer-facing stock reservation during browsing
- No separate product publish workflow beyond active and soft-delete visibility

## 2. Add to cart

### Business goal

Authenticated customers can create and maintain a personal shopping cart based on product variants.

### Main API behavior

Cart operations require an authenticated user.

Supported actions:

- Get current cart
- Add variant to cart
- Update item quantity
- Remove item
- Clear cart

### Business rules

- Cart is user-scoped; each user has one cart
- Cart items are stored by `variantId`
- If the cart does not exist yet, it is created automatically
- The system checks whether the variant exists before adding
- The system validates stock before adding or increasing quantity
- If the same variant already exists in cart, quantity is increased instead of inserting a duplicate row

### Cart summary behavior

The cart summary currently returns:

- Total item count
- Subtotal
- Discount amount
- Shipping fee
- Final total

Current implementation detail:

- Discount amount is always `0`
- Shipping fee is always `0`
- Final total is currently equal to subtotal

### Current limitations

- No guest cart
- No cart merge after login
- No promotion application at cart level
- No shipping fee estimate in cart
- No test suite currently covers cart use cases directly

## 3. Checkout

### Business goal

Customer places an order from the current cart using a saved address or an address snapshot entered during checkout.

### Main API behavior

Checkout is triggered by order creation.

Customer provides:

- Saved `addressId`, or
- Full shipping address snapshot
- Payment method
- Optional promotion code
- Optional note

### Supported payment methods in request validation

- `cod`
- `vnpay`
- `momo`
- `sepay`

### Checkout business rules

- Customer must be authenticated
- Cart must not be empty
- Every cart item must have sufficient stock at checkout time
- If shipping address snapshot is not provided, `addressId` must exist and belong to the same user
- Order items are created from the current cart snapshot
- Order starts in `pending` status
- Cart is cleared after successful order creation
- Inventory is reduced immediately when order is placed

### Order totals in current implementation

Current checkout calculation is:

- `totalAmount` = sum of item subtotals
- `shippingFee` = `0`
- `discountAmount` = `0`
- `finalAmount` = `totalAmount`

### Important current gaps

- `promotionCode` is saved on the order but is not actually validated or applied during checkout
- Shipping fee is not calculated from carrier quote
- No pre-checkout shipment fee estimation exists
- Inventory is deducted immediately, but current cancel and expire flows do not restore stock

This means checkout is working as an MVP order placement flow, but not yet a complete commercial checkout flow.

## 4. Payment

### Business goal

The system records payment provider callbacks and confirms eligible pending orders after successful payment.

### Current implementation scope

The payment module is currently webhook-driven.

Implemented today:

- Payment provider abstraction
- SePay provider implementation
- Payment webhook endpoint
- Payment transaction logging
- Duplicate webhook protection
- Amount verification against order final amount
- Automatic order confirmation when payment is valid

### Current business flow

1. Customer places an order, usually with `sepay` or `cod`
2. External payment provider sends webhook callback
3. Backend verifies provider identity and parses the payload
4. Backend extracts order ID from payment content or code
5. Backend rejects duplicate provider references
6. Backend compares paid amount with order `finalAmount`
7. Backend stores a payment transaction log
8. If payment is valid and the order is still `pending`, the backend changes order status to `confirmed`

### Payment outcomes in current implementation

- Matching inbound payment: treated as successful payment
- Mismatched amount: stored as `mismatch`
- Duplicate callback: ignored
- Unknown order ID or missing order code: ignored

### Current limitations

- No customer-facing payment initialization endpoint is wired into the order flow
- `PaymentProvider.initiatePayment(...)` exists but is not exposed as a documented checkout step
- No retry or reconciliation process is visible in the current code
- No refund flow is implemented in payment module
- `vnpay` and `momo` are accepted in request validation but no provider implementation is present in current source

Operational conclusion:

- Payment confirmation via SePay webhook exists
- Full online payment orchestration is not yet complete

## 5. Shipping

### Business goal

After an order is confirmed or in processing state, staff can create a shipment with a carrier, then the system tracks delivery progress through shipping webhooks.

### Current implementation scope

Implemented today:

- Shipment record per order
- Shipment logs
- Shipping provider abstraction
- GHN integration
- GHTK integration
- Shipment creation from admin flow
- Shipping webhook processing
- Order status synchronization from shipment updates

### Shipment creation flow

1. Admin or staff selects an order
2. Backend checks that no shipment already exists for the order
3. Backend loads order details and shipping address snapshot
4. Order must be in `confirmed` or `processing` status
5. Backend sends shipment creation request to configured provider
6. Shipment is stored with tracking code, fee, estimated delivery, and initial shipment status
7. Shipment log is created

### Shipping provider behavior

Currently supported providers:

- `ghn`
- `ghtk`

Provider is selected by request parameter or default configuration.

### Shipping webhook flow

1. Shipping provider sends status callback
2. Backend identifies shipment by provider and tracking code
3. Raw provider status is mapped to internal shipment status
4. Shipment timeline fields such as `shippedAt` or `deliveredAt` are updated
5. Shipment log is appended
6. Related order status may be updated

### Shipment to order synchronization

Current synchronization rules:

- In-transit shipment can move order from `pending` or `confirmed` to `processing`
- Delivered shipment moves order to `delivered`
- Cancelled order is not changed by shipment sync

### Current limitations

- No customer-facing shipping quotation before order placement
- No carrier service selection by customer
- No return merchandise authorization or structured reverse logistics flow
- Shipment creation is an admin-side action, not an automatic checkout action

## 6. Cancel order

### Business goal

Customer can cancel own order before it enters later fulfillment stages.

### Current implementation scope

Customer cancellation is supported.

### Cancellation business rules

Customer can cancel only when order status is:

- `pending`
- `confirmed`

If order is already:

- `processing`
- `shipped`
- `delivered`
- `cancelled`
- `expired`

then customer cancellation is rejected.

### Current cancellation flow

1. Customer requests cancellation for own order
2. Backend verifies the order belongs to the current user
3. Backend checks current order status
4. If allowed, backend changes status to `cancelled`
5. Audit log is recorded

### Important limitation

Current cancellation does not restore inventory.

Because inventory is reduced during checkout, this creates an important business gap:

- Cancelled orders may continue to consume stock unless a later enhancement adds stock release logic

## 7. Complete order

### Business goal

An order is treated as completed when fulfillment finishes and the shipment is delivered.

### Current implementation scope

There is no separate `completed` status in the code. The practical completion state is `delivered`.

### Completion paths

Path 1: manual operational progression

1. Staff updates order status from `confirmed` to `processing`
2. Staff updates from `processing` to `shipped`
3. Staff updates from `shipped` to `delivered`

Path 2: shipping-driven completion

1. Shipment is created
2. Carrier sends delivery status webhook
3. Backend updates shipment to `delivered`
4. Backend updates related order to `delivered`

### Business meaning of delivered

Once an order is `delivered`, the system currently treats it as the fulfilled end state for the purchase journey.

After `delivered`, the only defined next transition is:

- `delivered` -> `refunded`

### Current limitations

- No separate customer confirmation step such as "received order"
- No return window or after-sales completion workflow
- No automatic review invitation linked from delivery completion in this document scope

## End-to-end flow summary

### Standard COD flow

1. Customer browses active products
2. Customer adds variants to cart
3. Customer checks out with address and `cod`
4. Backend validates stock and creates order in `pending`
5. Backend deducts inventory and clears cart
6. Staff reviews and confirms order
7. Staff creates shipment
8. Carrier webhook updates shipment progress
9. Shipment delivered
10. Order becomes `delivered`

### Standard SePay flow

1. Customer browses active products
2. Customer adds variants to cart
3. Customer checks out with `sepay`
4. Backend validates stock and creates order in `pending`
5. Backend deducts inventory and clears cart
6. SePay webhook arrives with valid payment
7. Backend logs payment and confirms order
8. Staff creates shipment
9. Carrier webhook updates shipment progress
10. Shipment delivered
11. Order becomes `delivered`

## Business gaps and recommendations

### Gaps that materially affect commerce correctness

- Promotion code is stored but not applied
- Shipping fee is not calculated during checkout
- Online payment initiation is not fully connected to customer checkout
- Inventory is not restored when order is cancelled or expired
- `vnpay` and `momo` are accepted as payment methods without provider implementations

### Gaps that affect user experience or operations

- No guest cart
- No cart merge after login
- No customer-facing shipment quote and ETA selection
- No explicit order completion confirmation from customer
- No structured return or exchange flow

### Suggested development priority

1. Restore inventory on cancellation and order expiry
2. Apply promotion validation and discount calculation during checkout
3. Add shipping fee estimation and persist actual checkout shipping fee
4. Complete payment initiation flow for non-COD methods
5. Add customer-facing post-purchase flows such as returns and receipt confirmation

## Source basis

This document is based on the current logic in these areas:

- `application/product`
- `application/cart`
- `application/order`
- `application/payment`
- `application/shipping`
- `domain/order/usecase`
- `domain/payment/usecase`
- `domain/shipping/usecase`
- `domain/cart/usecase`
- `domain/inventory/usecase`
- `infrastructure/payment`
- `infrastructure/shipping`
- `src/main/resources/db/migration`

Keep this document updated whenever checkout, payment, shipment, or inventory flows change.
