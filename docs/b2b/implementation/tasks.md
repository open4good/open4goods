# Product Data API - task state (living document)

> **This file is the single source of truth for implementation progress.**
> Phases and gates are defined in [`plan.md`](plan.md); full scope in
> [`master-prompt.md`](master-prompt.md). Statuses: `[ ]` todo, `[~]` in
> progress, `[x]` done, `[!]` blocked (always with a note).

## Resume protocol (read before any work)

1. Read this file top to bottom, then the `plan.md` section for the first
   non-done phase.
2. **Do not trust checkboxes.** Re-run the verification gate of the last phase
   marked done. If it fails, fix the drift before starting new work, and
   correct the checkboxes to match reality.
3. Work in task order unless a dependency forces otherwise. Mark a task `[~]`
   when you start it.
4. A task is `[x]` only when its code/doc exists **and** the relevant part of
   the phase gate passes. Record blocked validations as `[!]` with the exact
   command, failure, and smallest next step (master prompt validation policy).
5. Update this file **in the same commit** as the work it describes, and append
   a one-line entry to the session log below.

## P0 - Documentation foundation

- [x] P0.1 Reorganize `docs/b2b` corpus (business/product/facets/implementation/frontend)
- [x] P0.2 Fix corpus inconsistencies; ratify new decisions in `00-canonical-decisions.md`
      (single frontend domain, composite DEBIT index, reconciliation-based crash
      recovery, `pdreq_` prefix)
- [x] P0.3 Facet system: `facets/README.md`, `_template.md`, `authoring-prompt.md`,
      `product-price.md`
- [x] P0.4 `plan.md` + this `tasks.md`; update `docs/README.md` and module guides

## P1 - Backend module scaffold

- [x] P1.1 Add `b2b-api` module to root `pom.xml`; create `b2b-api/pom.xml` (parent, deps per master prompt)
- [x] P1.2 `B2bApiApplication` + layered package skeleton (`org.open4goods.b2bapi`)
- [x] P1.3 `application.yml` / `application-devsec.yml` (jar-excluded) / `b2b-catalog.yml`
- [x] P1.4 `@ConfigurationProperties` classes + `additional-spring-configuration-metadata.json`
- [x] P1.5 OpenAPI config (`productDataApiKey` scheme, Swagger UI, Redoc/Scalar)
- [x] P1.6 Problem Detail `@RestControllerAdvice` + exception types (error catalog)
- [x] P1.7 Gate: `mvn -pl b2b-api -am install`; boot devsec; `/actuator/health`, `/v3/api-docs`

## P2 - Persistence

- [x] P2.1 Flyway `V1__product_data_api_init.sql` (13 tables, CHECKs, partial unique indices)
- [x] P2.2 JPA entities + repositories (ledger/audit insert-only)
- [x] P2.3 Gate: Testcontainers Postgres context test (Flyway + mapping validation)

## P3 - Auth

- [x] P3.1 JWT service (HS256, access/refresh) + cookie writer (`Domain=.product-data-api.com`)
- [x] P3.2 OIDC verifiers: Google, Microsoft, Apple (JWKS); GitHub (OAuth userinfo)
- [x] P3.3 `POST /api/v1/auth/oidc` + refresh/logout/me; user provisioning; default org + one-time free grant
- [x] P3.4 Org RBAC (`@PreAuthorize` matrix) + platform-admin allowlist
- [x] P3.5 API keys: generate/hash/prefix, create/list/rotate/revoke endpoints
- [x] P3.6 `ApiKeyAuthFilter` + Redis key cache + debounced `last_used_at`
- [x] P3.7 Gate: auth unit tests + 401 integration matrix

## P4 - Metering and ledger

- [x] P4.1 Redis Lua scripts (reserve/refund/reconcile, rate limit) + script loader
- [x] P4.2 `settleDebit` expiring-first bucket debit (locked tx, composite idempotency)
- [x] P4.3 Grant service (free grant, pack, subscription + rollover cap, manual, cancellation expiry)
- [x] P4.4 Usage stream emission + `HardenerBatch` (ShedLock): drain, expiry sweep, reconcile, last-used flush
- [x] P4.5 Gate: ledger test matrix (Testcontainers Postgres + Redis)

