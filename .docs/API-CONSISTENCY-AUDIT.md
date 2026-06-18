---
meta:
  contentType: Reference
---

# Audit API consistency

Use this checklist to verify API consistency before and after backend changes. Each check states the rule, the signal to inspect, and the command or file pattern that helps verify it.

## Layer boundaries

Domain code must not import infrastructure classes.

Check imports with:

```powershell
rg -n "import com\.nitrotech\.api\.infrastructure" src/main/java/com/nitrotech/api/domain
```

Expected result: no matches.

Run the boundary test after domain-layer changes:

```powershell
.\gradlew.bat test --tests com.nitrotech.api.architecture.DomainBoundaryTest
```

## Controller boundaries

Controllers should map requests, call use cases, and wrap responses. Controllers should not call repositories directly.

Check controller dependencies with:

```powershell
rg -n "Repository" src/main/java/com/nitrotech/api/application
```

Allowed result: no repository fields in controllers unless a documented exception exists.

## Response wrappers

Controllers should return `ResponseEntity<ApiResult<T>>`. Use cases should not return `ApiResult`.

Check use cases with:

```powershell
rg -n "ApiResult" src/main/java/com/nitrotech/api/domain
```

Expected result: no matches in use case return types.

## Paginated lists

Admin table endpoints should follow the module list pattern in `.docs/api/CONVENTIONS.md`.

Check for these signals:

- `Pageable` parameter
- `@PageableDefault`
- `@ValidSortFields`
- `ApiResult.paged(page)` for list data
- `ApiResult.ok(facets)` for filter metadata
- Repeatable Spring `sort` instead of custom `sortBy` and `sortDir`

Legacy endpoints can keep older shapes until touched, but new admin table endpoints should use the module list pattern.

## Query parameter names

Use shared parameter names when the meaning matches:

- `page`
- `size`
- `sort`
- `search`
- `createdFrom`
- `createdTo`
- `status`

Flag aliases such as `limit`, `q`, `from`, `to`, `sortBy`, and `sortDir` unless the endpoint documents an exception.

## Soft delete scope

Repository method names must state whether they read active, deleted, or all records.

Use names that expose scope:

- `findActive*`
- `findDeleted*`
- `findNotDeleted*`
- `existsActive*`
- `existsNotDeleted*`
- `countActive*`

Custom queries for active records must filter `deletedAt IS NULL`.

## Slug reuse

Brand, product, and category slugs can be reused after soft delete. Database uniqueness should apply only where `deleted_at IS NULL`.

Restore use cases must check conflicts with non-deleted records before clearing `deleted_at`.

## External provider transactions

External provider calls must run outside database transactions.

Use this flow:

1. Validate local state before the external call
2. Call the provider outside a transaction
3. Save local state in a small transactional method

Run focused tests after payment or shipping consistency changes:

```powershell
.\gradlew.bat test --tests com.nitrotech.api.domain.payment.usecase.HandlePaymentWebhookUseCaseTest
.\gradlew.bat test --tests com.nitrotech.api.domain.shipping.usecase.* --tests com.nitrotech.api.infrastructure.shipping.*
```

## Request DTO style

Use request `record` types for JSON bodies unless mutable binding is required. Use mutable request classes for `@ModelAttribute` query binding.

Limit mutable request DTOs to getter and setter binding.

## Exception shape

Business failures should use structured domain exceptions with stable error codes. Do not throw generic `RuntimeException` for expected domain failures.

## Completed consistency fixes

These fixes already landed and should stay true:

- Domain code no longer imports infrastructure classes
- Payment and shipping use domain provider ports
- Shipment provider calls run outside transactional persistence
- Payment transaction persistence uses a repository port
- Brand, product, and category slug checks state non-deleted scope
- Deleted slugs can be reused through partial unique indexes
- Restore and hard-delete checks align with single-item operations
- Storage failures use structured storage errors
- Domain boundary has an automated guard test
