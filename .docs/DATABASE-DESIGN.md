# Database design

Nitrotech API uses PostgreSQL with Flyway migrations. This document explains the current schema shape, the main relationships, and the conventions that matter when changing persistence code.

The migration source of truth is `src/main/resources/db/migration`. Keep this document in sync when adding, removing, or reshaping tables.

## Design principles

- Use `BIGSERIAL` primary keys for entity tables
- Store timestamps as `TIMESTAMPTZ` after the timestamp normalization migrations
- Use `deleted_at` for soft-deleted aggregate roots
- Use `active` for boolean enablement flags
- Store flexible product, address, payment, audit, and review snapshots in `JSONB`
- Keep request validation in application DTOs, not database entities
- Use partial unique indexes where soft-deleted records should not reserve unique values
- Prefer explicit foreign keys for ownership and lifecycle rules

## Soft delete

Soft-deleted records keep their row and set `deleted_at`.

| Table | Soft delete |
|------|-------------|
| `users` | Yes |
| `roles` | Yes |
| `permissions` | Yes |
| `brands` | Yes |
| `categories` | Yes |
| `products` | Yes |
| `product_variants` | Yes |
| `orders` | Yes |
| `reviews` | Yes |

Queries for active records should filter `deleted_at IS NULL`. Repository methods should make that scope visible with names such as `findActiveById`, `findDeletedById`, and `existsActiveBySlug`.

## Timestamp columns

Most tables started with `TIMESTAMP` columns and later migrated to `TIMESTAMPTZ`. Migrations `V31` through `V36` normalize order, payment, token, cart, wishlist, inventory, catalog, user, review, promotion, banner, role, permission, and shipping timestamps.

New timestamp columns should use `TIMESTAMPTZ` unless there is a specific reason to store local time.

## Identity and access

### `users`

Stores customer, staff, and admin accounts.

Important columns:

- `email`: unique login identifier
- `status`: account state such as `inactive`, `active`, `banned`, or `suspended`
- `provider` and `provider_id`: local or external auth provider metadata
- `deleted_at`: soft-delete marker

Relationships:

- One user has many `addresses`
- One user has one `cart`
- One user has many `orders`
- One user can have many `roles` through `user_roles`
- One user can have many `wishlists`, `reviews`, and `promotion_usages`

### `user_tokens`

Stores one-time account tokens such as email verification and password reset tokens.

Important columns:

- `token`: unique token value
- `type`: token purpose
- `expires_at`: token expiry
- `used`: prevents token reuse

Index:

- `idx_token_lookup` on `(token, type)` where `used = false`

### `roles`, `permissions`, `role_permissions`, and `user_roles`

These tables implement role-based access control (RBAC).

Important columns:

- `roles.slug`: stable role identifier such as `admin`, `staff`, or `customer`
- `roles.system_role`: protects system roles from unsafe edits
- `permissions.slug`: authority string such as `PRODUCT_READ` or `ORDER_UPDATE_STATUS`
- `permissions.group_name`: permission grouping for dashboard display
- `permissions.system_permission`: protects built-in permissions

Join tables:

- `role_permissions`: many-to-many role to permission mapping
- `user_roles`: many-to-many user to role mapping

Migration notes:

- `V38` adds system role and permission flags, seeds current authorities, and assigns defaults
- `V39` normalizes legacy permission slugs to the current `SCREAMING_SNAKE_CASE` format
- `V40` adds media management permission
- `V43` adds audit log read permission

## Customer profile and addresses

### `addresses`

Stores user shipping addresses and province/district/ward codes.

Important columns:

- `user_id`: owner
- `receiver` and `phone`: recipient contact
- `province_code`, `district_code`, `ward_code`: address codes used by shipping integrations
- `default_address`: marks the default address

Migration note:

- `V29` allows `district` and `district_code` to be nullable for address data that does not include district-level values.

## Catalog

### `brands`

Stores product brands.

Important columns:

- `slug`: public identifier
- `logo`: brand logo URL
- `active`: visibility flag
- `deleted_at`: soft-delete marker

Indexes:

- `idx_brands_slug_active`: unique `slug` for active rows only
- `idx_brands_slug`: slug lookup for active rows

### `categories`

Stores hierarchical product categories.

Important columns:

