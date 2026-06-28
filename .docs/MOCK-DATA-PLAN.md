# Mock Data Plan

This document analyzes and proposes mock data for the NitroTech system based on the current source code and the existing `.docs` materials.

Purpose of this document:

- support demo environments
- support backend functional QA
- support BA review of business flows
- align mock data with the actual current backend model

This is a mock data planning document only.

- No application code is changed
- No seed SQL is created here
- No runtime configuration is changed

## Scope

Data groups covered:

- users
- roles and permissions
- categories
- brands
- products
- product variants
- inventory
- cart
- orders
- payments
- shipments
- promotions
- reviews
- wishlist
- addresses

## Planning principles

- Only propose data that matches current source structure and business flows
- Prioritize data that supports demo and functional testing
- Use technology retail examples suitable for NitroTech:
  - CPU
  - GPU
  - RAM
  - SSD
  - laptop
  - monitor
  - keyboard
  - mouse
  - accessories
- Include both happy-path and risk-path data
- Keep relationships realistic across catalog, cart, order, payment, shipping, and inventory

## Demo and QA goals

The mock data should be enough to demonstrate and test:

1. public product browsing
2. filtering by category, brand, and price
3. choosing different product variants
4. stock checks in cart and checkout
5. creating orders by snapshot address or saved address
6. COD and SePay order flows
7. admin order management
8. shipment creation and webhook updates
9. wishlist and review examples
10. edge cases such as low stock, payment mismatch, and invalid tracking code

## Recommended environment data size

Suggested minimum size for a useful demo and QA dataset:

- 3 to 5 users
- 3 roles
- 20 to 35 permissions from current seeded permission model
- 8 to 12 categories
- 8 to 10 brands
- 18 to 30 products
- 30 to 50 variants
- inventory for every variant
- 2 to 4 active carts
- 8 to 15 orders in mixed statuses
- 3 to 6 payment transactions
- 3 to 6 shipments
- 2 to 4 promotions
- 4 to 8 reviews
- 3 to 6 wishlist items
- 4 to 8 addresses

## Data group plan

## Users

### Purpose

Support authentication, customer checkout, admin operations, permissions, wishlist, reviews, addresses, carts, and orders.

### Recommended sample data

Minimum user types:

- `admin@nitrotech.vn`
- `staff.ops@nitrotech.vn`
- `customer1@example.com`
- `customer2@example.com`
- `customer3@example.com`

Suggested user states:

- 1 active admin
- 1 active staff
- 2 active customers
- 1 inactive customer for email verification testing

Suggested profile fields:

- full name
- email
- phone
- avatar URL or null
- provider `local`
- status `active` or `inactive`

### Relationships

- linked to `user_roles`
- owns `addresses`
- owns `cart`
- owns `orders`
- can own `wishlists`
- can write `reviews`
- can receive password reset and email verification tokens

### Test and demo flow supported

- register
- login
- verify email
- forgot password
- role-based admin access
- customer order history

## Roles and permissions

### Purpose

Support authorization for admin and staff operations.

### Recommended sample data

Recommended roles:

- `admin`
- `staff`
- `customer`

Recommended role meaning:

- `admin`: broad dashboard and management permissions
- `staff`: operational permissions for order, shipment, inventory, and product read or update depending on current seeded model
- `customer`: own-profile, own-order, cart, wishlist flows

Permission examples that should be present in seeded data if available:

- `PRODUCT_READ`
- `PRODUCT_CREATE`
- `PRODUCT_UPDATE`
- `PRODUCT_DELETE`
- `ORDER_READ_ALL`
- `ORDER_UPDATE_STATUS`
- `ORDER_READ_OWN`
- `ORDER_CANCEL_OWN`
- `SHIPMENT_CREATE`
- `SHIPMENT_READ`
- `INVENTORY_MANAGE`

### Relationships

- `roles`
- `permissions`
- `role_permissions`
- `user_roles`

### Test and demo flow supported

- admin can create shipment
- staff can manage operations
- customer can only read or cancel own orders

