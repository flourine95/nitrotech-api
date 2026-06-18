---
meta:
  contentType: Reference
---

# Follow API conventions

This page defines API conventions for NitroTech backend and frontend work. Read this page before adding list endpoints, query parameters, response wrappers, or controller and use case boundaries.

## Use this page

Use this page as the source of truth for current API shape. When a module needs a different shape, document that exception in the module API page and add a decision record if the exception changes a shared convention.

## Keep controllers thin

Controllers map requests, call use cases, and wrap responses with `ApiResult`.

- **Controller**: accepts request DTOs, validates inputs, maps to commands or filters, and returns `ResponseEntity<ApiResult<T>>`
- **Use case**: executes business flow and returns domain data, page data, or result DTOs
- **Repository**: stays behind domain repository interfaces and does not leak into controllers

Use `ApiResult.paged(page)` in controllers for paginated responses. Return filter metadata from a separate `/facets` endpoint.

## Name query parameters

Use the same parameter names across modules when the meaning matches:

- **Paging**: `page`, `size`
- **Sorting**: repeatable `sort`
- **Text search**: `search`
- **Created date range**: `createdFrom`, `createdTo`
- **Amount range**: `amountMin`, `amountMax`
- **Status filter**: `status`
- **Soft delete filters**: `active`, `deleted`

Avoid aliases such as `limit`, `q`, `from`, and `to` when the API already has a named convention.

Frontend dashboard URLs should use these same names when the URL represents backend query state. For example, prefer `search=nguyen&status=pending` over `q=nguyen&st=pending`. This keeps browser URLs, frontend API clients, and backend request DTOs easy to compare while debugging.

## Use the module list pattern

Use this pattern for admin list endpoints that behave like tables. Replace `{{module}}` with the module name.

```text
GET /api/admin/{{module}}s?search=&page=0&size=20&sort=createdAt,desc
```

Use multiple `sort` parameters when the endpoint needs more than one sort key:

```text
GET /api/admin/{{module}}s?page=0&size=20&sort=status,asc&sort=createdAt,desc
```

Controller shape:

```java
@GetMapping
public ResponseEntity<ApiResult<List<{{Module}}Data>>> list(
        @Valid @ModelAttribute {{Module}}ListRequest filter,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        @ValidSortFields({"id", "name", "createdAt", "updatedAt"})
        Pageable pageable
) {
    var page = get{{Module}}sUseCase.execute(filter.toFilter(), pageable);
    return ResponseEntity.ok(ApiResult.paged(page));
}
```

Use `@RequestParam` instead of `@ModelAttribute` only when the filter has fewer than three fields and will not grow. Prefer a request DTO for admin table filters.

## Format paginated responses

Paginated endpoints return an `ApiResult` with `data` and `meta`.

The response shape is:

```json
{
  "data": [],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

## Split facets from list data

Facets describe filter metadata for the frontend. They can contain static option values, live counts, ranges, or both. Keep them separate from the list response.

Use this pattern for admin table facets:

```text
GET /api/admin/{{module}}s/facets
```

Return `ApiResult.ok(facets)`. Prefer `value`, `label`, and `count` pairs when the dashboard needs badges or tabs.

Rules:

- Return all static values even when `count` is `0`.
- Count facets isolate their own selected filter.
- Sorting and paging do not affect facet counts.
- `meta.totalElements` describes the current list query only; do not use it as an "All" facet when a facet filter is selected.
- List responses stay `data + meta`; do not add new filter metadata to paged list responses.

## Improve the base pattern when needed

The current module pattern works, but it has gaps to fix as modules mature:

- **Filter DTO consistency**: prefer `{{Module}}ListRequest` over many `@RequestParam` values for admin table filters
- **Facet ownership**: return filter metadata from `GET /api/admin/{{module}}s/facets`
- **Legacy list facets**: older modules may still return facets inside list responses; migrate them to `/facets` when touched
- **List DTO shape**: use `{{Module}}ListItemData` when full detail DTOs load nested data or large fields
- **Sort validation**: every pageable admin list should use `@ValidSortFields`
- **Date names**: use `createdFrom` and `createdTo` when filtering by creation time
- **Multiple sort**: keep Spring repeatable `sort` instead of custom `sortBy` and `sortDir`

## Keep status values stable

Use lowercase status values in API contracts. Frontend code can map labels and display styles, but it should not invent uppercase API status values.

Order status values are:

- `pending`
- `confirmed`
- `processing`
- `shipped`
- `delivered`
- `cancelled`
- `refunded`
- `expired`