- `parent_id`: self-reference for category trees
- `sort_order`: order among siblings
- `active`: visibility flag
- `deleted_at`: soft-delete marker

Indexes:

- `idx_categories_slug_active`: unique `slug` for active rows only
- `idx_categories_slug`: slug lookup for active rows
- `idx_categories_parent_sort`: `(parent_id, sort_order)` for active rows

### `products`

Stores product-level catalog information.

Important columns:

- `category_id`: required category
- `brand_id`: optional brand
- `slug`: public product identifier
- `description`: rich product description
- `short_description`: summary text derived from description when migrated
- `thumbnail`: default product image
- `specs`: `JSONB` product specifications
- `manual_badge` and `manual_badge_expires_at`: admin-controlled merchandising badge
- `active` and `deleted_at`: visibility and soft-delete state

Indexes:

- `idx_products_name_lower`: case-insensitive name search
- `idx_products_active_deleted`: active product filtering
- `idx_products_search`: combined filter index for active, deleted, category, and brand

### `product_images`

Stores product gallery images.

Important columns:

- `product_id`: owning product
- `url`: image URL
- `sort_order`: gallery order

Lifecycle:

- Deleted products cascade to product images through `ON DELETE CASCADE`

### `product_variants`

Stores purchasable stock keeping units (SKUs).

Important columns:

- `product_id`: owning product
- `sku`: unique variant identifier
- `name`: variant display name
- `price`: current variant price
- `attributes`: `JSONB` variant attributes such as CPU, RAM, storage, or color
- `image_id`: optional main image from `product_images`
- `active` and `deleted_at`: visibility and soft-delete state

Indexes:

- `idx_variant_attributes`: GIN index for `attributes`
- `idx_variant_image`: lookup by variant image
- `idx_product_variants_product_price`: active price aggregation by product

## Cart, orders, and inventory

### `carts` and `cart_items`

Each user has one cart. Cart items reference product variants.

Important constraints:

- `carts.user_id` is unique
- `cart_items` has a unique `(cart_id, variant_id)` pair
- Deleting a cart cascades to its items
- Deleting a variant cascades to cart items

### `orders` and `order_items`

Orders store checkout snapshots and order totals.

Important `orders` columns:

- `user_id`: customer
- `shipping_address`: `JSONB` address snapshot at checkout
- `status`: order lifecycle state
- `payment_method`: selected payment method
- `total_amount`, `discount_amount`, `shipping_fee`, `final_amount`: monetary totals
- `promotion_code`: code applied at checkout
- `deleted_at`: soft-delete marker

Important `order_items` columns:

- `order_id`: owning order
- `variant_id`: purchased variant
- `name` and `sku`: item snapshot
- `quantity`, `unit_price`, and `subtotal`: order-time pricing

Indexes:

- `idx_orders_status`
- `idx_orders_user_id`

### `inventories`

Stores stock quantity per variant.

Important constraints:

- `variant_id` is unique
- Deleting a variant cascades to inventory

Important columns:

- `quantity`: available stock
- `low_stock_threshold`: threshold for low-stock warnings

## Payment

### `payment_transactions`

Stores payment attempts and provider responses.

Important columns:

- `order_id`: owning order
- `provider`: payment provider such as `cod` or `sepay`
- `amount`: payment amount
- `status`: transaction state
- `provider_ref`: provider transaction reference
- `provider_data`: `JSONB` raw provider payload
- `paid_at`: payment completion time

Index:

- `uq_payment_transactions_provider_ref`: unique `(provider, provider_ref)` where `provider_ref IS NOT NULL`

## Shipping

### `shipments`

Stores one shipment per order.

Important columns:

- `order_id`: unique order reference
- `provider`: carrier such as `ghn`, `ghtk`, or internal fulfillment
- `tracking_code`: carrier tracking code
- `status`: shipment lifecycle state
- `fee`: shipping fee
- `estimated_at`, `shipped_at`, `delivered_at`: shipping timeline

### `shipment_logs`

Stores shipment status history.

Important columns:

- `shipment_id`: owning shipment
- `status`: normalized status
- `raw_status`: carrier-provided status from webhook or API
- `source`: source of the event, defaults to `SYSTEM`
- `location` and `note`: status context

Lifecycle:

- Deleting a shipment cascades to shipment logs