## P5 - Price facet endpoint

- [x] P5.1 Sanitized DTOs (`B2bResponse`/`B2bMeta`/`B2bPriceDto`/`B2bOfferDto`...) with `@Schema` examples
- [x] P5.2 Allow-list mapper from `Product`/`AggregatedPrices` (adapt `ProductMappingService` logic)
- [x] P5.3 Verify GTIN -> Long normalization rule against `BarcodeValidationService` (leading zeros)
- [~] P5.4 `GET /api/v1/products/{gtin}/price` controller + metering workflow + envelope/headers
- [ ] P5.5 Gate: runbook section 5 curl matrix against devsec ES + integration tests
- [ ] P5.6 Run the coverage queries of [`product-price.md`](../facets/product-price.md); record results + date there

## P6 - Stripe billing

- [ ] P6.1 Catalog service (`b2b-catalog.yml` -> public catalog DTO)
- [ ] P6.2 Checkout (pack, subscription) + billing portal + lazy `stripe_customers`
- [ ] P6.3 Webhook endpoint (signature, `stripe_events` idempotency, out-of-order upserts)
- [ ] P6.4 Event handlers -> grants/buckets/invoices (per stripe contract)
- [ ] P6.5 Gate: webhook tests + manual Stripe CLI session (record output here)

## P7 - Customer and admin REST

- [ ] P7.1 Customer billing/usage endpoints (balance, transactions, invoices, subscriptions)
- [ ] P7.2 Playground proxy `POST /api/v1/customer/playground/products/price`
- [ ] P7.3 Admin endpoints (orgs, transactions, manual grants, key oversight, usage) + audit events
- [ ] P7.4 Gate: role-matrix endpoint tests; OpenAPI complete; `mvn -pl b2b-api -am test`

## P8 - Frontend scaffold and codegen

- [ ] P8.1 Bootstrap `b2b-frontend/` from `infera/apps/frontend` (flat layout)
- [ ] P8.2 i18n (`prefix_except_default`, en `/`, fr `/fr/`) + `@nuxtjs/seo` + `@nuxt/content`
- [ ] P8.3 OpenAPI codegen pipeline (committed static spec + `generate:openapi`)
- [ ] P8.4 Gate: lint + typecheck + build

## P9 - Frontend pages

- [ ] P9.1 Shared `B2b*` components (ui-spec section 8)
- [ ] P9.2 Public pages (landing, pricing from backend catalog, faq/contact/legal/privacy/terms)
- [ ] P9.3 Auth login + session state
- [ ] P9.4 Dashboard pages (overview, usage, api-keys, billing, invoices, settings)
- [ ] P9.5 Admin pages + role gating
- [ ] P9.6 Gate: focused tests (ui-spec section 12 subset)

## P10 - Public docs, playground, SEO

- [ ] P10.1 Docs content en+fr (getting-started, api-reference, authentication, billing-and-credits, errors, products/price, java, python, FAQ)
- [ ] P10.2 Playground sample mode + live mode (proxy)
- [ ] P10.3 SEO foundation (build.md section 4) + price facet SEO plan (product-price.md)
- [ ] P10.4 Gate: i18n/SEO/docs/playground tests; sitemap + robots

## P11 - Final validation and handoff

- [ ] P11.1 CI: b2b-api in Maven CI; b2b-frontend workflow (lint/typecheck/test/build)
- [ ] P11.2 Refresh module guides + runbook against reality
- [ ] P11.3 Full acceptance checklist (master prompt) - tick or record blockers here
- [ ] P11.4 Handoff summary (master prompt handoff requirements)

## Facet runs (recurring, post-v1)

Each new facet is a run of the lifecycle in [`facets/README.md`](../facets/README.md):
add a `FACET-<id>` section here when one starts (spec -> coverage -> catalog ->
backend -> docs -> SEO -> launch).

## Session log

- 2026-06-12: P0 done - corpus reorganized, inconsistencies fixed, facet system
  and plan/tasks created. Next: P1.1.
- 2026-06-15: P1 done - backend module scaffold added, install gate passed,
  devsec boot verified with `/actuator/health` and `/v3/api-docs`. Next: P2.1.
