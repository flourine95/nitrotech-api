# Audit API consistency

Use this checklist to keep consistency work scoped. Each item must name the rule, the files affected, and the verification command before code changes start.

## Rules

- **Domain boundary**: domain code must not import `com.nitrotech.api.infrastructure`
- **Transaction boundary**: external provider calls must not run inside database transactions
- **Soft delete scope**: repository method names must state whether they read visible, deleted, or all records
- **Slug checks**: slug uniqueness must define whether deleted records still reserve the slug
- **DTO style**: request DTOs should use `record` unless mutable binding is required
- **Helper placement**: keep one-class helpers private; extract shared helpers only after repeated equivalent usage
- **Exception shape**: use structured domain exceptions instead of generic `RuntimeException`

## Done

- [x] Move shipment provider calls outside transactional persistence
- [x] Remove domain imports of infrastructure classes
- [x] Add provider resolver ports for payment and shipping
- [x] Add a payment transaction repository port
- [x] Rename brand slug checks to state the non-deleted scope
- [x] Rename product slug checks to state the non-deleted scope
- [x] Remove duplicate category slug checks and keep non-deleted names
- [x] Rename brand, product, and category detail lookups to state the non-deleted scope
- [x] Align bulk restore and hard-delete checks with single-item operations
- [x] Add an automated domain boundary guard test
- [x] Standardize deleted slug reuse with partial unique indexes
- [x] Replace generic storage runtime exceptions with structured storage errors
- [x] Limit mutable query request DTOs to getter/setter binding
- [x] Add repository tests for soft-deleted slug reuse checks
- [x] Audit entity-to-DTO mapping helpers and keep existing private repository mappers
- [x] Audit helper placement and keep focused provider/repository helpers as-is

## Next audit passes

- No open audit passes in this checklist.

## Current findings

### Soft delete and slug checks

Brand, product, and category repositories now state slug check scope in the domain contract:

- `existsNotDeletedBySlugAndIdNot`
- `existsNotDeletedBySlug`
- `existsNotDeletedBySlugAndIdNot`
- `existsNotDeletedBySlug`

Decision: deleted brand, product, and category slugs can be reused. Database uniqueness is enforced only for records where `deleted_at IS NULL`. Restore use cases must check conflict with non-deleted records before clearing `deleted_at`.

### Payment and shipping boundaries

Payment and shipping now depend on domain ports instead of infrastructure classes. Keep this rule in place with a guard command:

```powershell
rg -n "import com\.nitrotech\.api\.infrastructure" src/main/java/com/nitrotech/api/domain
```

The command should return no matches.

### Transaction boundary

`CreateShipmentUseCase.execute` calls the shipping provider outside transactional persistence. `CreateShipmentTransaction.save` owns the database write, shipment log, and audit record.

Keep this pattern for future provider calls:

- Validate local state before the external call
- Call the provider outside a transaction
- Save local state in a small transactional method

## Verification commands

Run the focused tests after payment or shipping consistency changes:

```powershell
.\gradlew.bat test --tests com.nitrotech.api.domain.payment.usecase.HandlePaymentWebhookUseCaseTest
.\gradlew.bat test --tests com.nitrotech.api.domain.shipping.usecase.* --tests com.nitrotech.api.infrastructure.shipping.*
```

Run the automated boundary guard after domain-layer changes:

```powershell
.\gradlew.bat test --tests com.nitrotech.api.architecture.DomainBoundaryTest
```
