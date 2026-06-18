---
meta:
  contentType: Conceptual
---

# Choose API query conventions

This decision records the shared query parameter and response wrapper conventions for NitroTech APIs. Use it when a module proposes a different list endpoint shape.

## Context for this decision

The strongest admin table APIs use Spring pagination patterns and wrap paginated responses at the controller layer. Order APIs had a thinner list shape and mixed response wrapping inside the use case.

The frontend also had inconsistent names, such as `limit` in one client while the backend expected `size`.

## Decision

Use shared names for common query parameters:

- `page`
- `size`
- repeatable `sort`
- `search`
- `createdFrom`
- `createdTo`
- `status`

Controllers wrap paginated responses with `ApiResult.paged(page)`. Filter metadata belongs in a separate facets endpoint such as `GET /api/admin/{{module}}s/facets`.

Use cases return domain data, `Page<T>`, or module result DTOs. Use cases do not return `ApiResult`.

Use the `{{module}}` list pattern in `API-CONVENTIONS.md` as the baseline for new admin table endpoints.

## Consequences

Frontend API clients must use `size`, not `limit`, when calling paginated backend endpoints.

Modules with dashboard filters can add facets without changing the base response shape.

Order list work should add module-specific request and response DTOs instead of returning full `OrderData` for list rows.