## Categories

### Purpose

Support storefront navigation, filtering, and demo of technology catalog structure.

### Recommended sample data

Suggested category tree:

- Laptops
- PC Components
  - CPU
  - GPU
  - RAM
  - SSD
- Monitors
- Keyboards
- Mice
- Accessories

Suggested category properties:

- meaningful slug
- mix of parent and child categories
- all main demo categories active
- 1 inactive or soft-deleted category for admin-only behavior testing if environment allows

### Relationships

- parent-child in category hierarchy
- referenced by products
- used in public product filtering

### Test and demo flow supported

- browse category pages
- filter products by category
- show nested category structure

## Brands

### Purpose

Support filtering, product identity, and demo credibility for tech retail.

### Recommended sample data

Suggested brands:

- Intel
- AMD
- NVIDIA
- ASUS
- MSI
- Lenovo
- Dell
- LG
- Logitech
- Keychron

Suggested brand fields:

- name
- slug
- logo URL if available
- description
- active true for most brands

### Relationships

- referenced by products
- used in public and admin product filtering

### Test and demo flow supported

- filter by brand
- brand detail browsing
- admin brand management review

## Products

### Purpose

Provide enough catalog depth for browsing, filtering, variant selection, cart, checkout, wishlist, and review flows.

### Recommended sample data

Suggested product set:

- Laptop ASUS ROG Strix G16
- Laptop Lenovo Legion 5
- CPU Intel Core i5-14600K
- CPU AMD Ryzen 7 7800X3D
- GPU MSI GeForce RTX 4060 Ventus 2X
- GPU ASUS TUF RTX 4070 Super
- RAM Corsair Vengeance DDR5 32GB
- RAM Kingston Fury Beast DDR5 16GB
- SSD Samsung 990 EVO 1TB
- SSD WD Black SN850X 2TB
- Monitor LG UltraGear 27 inch 144Hz
- Monitor Dell 24 inch IPS
- Keyboard Keychron K8 Pro
- Keyboard Logitech G Pro Keyboard
- Mouse Logitech G304
- Mouse Razer DeathAdder Essential
- Laptop cooling pad or USB hub in accessories
- Backpack or sleeve in accessories

Recommended product data attributes:

- category
- optional brand
- name
- slug
- description
- short description
- thumbnail
- specs JSON suitable for tech products
- active flag
- manual badge for a few products such as `new`, `hot`, or `sale`

Examples of product specs:

- laptop: CPU, GPU, RAM, SSD, display, battery, weight
- CPU: socket, core count, thread count, base clock
- GPU: VRAM, boost clock, power connector
- monitor: size, refresh rate, panel type, resolution
- keyboard: switch type, layout, connection type
- mouse: DPI, weight, connection type

### Relationships

- belongs to category
- optional brand
- owns product images
- owns variants
- appears in wishlist
- appears in reviews through order items and product association

### Test and demo flow supported

- product list
- product detail
- search
- facets
- related products
- admin CRUD review

## Product variants

### Purpose

Support purchasable SKUs, pricing differences, stock management, cart, and order item snapshots.

### Recommended sample data

Every configurable product should have 1 to 3 variants.

Suggested variant examples:

- ASUS ROG Strix G16
  - i7 / RTX 4060 / 16GB / 512GB
  - i7 / RTX 4070 / 16GB / 1TB
- Lenovo Legion 5
  - Ryzen 7 / RTX 4060 / 16GB / 512GB
  - Ryzen 7 / RTX 4070 / 32GB / 1TB
- LG UltraGear monitor
  - 27 inch / 144Hz
  - 27 inch / 180Hz
- Keychron keyboard
  - Brown switch
  - Red switch
- Logitech mouse
  - Black
  - White

Required variant fields:

- SKU
- display name
- price
- attributes JSON
- active flag
- optional image reference

### Relationships

- belongs to product
- referenced by cart items
- referenced by order items
- referenced by inventory

### Test and demo flow supported

- variant selection
- price filtering
- stock validation
- order item snapshot

