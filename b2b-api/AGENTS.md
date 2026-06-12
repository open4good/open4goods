# b2b-api - agent guide

> Extends the root [`AGENTS.md`](../AGENTS.md). This module is the B2B "Product
> Data API" backend. Read the canonical decisions and architecture specs before
> changing anything here.

## What this module is

Spring Boot 4 / Java 21 Maven module (`org.open4goods.b2bapi`) exposing a metered,
authenticated product-data API. v1 = the price facet only
(`GET /api/v1/products/{gtin}/price`). Default local port **8087**.

## Read first

- [docs/b2b/00-canonical-decisions.md](../docs/b2b/00-canonical-decisions.md) - source of truth
- [docs/b2b/implementation/master-prompt.md](../docs/b2b/implementation/master-prompt.md) - master implementation prompt
- [docs/b2b/implementation/tasks.md](../docs/b2b/implementation/tasks.md) - **living task state; follow its resume protocol before any work** (phases: [plan.md](../docs/b2b/implementation/plan.md))
- [docs/b2b/facets/product-price.md](../docs/b2b/facets/product-price.md) - v1 facet spec (coverage queries, no-data-no-pay matrix)
- [docs/adr/0005-product-data-api-b2b-v1.md](../docs/adr/0005-product-data-api-b2b-v1.md)
- Architecture specs: data model, redis contract, billing ledger, API contract,
  errors, auth, stripe, ops (`docs/architecture/product-data-api-*.md`)
- [docs/operations/product-data-api-local-runbook.md](../docs/operations/product-data-api-local-runbook.md)

## Conventions (module-specific)

- Spring Boot **4.0.6** from the open4goods parent (ignore the stale "Spring Boot 3"
  note in `front-api/AGENTS.md`; its controller/OpenAPI/Javadoc style is still a
  good reference).
- Layered packages: thin controllers, services own workflows, repositories own
  persistence, DTOs are records, `@ConfigurationProperties` have metadata.
- **Postgres is authoritative** for credits/ledger; **Redis is hot-path only**.
  Never persist the ledger in Redis.
- Metering is a service-level reserve->settle workflow, never controller logic.
  No-data-no-pay and GTIN-first-strict are contractual - do not bypass.
- Sanitized DTOs only: never expose `compensation`, `affiliationToken`, internal
  datasource ids, crawler/cache keys, or internal scoring (allow-list mapping).
- `application-devsec.yml` is excluded from the packaged jar (as `front-api`).
- Secrets only via env/properties; never commit keys.

## Reuse (do not re-implement)

- GTIN validation: `org.open4goods.commons.services.BarcodeValidationService`
- Product fetch: `ProductRepository.getByIdWithoutEmbedding(Long)`
- Offer mapping: adapt `front-api` `ProductMappingService` (allow-list into B2B DTOs)
- Controller/security/OpenAPI style: `front-api` `WebSecurityConfig`, `OpenApiConfig`
- OIDC verification: reference Infera `apps/backend` verifier services

## Validate

```bash
mvn -pl b2b-api -am test
mvn -pl b2b-api -am install
./scripts/lint.sh
```
