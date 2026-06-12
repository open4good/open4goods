# b2b-api

Backend for the **Product Data API** (B2B), served on `product-data-api.com`.
A metered, authenticated product-data API over the open4goods Elasticsearch
index. v1 exposes the **price facet** only, with "No data, no pay" billing.

- Stack: Spring Boot 4 / Java 21, package `org.open4goods.b2bapi`, port `8087`.
- Auth: OIDC (dashboard) + opaque `pdapi_` API keys (data endpoints).
- Storage: Postgres (authoritative credits/ledger) + Redis (hot path) +
  Elasticsearch (products) + Stripe (billing).

## Quick start

See the [local runbook](../docs/operations/product-data-api-local-runbook.md):
start Postgres + Redis, `mvn -pl b2b-api -am install`, run with the `devsec`
profile, seed an org/key, then exercise
`GET /api/v1/products/{gtin}/price` with `Authorization: Bearer pdapi_...`.

## Documentation

- [Canonical decisions](../docs/b2b/00-canonical-decisions.md)
- [Master implementation prompt](../docs/b2b/implementation/master-prompt.md)
- [Phased plan](../docs/b2b/implementation/plan.md) & [task state](../docs/b2b/implementation/tasks.md)
- [Price facet spec](../docs/b2b/facets/product-price.md)
- [ADR 0005](../docs/adr/0005-product-data-api-b2b-v1.md)
- Architecture: [data model](../docs/architecture/product-data-api-data-model.md),
  [redis contract](../docs/architecture/product-data-api-redis-contract.md),
  [billing ledger](../docs/architecture/product-data-api-billing-ledger.md),
  [API contract](../docs/architecture/product-data-api-contract.md),
  [errors](../docs/architecture/product-data-api-errors.md),
  [auth](../docs/architecture/product-data-api-auth.md),
  [stripe](../docs/architecture/product-data-api-stripe-contract.md),
  [ops](../docs/architecture/product-data-api-ops.md)
- [Agent guide](AGENTS.md)

## API docs (running instance)

`/v3/api-docs` (OpenAPI JSON), `/swagger-ui`, Redoc/Scalar.