## Inventory

### Purpose

Support stock checks in cart and checkout, low-stock demo, and inventory management screens.

### Recommended sample data

Every variant should have inventory.

Suggested stock distribution:

- high stock variants: 20 to 50 units
- medium stock variants: 5 to 10 units
- low stock variants: 1 to 3 units
- one out-of-stock variant: 0 units

Suggested threshold examples:

- standard threshold: 5
- premium laptop threshold: 2
- accessories threshold: 10

### Relationships

- one inventory row per variant
- checked during add-to-cart
- checked during checkout
- affected when order is placed

### Test and demo flow supported

- low-stock listing
- insufficient stock rejection
- checkout stock deduction
- admin stock adjustment

## Cart

### Purpose

Support demo and QA of authenticated shopping cart flows.

### Recommended sample data

Suggested carts:

- `customer1`: active cart with 2 items
- `customer2`: active cart with 1 low-stock item
- `customer3`: empty cart

Suggested cart content examples:

- customer1
  - 1 laptop variant
  - 1 mouse variant
- customer2
  - 2 units of a monitor or GPU variant near stock limit

### Relationships

- one cart per user
- cart items reference variants
- cart summary derives from variant price and quantity

### Test and demo flow supported

- get cart
- add item
- update quantity
- remove item
- clear cart
- stock enforcement

## Orders

### Purpose

Support checkout demo, customer order history, admin order management, payment and shipment integration, cancellation, and expiration flows.

### Recommended sample data

Suggested order status mix:

- 2 `pending`
- 2 `confirmed`
- 2 `processing`
- 2 `shipped`
- 2 `delivered`
- 1 `cancelled`
- 1 `expired`

Suggested payment methods mix:

- `cod`
- `sepay`
- 1 example with `vnpay` or `momo` only if specifically needed to demonstrate current validation gap, and clearly label it as unsupported operationally

Suggested order content examples:

- laptop + mouse bundle
- CPU + RAM + SSD build
- monitor + keyboard

Suggested total behavior should match current source:

- `discountAmount = 0` unless specifically stored for legacy or demonstration note
- `shippingFee = 0` in line with current checkout implementation

### Relationships

- belongs to user
- contains shipping address snapshot
- contains order items referencing variants by snapshot
- links to payment transactions
- links to shipment

### Test and demo flow supported

- place order
- list own orders
- get order detail
- cancel own order
- admin update status
- expire pending orders

## Payments

### Purpose

Support SePay webhook demo, payment transaction review, mismatch scenarios, and provider-gap analysis.

### Recommended sample data

Suggested payment transaction set:

- 1 successful SePay payment for a `pending` order that becomes `confirmed`
- 1 payment mismatch record where amount differs from order total
- 1 duplicate provider reference scenario for webhook idempotency testing
- 1 ignored payment for unknown order code if QA needs negative case data

Suggested fields:

- provider `sepay`
- amount
- status `paid` or `mismatch`
- provider reference
- raw provider payload snapshot
- paid timestamp where relevant

### Relationships

- belongs to order
- tied to payment provider behavior
- affects order status transition

### Test and demo flow supported

- SePay webhook success
- amount mismatch
- duplicate transaction detection
- ignored unknown order

## Shipments

### Purpose

Support admin shipment creation, shipment tracking demo, webhook handling, and order status sync.

### Recommended sample data

Suggested shipment set:

- 1 GHN shipment in `delivering`
- 1 GHN shipment in `delivered`
- 1 GHTK shipment in `ready_to_pick`
- 1 GHTK shipment in `delivered`

Suggested shipment fields:

- provider `ghn` or `ghtk`
- tracking code
- fee
- estimated delivery time
- status
- shippedAt or deliveredAt where applicable

Shipment log examples:

- created by admin
- webhook moved to delivering
- webhook moved to delivered

### Relationships

- one shipment per order
- shipment logs belong to shipment
- shipment status may update order status

### Test and demo flow supported

- create shipment
- get order shipment
- GHN webhook update
- GHTK webhook update
- order sync to `processing` or `delivered`