- 2026-06-15: P2 done - Flyway schema, JPA entities/repositories, and
  Testcontainers Postgres mapping gate added; `mvn -pl b2b-api -am test`
  passed. Next: P3.1.
- 2026-06-15: P3.1 done - HS256 access/refresh JWT service and HttpOnly cookie
  writer added; focused JWT/cookie tests and `mvn -pl b2b-api -am test`
  passed. Next: P3.2.
- 2026-06-15: P3.2 done - JWKS OIDC verifiers for Google, Microsoft, and
  Apple plus GitHub OAuth userinfo verification added; focused verifier tests
  and `mvn -pl b2b-api -am test` passed. Next: P3.3.
- 2026-06-15: P3.3 done - OIDC login, refresh/logout/me endpoints, session
  token resolution, user provisioning, default org creation, admin-email
  allowlist, and one-time free grant ledger writes added; focused auth tests
  and `mvn -pl b2b-api -am test` passed. Next: P3.4.
- 2026-06-15: P3.4 done - method security enabled, dashboard JWT filter and
  principal/authority mapping added, platform admin route gate wired, and org
  RBAC matrix service covered by tests; `mvn -pl b2b-api -am test` passed.
  Next: P3.5.
- 2026-06-15: P3.5 done - API key generation, SHA-256 hashing, display
  prefixes, create/list/rotate/revoke customer endpoints, ownership checks, and
  key lifecycle tests added; `mvn -pl b2b-api -am test` passed. Next: P3.6.
- 2026-06-15: P3.6 done - Product API `pdapi_` bearer filter,
  Postgres-backed key lookup, Redis hash cache, cache eviction on rotate/revoke,
  last-used debounce, and focused filter/auth tests added; `mvn -pl b2b-api -am
  test` passed. Next: P3.7.
- 2026-06-15: P3.7 done - auth gate completed with Testcontainers-backed
  Product API 401 matrix, valid-key security pass-through coverage, and
  no-datasource context verification; `mvn -pl b2b-api -am test` passed. Next:
  P4.1.
- 2026-06-15: P4.1 done - Redis Lua reserve/refund/reconcile/rate-limit scripts,
  lazy SCRIPT LOAD/EVALSHA invocation, Redis/rate-limit configuration, and
  Testcontainers Redis coverage added; `mvn -pl b2b-api -am test` passed. Next:
  P4.2.
- 2026-06-15: P4.2 done - durable expiring-first `settleDebit` added with
  bucket locks, duplicate request replay guard, rollback-on-insufficient
  handling, and Testcontainers Postgres coverage; `mvn -pl b2b-api -am test`
  passed. Next: P4.3.
- 2026-06-15: P4.3 done - credit grant service added for free grants, packs,
  subscription grants with rollover cap, manual grants with admin audit, and
  cancellation expiry; bucket catalog ids added for per-plan caps; `mvn -pl
  b2b-api -am test` passed. Next: P4.4.
- 2026-06-15: P4.4 done - Redis usage stream writer, ShedLock Redis
  configuration, hardener drain/expiry/reconcile/last-used flush, and
  Testcontainers Postgres+Redis coverage added; `mvn -pl b2b-api -am test`
  passed. Next: P4.5.
- 2026-06-15: P4.5 done - ledger/metering matrix passed for durable debits,
  grants and rollover, Redis Lua metering, and hardener recovery with
  Testcontainers Postgres+Redis; focused matrix and `mvn -pl b2b-api -am test`
  passed. Next: P5.1.
- 2026-06-15: P5.1 done - sanitized product DTO envelope, metadata, offer,
  price, trend, and history summary records added with OpenAPI schema examples;
  DTO contract test and `mvn -pl b2b-api -am test` passed. Next: P5.2.
- 2026-06-15: P5.2 done - allow-list price mapper added from Product and
  AggregatedPrices into sanitized B2B DTOs with freshness filtering, grouped
  offers, trend/history summaries, currency conversion, and redaction tests;
  `mvn -pl b2b-api -am test` passed. Next: P5.3.
- 2026-06-15: P5.3 done - GTIN normalization service added around
  BarcodeValidationService, including leading-zero validator output checks,
  Long product id conversion, invalid checksum/format failures, and Spring bean
  wiring; `mvn -pl b2b-api -am test` passed. Next: P5.4.
