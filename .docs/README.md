# Documentation index

This directory contains backend documentation for Nitrotech API. Start with the project root [README](../README.md) when you only need setup and runtime instructions.

## Documents

### Architecture

[ARCHITECTURE.md](./ARCHITECTURE.md) explains the layered Spring Boot structure, data flow, module responsibilities, and shared architectural patterns.

Use it when you need to understand where a controller, use case, repository, mapper, provider, or shared utility belongs.

### Coding standards

[CODING-STANDARDS.md](./CODING-STANDARDS.md) explains Java, Spring, API, persistence, exception, naming, and testing conventions.

Use it when you need examples or rationale behind the shorter rules in [AGENTS.md](../AGENTS.md).

### API conventions

[api/CONVENTIONS.md](./api/CONVENTIONS.md) defines shared API shapes, query parameter names, pagination, sorting, facets, and the `{{module}}` admin list pattern.

[api/modules/ORDER.md](./api/modules/ORDER.md) defines the order API contract for customer and admin surfaces.

[API-CONSISTENCY-AUDIT.md](./API-CONSISTENCY-AUDIT.md) gives checks for controller boundaries, response wrappers, pagination, soft delete scope, provider transactions, and exception shape.

### Database design

[DATABASE-DESIGN.md](./DATABASE-DESIGN.md) describes schema principles, soft-delete behavior, table relationships, and migration notes.

This file should be synced with Flyway migrations before it is used as a source of truth. The current schema has evolved through migrations in `src/main/resources/db/migration`.

## Reading order

New backend developers should read:

1. [README.md](../README.md)
2. [ARCHITECTURE.md](./ARCHITECTURE.md)
3. [CODING-STANDARDS.md](./CODING-STANDARDS.md)
4. [api/CONVENTIONS.md](./api/CONVENTIONS.md)
5. [DATABASE-DESIGN.md](./DATABASE-DESIGN.md)

Frontend developers usually need:

1. Swagger UI at `http://localhost:8080/swagger-ui.html`
2. [DATABASE-DESIGN.md](./DATABASE-DESIGN.md) for entity relationships and response context

DevOps or deployment work usually starts with:

1. [README.md](../README.md)
2. [ARCHITECTURE.md](./ARCHITECTURE.md)

## Maintenance rules

- Keep setup instructions in the root `README.md`
- Keep agent-facing rules in `AGENTS.md`
- Keep long-form explanations in this directory
- Update this index when adding or removing documentation files
- Sync database documentation after schema migrations