## Promotions

### Purpose

Support admin visibility and future testing, while also documenting the current gap that promotion is not truly applied during checkout.

### Recommended sample data

Suggested promotions:

- `LAPTOP5`: 5 percent off laptops above a minimum amount
- `SSD200K`: fixed discount on SSD category
- `FREESHIP10`: example freeship-style promotion for future use

Suggested promotion fields:

- code
- type
- discount value
- minimum order amount
- usage limit
- date window
- status active

### Relationships

- promotions
- promotion targets
- promotion usage
- referenced by order `promotionCode`

### Test and demo flow supported

- admin promotion listing and validation review if available
- demonstration of current gap:
  - promotion data may exist
  - checkout currently stores `promotionCode` but does not apply real discount

## Reviews

### Purpose

Support product social proof demo and moderation-related QA.

### Recommended sample data

Suggested review set:

- 2 approved reviews for a laptop
- 1 approved review for a monitor
- 1 pending review
- 1 rejected review if moderation flow needs example data

Suggested content:

- realistic star ratings 4 to 5 for demo
- 1 average rating review 3 stars
- optional image URLs for 1 or 2 reviews

### Relationships

- belongs to product
- belongs to user
- tied to order for purchase proof

### Test and demo flow supported

- public review display
- pending review moderation
- review stats on product pages

## Wishlist

### Purpose

Support customer save-for-later behavior and simple engagement demo.

### Recommended sample data

Suggested wishlist examples:

- customer1 wishlist
  - 1 GPU
  - 1 laptop
- customer2 wishlist
  - 1 keyboard

### Relationships

- belongs to user
- references product

### Test and demo flow supported

- get wishlist
- toggle wishlist
- check product appeal across categories

## Addresses

### Purpose

Support saved-address checkout, shipping provider integration, and address validation edge cases.

### Recommended sample data

Suggested address set:

- customer1
  - 1 default address in Ho Chi Minh City
  - 1 secondary address in Thu Duc
- customer2
  - 1 default address in Ha Noi
- customer3
  - 1 incomplete or edge-case address only if QA wants negative testing in a safe environment

Recommended address fields:

- receiver
- phone
- province or city
- province code
- district
- district code
- ward
- ward code
- street
- default flag

### Relationships

- belongs to user
- used directly by address module
- used indirectly by order checkout
- used by shipping provider mapping and shipment creation

### Test and demo flow supported

- get addresses
- create and update address
- set default address
- checkout with saved address
- shipping integration validation

## Minimal Demo Mock Data Table

This is the minimum recommended business data set for a demo that covers browse, cart, checkout, payment, and shipping.

| Group | Minimal mock data |
|------|--------------------|
| `category` | Laptops, CPU, GPU, RAM, SSD, Monitors, Keyboards, Mice, Accessories |
| `brand` | Intel, AMD, NVIDIA, ASUS, MSI, Lenovo, Dell, LG, Logitech, Keychron |
| `product` | At least 10 products: 2 laptops, 2 CPUs, 2 GPUs, 1 RAM, 1 SSD, 1 monitor, 1 keyboard, 1 mouse |
| `variant` | At least 15 variants with real differences in CPU, GPU, RAM, SSD, switch, color, refresh rate |
| `stock` | At least 1 high-stock variant, 1 low-stock variant, 1 out-of-stock variant |
| `customer` | 2 active customers, 1 inactive customer, 1 admin, 1 staff |
| `order` | At least 6 orders in mixed states: pending, confirmed, processing, shipped, delivered, cancelled or expired |
| `payment` | At least 2 SePay transactions: 1 paid success, 1 mismatch |
| `shipment` | At least 2 shipments: 1 GHN and 1 GHTK, with one in-progress and one delivered |

## Suggested concrete demo examples

### Category examples

- `laptops`
- `cpu`
- `gpu`
- `ram`
- `ssd`
- `monitors`
- `keyboards`
- `mice`
- `accessories`

### Brand examples

