# Product Data API - phased implementation plan

> Stable companion to [`master-prompt.md`](master-prompt.md) (full scope) and
> [`tasks.md`](tasks.md) (living state - **do not** track progress here).
> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md).
>
> Each phase has a **verification gate**: executable commands that prove the
> phase is really done. A phase is complete only when its gate passes (or the
> blocker is recorded in `tasks.md` per the master prompt validation policy).
> Phases are ordered by dependency; within a phase, tasks (see `tasks.md`) can
> be reordered freely.

## P0 - Documentation foundation

Reorganized corpus, canonical decisions consolidated, facet spec system
(template + authoring prompt + `product-price.md`), this plan and `tasks.md`.

**Gate**: `./scripts/lint.sh` passes; no dangling links to renamed b2b docs.

## P1 - Backend module scaffold

`<module>b2b-api</module>` in the root `pom.xml`; `b2b-api/pom.xml` (parent +
minimum dependencies per master prompt); `B2bApiApplication`; `application.yml`
+ `application-devsec.yml` (jar-excluded) + `b2b-catalog.yml`;
`@ConfigurationProperties` classes + metadata; OpenAPI config; Problem Detail
`@RestControllerAdvice` per the [error catalog](../../architecture/product-data-api-errors.md);
port 8087.

**Gate**: `mvn -pl b2b-api -am install` succeeds; app boots with `devsec`
profile; `/actuator/health` and `/v3/api-docs` respond.

## P2 - Persistence

Flyway `V1__product_data_api_init.sql` implementing the
[data model](../../architecture/product-data-api-data-model.md) 1:1 (13 tables,
CHECK constraints, partial unique indices: one OWNER per org, composite
`(request_id, bucket_id) WHERE type='DEBIT'`); JPA entities + Spring Data
repositories (`credit_transactions` and `admin_audit_events` insert-only).

**Gate**: Testcontainers (Postgres) context test boots Flyway and validates the
JPA mapping.

## P3 - Auth

OIDC (`POST /api/v1/auth/oidc` + refresh/logout/me) for Google, Microsoft,
GitHub (OAuth userinfo, not JWKS), Apple per the
[auth spec](../../architecture/product-data-api-auth.md); JWT HS256 + HttpOnly
cookies (`Domain=.product-data-api.com`); user provisioning + default org +
one-time 2500-credit free grant; org RBAC (`@PreAuthorize` matrix); platform
admin allowlist; API keys (`pdapi_` generate/hash/prefix, create/list/rotate/
revoke, `ApiKeyAuthFilter`, Redis lookup cache, debounced `last_used_at`).

**Gate**: unit tests (provisioning with mocked verifiers, key hashing/rotation/
revocation, role matrix) + integration tests: 401 matrix from the master prompt.

## P4 - Metering and ledger

Redis Lua `reserve`/`refund`/`reconcile` + rate-limit script per the
[redis contract](../../architecture/product-data-api-redis-contract.md);
`settleDebit` expiring-first bucket debit per the
[billing ledger](../../architecture/product-data-api-billing-ledger.md);
usage stream emission; `HardenerBatch` (ShedLock Redis): stream drain, expiry
sweep, balance reconciliation, last-used flush.

**Gate**: ledger test matrix (Testcontainers Postgres + Redis): reserve/402,
no-data refund, idempotent debit, debit order, rollover cap, cancellation
expiry, reconciliation.

## P5 - Price facet endpoint

`GET /api/v1/products/{gtin}/price` per the
[API contract](../../architecture/product-data-api-contract.md) and the facet
spec [`product-price.md`](../facets/product-price.md): GTIN validation
(`BarcodeValidationService`) -> Long normalization (verify the leading-zeros
rule against the service output) -> `getByIdWithoutEmbedding` -> allow-list
sanitized DTO mapping (adapt `front-api` `ProductMappingService`) -> freshness
billable decision -> envelope + headers. Full OpenAPI annotations.

**Gate**: curl matrix from the
[runbook](../../operations/product-data-api-local-runbook.md) section 5 against
devsec ES (401/400/404/200-nonbillable/200-billable/402), plus the price
integration tests.

## P6 - Stripe billing

Catalog binding, pack/subscription Checkout, billing portal, webhook endpoint
(signature + `stripe_events` idempotency + out-of-order upserts), grant->bucket
mapping, rollover cap, cancellation +30d expiry, invoices mirror, manual admin
grants, per the [stripe contract](../../architecture/product-data-api-stripe-contract.md).

**Gate**: webhook unit/integration tests (duplicate event no-op, pack grant,
subscription grant + cap, cancellation) and a manual Stripe CLI session
(`stripe listen` + `stripe trigger`) recorded in `tasks.md`.

## P7 - Customer and admin REST

All `/api/v1/customer/**` endpoints (billing catalog/balance/transactions/
invoices/subscriptions, checkout, portal, playground proxy
`POST /api/v1/customer/playground/products/price`) and `/api/v1/admin/**`
endpoints (orgs, transactions, manual grants, key oversight, usage) with role
gating and `admin_audit_events`.

**Gate**: endpoint tests with role matrix; OpenAPI complete for every endpoint;
`mvn -pl b2b-api -am test` green.

## P8 - Frontend scaffold and codegen

`b2b-frontend/` bootstrapped from `infera/apps/frontend` (flat layout per
[`build.md`](../frontend/build.md)); Nuxt 4 + Vuetify 4 + i18n
(`prefix_except_default`, `/fr/`) + `@nuxtjs/seo` + `@nuxt/content`; OpenAPI
client generation from the `b2b-api` spec (committed static spec).

**Gate**: `pnpm --dir b2b-frontend lint && pnpm --dir b2b-frontend typecheck &&
pnpm --dir b2b-frontend build` pass; generated client compiles.

## P9 - Frontend pages

Public pages (landing, pricing from backend catalog, faq/contact/legal/privacy/
terms), auth login, dashboard (overview/usage/api-keys/billing/invoices/
settings), admin pages - per the route map and page requirements in
[`ui-spec.md`](../frontend/ui-spec.md), reusing the shared `B2b*` components.

**Gate**: focused tests from `ui-spec.md` section 12 (login/session, dashboard
states, key UI, admin gating, manual grant flow); `pnpm test` green.

## P10 - Public docs, playground, SEO

`@nuxt/content` docs in `content/en` + `content/fr` (getting-started,
api-reference, authentication, billing-and-credits, errors, products/price,
java/python quickstarts, FAQ - real French content, not placeholders);
playground sample + live modes; SEO foundation per
[`build.md`](../frontend/build.md) section 4 and the facet SEO plan in
[`product-price.md`](../facets/product-price.md).

**Gate**: i18n routing + localized SEO metadata + docs rendering + playground
state tests; sitemap/robots generated.

## P11 - Final validation and handoff

Full acceptance criteria of the master prompt; CI wiring (b2b-api in the Maven
CI reactor, a b2b-frontend lint/typecheck/test/build workflow); module guides
and runbook refreshed against reality; handoff summary per the master prompt.

**Gate**: `mvn -pl b2b-api -am test && mvn -pl b2b-api -am install`,
`./scripts/lint.sh`, all four `pnpm` commands, and the acceptance checklist
ticked in `tasks.md` (or blockers recorded).
