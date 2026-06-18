---
meta:
  contentType: Reference
---

# Define order API behavior

This page defines the order API contract for customer and admin surfaces. Use it to compare backend endpoints, frontend API clients, and dashboard requirements before changing order code.

## Current endpoints

Customer order endpoints:

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`
- `PATCH /api/orders/{id}/cancel`

Admin order endpoints:

- `GET /api/admin/orders`
- `GET /api/admin/orders/facets`
- `GET /api/admin/orders/{id}`
- `PATCH /api/admin/orders/{id}/status`
- `GET /api/admin/orders/{id}/shipment`
- `POST /api/admin/orders/{id}/shipment`

## Admin order list contract

The admin order list supports operational work: search, filter, sort, and open order details.

Target request:

```text
GET /api/admin/orders?search=&status=&paymentMethod=&createdFrom=&createdTo=&amountMin=&amountMax=&page=0&size=20&sort=createdAt,desc
```

Use repeatable `sort` parameters for multiple sort keys:

```text
GET /api/admin/orders?page=0&size=20&sort=status,asc&sort=createdAt,desc
```

Supported parameters:

- **`search`**: searches order id, receiver name, receiver phone, and user id where supported
- **`status`**: filters by order status
- **`paymentMethod`**: filters by payment method
- **`createdFrom`**: includes orders created at or after this ISO-8601 timestamp
- **`createdTo`**: excludes orders created at or after this ISO-8601 timestamp
- **`amountMin`**: includes orders with `finalAmount` greater than or equal to this VND amount
- **`amountMax`**: includes orders with `finalAmount` less than or equal to this VND amount
- **`page`**: zero-based page index
- **`size`**: page size
- **`sort`**: repeatable sort field and direction

Allowed sort fields:

- `createdAt`
- `updatedAt`
- `finalAmount`
- `status`

## Admin order list response

The admin order list should return a list item DTO, not full order detail data. This avoids loading order items for each row.

Target list item:

```text
OrderListItemData(
  id,
  userId,
  receiver,
  phone,
  status,
  paymentMethod,
  finalAmount,
  itemCount,
  createdAt,
  updatedAt
)
```

Target response:

```java
return ResponseEntity.ok(ApiResult.paged(page));
```

## Admin order facets

Admin facets contain backend-owned filter values and live counts. The frontend should fetch them separately from the list.

Target request:

```text
GET /api/admin/orders/facets?search=&status=&paymentMethod=&createdFrom=&createdTo=&amountMin=&amountMax=
```

Target response:

```json
{
  "data": {
    "total": 18,
    "statuses": [
      { "value": "pending", "label": "Chờ xác nhận", "count": 12 },
      { "value": "confirmed", "label": "Đã xác nhận", "count": 4 },
      { "value": "processing", "label": "Đang xử lý", "count": 0 },
      { "value": "shipped", "label": "Đang giao", "count": 8 },
      { "value": "delivered", "label": "Hoàn thành", "count": 140 },
      { "value": "cancelled", "label": "Đã hủy", "count": 2 },
      { "value": "refunded", "label": "Đã hoàn tiền", "count": 1 },
      { "value": "expired", "label": "Đã hết hạn", "count": 0 }
    ],
    "paymentMethods": [
      { "value": "cod", "label": "COD", "count": 20 },
      { "value": "vnpay", "label": "VNPay", "count": 6 },
      { "value": "momo", "label": "MoMo", "count": 0 },
      { "value": "sepay", "label": "SePay", "count": 7 }
    ]
  }
}
```

Do not derive valid values from currently existing orders. Empty datasets must still return all valid filter values with `count: 0`.

Count facets isolate the selected facet from its own filter:

- **`statuses`**: respect `search`, `paymentMethod`, `createdFrom`, `createdTo`, `amountMin`, and `amountMax`; ignore selected `status`
- **`paymentMethods`**: respect `search`, `status`, `createdFrom`, `createdTo`, `amountMin`, and `amountMax`; ignore selected `paymentMethod`

Facet counts must not change when only `page`, `size`, or `sort` changes.

Use `data.total` for the status "All" tab. It must respect `search`, `paymentMethod`, `createdFrom`, `createdTo`, `amountMin`, and `amountMax`, but ignore selected `status`. Do not use list `meta.totalElements` for the "All" tab when a status filter is selected, because list metadata describes only the current filtered rows.

Example behavior:

```text
GET /api/admin/orders/facets?search=nguyen&paymentMethod=cod
```

The response counts orders matching `search=nguyen` and `paymentMethod=cod`, grouped by status.

```text
GET /api/admin/orders/facets?search=nguyen&paymentMethod=cod&status=pending
```

The `statuses` counts stay the same as the previous request because status facets ignore the selected `status`. The `paymentMethods` counts may change because they respect `status=pending`.

Do not add facets for fields that the order table does not store. For example, do not add `paymentStatus` until the order model has a payment status field.

## Created time range behavior

Order time filters use a half-open range to avoid end-of-day precision bugs. Frontend code converts local date selections into ISO-8601 timestamps before calling the backend.

Use this query shape:

```sql
created_at >= :createdFrom AND created_at < :createdToExclusive
```

Interpret timestamp inputs this way:

- **`createdFrom`**: exact inclusive timestamp
- **`createdTo`**: exact exclusive timestamp

Example:

```text
createdFrom=2026-06-14T17:00:00Z&createdTo=2026-06-17T17:00:00Z
```

The backend should query:

```text
createdAt >= 2026-06-14T17:00:00Z
createdAt < 2026-06-17T17:00:00Z
```

For a Vietnam-local date range of `2026-06-15` through `2026-06-17`, frontend sends the UTC timestamps above.

## Amount range behavior

Order amount filters use VND values and compare against `finalAmount`.

Use this query shape:

```sql
final_amount >= :amountMin AND final_amount <= :amountMax
```

Interpret amount inputs this way:

- **`amountMin`**: inclusive minimum final amount in VND
- **`amountMax`**: inclusive maximum final amount in VND

Example:

```text
amountMin=5000000&amountMax=75000000
```

The backend should query:

```text
finalAmount >= 5000000
finalAmount <= 75000000
```

Frontend controls can display friendlier units, such as million VND, but URL query state and API calls should still use the backend parameter names and VND values.

Debounce high-frequency amount controls before updating URL query state or calling the API. Dragging a range slider should not send a request for every pointer movement.

## Admin order search behavior

The `search` parameter on `GET /api/admin/orders` matches multiple fields with an `OR` condition.

Search should include these fields:

- `orders.id`
- `orders.shipping_address.receiver`
- `orders.shipping_address.phone`
- `users.email`

Search should include `users.email` after the order query joins user data. Keep status values lowercase and do not add search fields that the order model does not store.

Debounce text search before updating URL query state or calling the API. The dashboard URL should use `search`, not `q`, so the browser URL matches the backend request DTO.

## Customer order list contract

Customer order list can stay smaller than admin order list.

Target request:

```text
GET /api/orders?status=&page=0&size=10
```

Customer list does not need facets unless the account page adds status counts or richer filters.

## Customer order list response

The customer order list should return `OrderListItemData`, not full `OrderData`.

Full `OrderData` is reserved for `GET /api/orders/{id}`. This keeps list payloads small and avoids loading order items for every row.
