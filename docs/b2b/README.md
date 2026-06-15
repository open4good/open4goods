# Product Data API (B2B) - documentation corpus

This folder is the foundation for the **Product Data API** brick: a metered,
freemium B2B product-data API (`api.product-data-api.com`) and its frontend
(`product-data-api.com`), built as `b2b-api` + `b2b-frontend` in this monorepo.

**Authority chain**: [`00-canonical-decisions.md`](00-canonical-decisions.md)
wins over every other document. The master prompt defines the v1 scope; the
architecture specs define the contracts; `implementation/plan.md` +
`implementation/tasks.md` define and track execution.

## Reading order for an implementing agent

1. This README (corpus map).
2. [`00-canonical-decisions.md`](00-canonical-decisions.md) - locked decisions.
3. [`implementation/tasks.md`](implementation/tasks.md) - **resume protocol +
   current state**. Always start here when picking up work.
4. [`implementation/plan.md`](implementation/plan.md) - phase you are in, its
   verification gate.
5. [`implementation/master-prompt.md`](implementation/master-prompt.md) - full
   v1 scope and acceptance criteria.
6. The architecture spec(s) relevant to the current phase.

## Corpus map

### Business (why this product, what data we have)

| Doc | Content |
|---|---|
| [`business/competition.md`](business/competition.md) | Competitive study (PriceAPI, SerpApi, Bright Data, Winamaz...), billing-model comparison, positioning (French) |
| [`business/data-coverage.md`](business/data-coverage.md) | Measured Elasticsearch coverage of the nudger index (2026-06-02) + the reproducible ES queries (French) |

### Product (what we sell)

| Doc | Content |
|---|---|
| [`product/facet-catalog.md`](product/facet-catalog.md) | Prioritised facet catalogue, credit tiers, implementation waves, product open questions (French) |
| [`facets/`](facets/README.md) | One spec per shipped facet + template + AI authoring prompt. [`facets/product-price.md`](facets/product-price.md) is the v1 facet and the canonical example |

### Implementation (how we build it)

| Doc | Content |
|---|---|
| [`implementation/master-prompt.md`](implementation/master-prompt.md) | Canonical, executable v1 implementation prompt (scope, contracts, tests, acceptance criteria) |
| [`implementation/plan.md`](implementation/plan.md) | Phased plan P0-P11 with per-phase verification gates |
| [`implementation/tasks.md`](implementation/tasks.md) | **Living** task state, resume protocol, session log |

### Frontend

| Doc | Content |
|---|---|
| [`frontend/ui-spec.md`](frontend/ui-spec.md) | UX principles, route map, page-level requirements, components, acceptance criteria |
| [`frontend/build.md`](frontend/build.md) | Project layout, OpenAPI codegen pipeline, cookie/session model, SEO foundation |

### Contracts and operations (outside this folder, per repo conventions)

- Architecture specs: [`../architecture/`](../architecture/) -
  [data model](../architecture/product-data-api-data-model.md),
  [redis contract](../architecture/product-data-api-redis-contract.md),
  [billing ledger](../architecture/product-data-api-billing-ledger.md),
  [API contract](../architecture/product-data-api-contract.md),
  [errors](../architecture/product-data-api-errors.md),
  [auth](../architecture/product-data-api-auth.md),
  [stripe](../architecture/product-data-api-stripe-contract.md),
  [ops](../architecture/product-data-api-ops.md)
- Decision record: [ADR 0005](../adr/0005-product-data-api-b2b-v1.md)
- Local runbook: [`../operations/product-data-api-local-runbook.md`](../operations/product-data-api-local-runbook.md)
- Module guides: [`b2b-api/AGENTS.md`](../../b2b-api/AGENTS.md),
  [`b2b-frontend/AGENTS.md`](../../b2b-frontend/AGENTS.md)

## Status

| Area | State |
|---|---|
| Documentation foundation | Complete (this corpus) |
| `b2b-api` code | P1-P4 complete; P5 price facet endpoint in progress. Resume from [`implementation/tasks.md`](implementation/tasks.md) P5.4 |
| `b2b-frontend` code | Not started |
| Facet specs | `product.price` done; future facets are generated per [`facets/authoring-prompt.md`](facets/authoring-prompt.md) |

## Current resume pointer

The living state is [`implementation/tasks.md`](implementation/tasks.md). As of
2026-06-15, the last completed gate is P4.5 (metering and ledger matrix with
Testcontainers Postgres + Redis). The active implementation phase is P5:
`GET /api/v1/products/{gtin}/price`.

Before starting new work, follow the resume protocol in
[`implementation/tasks.md`](implementation/tasks.md): re-run the verification
gate for the last completed phase, fix any drift, then continue at the first
non-done task.