### `carrier_address_mappings`

Maps local address codes to carrier-specific address identifiers.

Important columns:

- `provider`: carrier name
- `province_code`, `district_code`, `ward_code`: local address identifiers
- `carrier_province_id`, `carrier_district_id`, `carrier_ward_code`: carrier identifiers
- `confidence`: mapping quality, defaults to `verified`
- `active`: whether the mapping can be used

Constraints and indexes:

- Unique `(provider, province_code, district_code, ward_code)`
- `idx_carrier_address_mappings_provider_active`

## Promotions and marketing

### `promotions`

Stores discount and campaign rules.

Important columns:

- `code`: voucher code, nullable for automatic promotions
- `type`: discount type such as percentage, fixed amount, or freeship
- `discount_value`, `min_order_amount`, `max_discount_amount`: discount calculation fields
- `stackable`: whether it can combine with other promotions
- `priority`: conflict resolution ordering
- `usage_limit` and `usage_per_user`: usage caps
- `start_at`, `end_at`, `status`: availability window and state

### `promotion_targets`

Stores product, category, or brand targeting rules for promotions.

Important columns:

- `promotion_id`: owning promotion
- `target_type`: target kind
- `target_id`: target identifier
- `excluded`: whether the target is excluded instead of included

Index:

- `idx_pt_lookup` on `(promotion_id, target_type, target_id)`

### `promotion_usages`

Stores each applied promotion.

Important columns:

- `promotion_id`: promotion used
- `user_id`: user who used it
- `order_id`: order where it was applied
- `used_code`: code used at checkout
- `discount_amount`: discount applied

Index:

- `idx_pu_lookup` on `(promotion_id, user_id)`

### `banners`

Stores homepage and promotional banners.

Important columns:

- `title`, `image`, `url`: banner content
- `position`: placement key such as hero or sidebar
- `active`: visibility flag
- `start_date`, `end_date`: display window
- `sort_order`: display order

## Reviews and wishlist

### `reviews`

Stores product reviews tied to completed purchases.

Important columns:

- `product_id`, `user_id`, `order_id`: review ownership and purchase proof
- `rating`: value from 1 to 5
- `images`: `JSONB` review image URLs
- `status`: moderation state
- `deleted_at`: soft-delete marker

Constraint:

- Unique `(product_id, user_id, order_id)` to keep one review per product per order

### `wishlists`

Stores saved products per user.

Important constraints:

- Primary key `(user_id, product_id)`
- Deleting the user or product cascades to wishlist rows

## Audit

### `audit_logs`

Stores administrative and system audit events.

Important columns:

- `correlation_id`: request or operation correlation key
- `actor_type`, `actor_id`, `actor_email`, `actor_roles`: actor snapshot
- `action`: operation name
- `resource_type`, `resource_id`: affected resource
- `outcome`: result such as success or failure
- `before_data`, `after_data`, `metadata`: `JSONB` event context
- `ip_address`, `user_agent`: request metadata
- `created_at`: event time

Indexes:

- `idx_audit_logs_resource`
- `idx_audit_logs_actor`
- `idx_audit_logs_correlation`
- `idx_audit_logs_created_at`

## Migration history highlights

| Migration range | Purpose |
|-----------------|---------|
| `V1` to `V14` | Initial identity, access, catalog, cart, order, payment, shipping, promotion, review, banner, inventory, and wishlist schema |
| `V15` to `V18` | Seed refactor and token table cleanup |
| `V19` to `V28` | Catalog slug, category order, variant image, manual badge, search index, and short description changes |
| `V29` to `V30` | Nullable district support and payment provider reference uniqueness |
| `V31` to `V36` | Timestamp normalization to `TIMESTAMPTZ` |
| `V37` | Carrier address mapping |
| `V38` to `V40` | RBAC authority normalization and media permission |
| `V41` | Shipment log source and raw status metadata |
| `V42` | Audit log table and indexes |
| `V43` | Audit log read permission |

## Maintenance checklist

When changing the schema:

- Add a Flyway migration under `src/main/resources/db/migration`
- Update entity mappings and repository queries
- Update this document for table, relationship, index, and lifecycle changes
- Keep `deleted_at IS NULL` filters in active-record queries
- Add or update tests for repository behavior when the change affects business rules