- `asus`
- `lenovo`
- `intel`
- `amd`
- `nvidia`
- `msi`
- `lg`
- `logitech`
- `keychron`

### Product and variant examples

- ASUS ROG Strix G16
  - `ROG-G16-I7-4060-16-512`
  - `ROG-G16-I7-4070-16-1TB`
- Lenovo Legion 5
  - `LEGION5-R7-4060-16-512`
  - `LEGION5-R7-4070-32-1TB`
- Intel Core i5-14600K
  - `I5-14600K-TRAY`
- AMD Ryzen 7 7800X3D
  - `R7-7800X3D-BOX`
- MSI RTX 4060 Ventus 2X
  - `RTX4060-VENTUS-8G`
- ASUS TUF RTX 4070 Super
  - `RTX4070S-TUF-12G`
- Samsung 990 EVO 1TB
  - `990EVO-1TB`
- LG UltraGear 27 inch 144Hz
  - `LG-27UG-144`
- Keychron K8 Pro
  - `K8PRO-BROWN`
  - `K8PRO-RED`
- Logitech G304
  - `G304-BLACK`
  - `G304-WHITE`

### Customer examples

- Admin NitroTech
- Staff Operations NitroTech
- Nguyen Anh Tuan
- Tran Minh Chau
- Le Hoang An

### Order examples

- Order A: customer1, `sepay`, laptop + mouse, `pending`
- Order B: customer1, `cod`, monitor + keyboard, `confirmed`
- Order C: customer2, `sepay`, CPU + RAM + SSD, `processing`
- Order D: customer2, `cod`, laptop, `shipped`
- Order E: customer1, `sepay`, GPU, `delivered`
- Order F: customer3, `cod`, accessory, `cancelled`

### Payment examples

- SePay paid transaction for Order A
- SePay mismatch transaction for an order where webhook amount is lower than `finalAmount`

### Shipment examples

- GHN shipment for Order C with status `delivering`
- GHTK shipment for Order E with status `delivered`

## Risk notes

These risks should be covered when preparing or reviewing mock data.

### Data missing stock

If a variant has no inventory row or quantity too low:

- cart add may fail
- checkout may fail
- demo order flow may stop unexpectedly

Mitigation:

- every demo variant should have inventory
- explicitly label low-stock and out-of-stock test cases

### Data missing variants

If a product has no usable active variant:

- product page may look valid but not be purchasable
- cart and checkout flow cannot demonstrate actual buying

Mitigation:

- every key demo product should have at least one active variant
- configurable products should have at least two variants

### Wrong address data

If address codes do not match shipping provider expectations:

- checkout with saved address may succeed
- shipment creation may fail later due to carrier mapping

Mitigation:

- ensure at least 2 addresses have realistic city, district, ward, and code combinations
- keep one deliberately problematic address only for negative testing

### Payment mismatch

If mock payment webhook amount does not match `order.finalAmount`:

- transaction may be stored as mismatch
- order will not move to `confirmed`

Mitigation:

- prepare both happy-path and mismatch examples intentionally
- label them clearly in test notes

### Shipping webhook not matching tracking code

If webhook payload uses a tracking code not existing in shipment table:

- backend returns shipment not found
- order status will not update

Mitigation:

- prepare one valid webhook scenario and one invalid tracking scenario

## Suggested rollout order for mock data preparation

1. roles and users
2. categories and brands
3. products and variants
4. inventory
5. addresses
6. carts
7. orders
8. payments
9. shipments
10. wishlist and reviews
11. promotions

## BA and QA conclusion

The mock data plan should prioritize not just catalog richness, but flow completeness.

For NitroTech, the most important demo and QA dataset is one that can reliably support:

- browse to cart
- cart to checkout
- checkout to payment
- payment to confirmation
- confirmation to shipment
- shipment to delivered order

At the same time, the environment should preserve a few controlled negative cases:

- low stock
- payment mismatch
- invalid tracking code
- inactive user
- unsupported payment method gap awareness

This document is a planning artifact only and does not create seed scripts or modify code.
