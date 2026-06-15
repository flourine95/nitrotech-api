# Karpathy Behavioral Guidelines

These guidelines reduce common LLM coding mistakes. Merge them with project-specific instructions as needed.

Tradeoff: these guidelines bias toward caution over speed. For trivial tasks, use judgment.

## Think Before Coding

Do not assume, hide confusion, or skip tradeoffs.

Before implementing:
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them instead of picking silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop, name what is confusing, and ask.

## Simplicity First

Use the minimum code that solves the problem. Do not add speculative features.

- No features beyond what was asked.
- No abstractions for single-use code.
- No flexibility or configurability that was not requested.
- No error handling for impossible scenarios.
- If 200 lines could be 50, simplify.

Ask: would a senior engineer say this is overcomplicated? If yes, simplify.

## Surgical Changes

Touch only what is necessary. Clean up only your own changes.

When editing existing code:
- Do not improve adjacent code, comments, or formatting unless required.
- Do not refactor things that are not broken.
- Match existing style, even if you would choose differently.
- If unrelated dead code is noticed, mention it instead of deleting it.

When changes create orphans:
- Remove imports, variables, or functions made unused by the current change.
- Do not remove pre-existing dead code unless asked.

Every changed line should trace directly to the user's request.

## Goal-Driven Execution

Define verifiable success criteria and loop until verified.

Transform tasks into concrete checks:
- "Add validation" -> write tests for invalid inputs, then make them pass.
- "Fix the bug" -> write a test that reproduces it, then make it pass.
- "Refactor X" -> ensure tests pass before and after.

For multi-step tasks, state a brief plan:

```text
1. [Step] -> verify: [check]
2. [Step] -> verify: [check]
3. [Step] -> verify: [check]
```

These guidelines are working if diffs have fewer unnecessary changes, fewer rewrites happen due to overcomplication, and clarifying questions come before implementation mistakes.

## NitroTech API project rules

Use these rules for changes under `nitrotech-api`. Treat this file as the canonical project guidance for Codex.

### Architecture

- Keep the existing layered structure: `application` for controllers/request DTOs, `domain` for use cases/repository interfaces/domain DTOs, `infrastructure` for persistence/providers/config, and `shared` for cross-cutting code.
- Controllers should stay thin. Map requests to commands, call use cases, and return `ResponseEntity<ApiResult<T>>`.
- Use custom domain exceptions with structured error codes. Do not throw generic `RuntimeException` for business failures.

### REST API conventions

- Prefix endpoints with `/api`.
- Use plural resource nouns and kebab-case for multi-word resources, for example `/api/products` and `/api/product-variants`.
- Use HTTP methods for actions. Do not put verbs such as `createProduct` in URLs.
- For paginated endpoints, use Spring Data `Page<T>` and `Pageable`, then return `ApiResult.paged(page)`.
- Declare sortable fields as `private static final Set<String>` in controllers before passing them to `SortUtils`.
- Validation messages use sentence case with no trailing period, for example `Name is required`.
- Error codes use `SCREAMING_SNAKE_CASE`, usually `ENTITY_ACTION` or `ENTITY_STATE`, for example `PRODUCT_NOT_FOUND`.
- Create request records use primitive `boolean`; update request records use `Boolean` when null means "leave unchanged".

### Java style

- Use explicit imports. Avoid fully qualified names in code except for unavoidable name conflicts.
- Use constructor injection with `@RequiredArgsConstructor`; do not use field injection.
- On JPA entities, use only `@Getter`, `@Setter`, and `@NoArgsConstructor`. Do not use `@Data`, `@ToString`, or `@EqualsAndHashCode` on entities.
- Do not add Lombok to records.
- Use `@Slf4j` and parameterized logs. Do not use `System.out` or `System.err`.
- Prefer method references for straightforward stream operations.
- Extract complex stream predicates or business checks into named entity methods such as `isActive`, `hasStock`, or `canBeCancelled`.
- Check nulls, collection bounds, and `instanceof` before casting aggregate query results.

### Persistence

- Keep validation annotations in request DTOs. Entities should express database constraints with JPA annotations.
- This project uses soft deletes. Custom queries for active records must filter `deletedAt IS NULL`.
- Use naming that exposes soft-delete scope: `findActive*`, `findDeleted*`, `existsActive*`, `countActive*`.
- Avoid N+1 queries. Collect IDs and use batch queries with `IN`.
- Use `@Transactional` for multi-step writes and use cases that orchestrate multiple repositories.
- Keep transactions short. Do not call external APIs inside a transaction.
- For PostgreSQL JSON columns, use `@JdbcTypeCode(SqlTypes.JSON)` and `columnDefinition = "jsonb"`.
- Document `Object[]` aggregate query results with Javadoc and cast defensively.

### Verification

- For backend changes, run `.\gradlew.bat test` from `nitrotech-api` when feasible.
