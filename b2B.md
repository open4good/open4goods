# Product Data API - B2B v1 master implementation prompt

You are an AI coding agent working in `/home/goulven/git/open4goods`.
Implement the first production-grade B2B vertical for **Product Data API**,
served publicly on `product-data-api.com`.

This document is the executable implementation prompt. Do not treat it as a
brainstorming note. Implement the scope below end to end, validate it, and
leave the repository in a state that another engineer can run locally.

## Mission

Build a complete B2B product-data service for the **price facet** only:

- `b2b-api`: a Spring Boot 4 / Java 21 Maven module in the open4goods reactor.
- `b2b-frontend`: a Nuxt 4 / Vue 3 / Vuetify 4 frontend, based on the Infera
  frontend patterns.
- OIDC customer authentication, organizations, members, roles, API keys, credit
  metering, Stripe billing, admin dashboard, public docs, and a playground.
- The public value proposition is "No data, no pay": invalid GTINs, missing
  products, empty data, or stale price-only data must not debit credits.

v1 must be production-grade for the price vertical. Do not implement the full
future facet catalog in v1. Build the architecture so future facets can reuse
the same catalog, metering, docs, dashboard, and playground patterns.

## Mandatory discovery before coding

Before editing files, inspect and obey the repository instructions:

- `AGENTS.md`
- `CLAUDE.md`
- nearest module `AGENTS.md` files relevant to touched paths
- `docs/README.md`
- `docs/conventions/documentation-guidelines.md`
- `docs/adr/README.md`

Use the following reference implementations, but adapt them to open4goods:

- `/home/goulven/git/infera/apps/frontend`
- `/home/goulven/git/infera/apps/backend`
- `front-api/`
- `frontend/`

When instructions conflict:

- The open4goods root `AGENTS.md` and root `pom.xml` win for Java version,
  Spring Boot version, build conventions, and repo documentation policy.
- `front-api/AGENTS.md` is useful for OpenAPI/Javadoc/controller style, but its
  Spring Boot 3 wording is stale. Use Spring Boot 4 from the root parent.
- The Infera apps are the reference for OIDC, organization UX, API key rotation,
  billing/admin/dashboard patterns, OpenAPI codegen, `domains/` mapping, and
  Nuxt 4 / Vuetify 4 structure.

## Non-negotiable technical decisions

- Java: 21.
- Backend framework: Spring Boot 4 from the open4goods parent.
- Backend package root: `org.open4goods.b2bapi`.
- Frontend stack: Nuxt 4, Vue 3, Vuetify 4, TypeScript, `@nuxtjs/i18n`,
  `@nuxtjs/seo`, `@nuxt/content`, generated OpenAPI client, `domains/` mapping.
- Customer-facing brand: **Product Data API**.
- Public domain: `product-data-api.com`.
- Default public language: English at `/`.
- French mirror: `/fr/`.
- API localization query: `language=en|fr`, default `en`.
- External data API authentication: `Authorization: Bearer pdapi_...`.
- Customer/admin dashboard authentication: OIDC session cookies plus JWT
  bearer compatibility, following Infera backend/frontend patterns.
- OIDC providers in v1: Google, Microsoft, GitHub, Apple.
- Organization model: organizations own credits, API keys, billing, members,
  and usage.
- Organization roles: `OWNER`, `ADMIN`, `DEVELOPER`, `BILLING`.
- Credit authority: Postgres is authoritative for balances, buckets, and the
  append-only transaction ledger.
- Redis role: hot atomic reservation/counter/cache/event stream only. Redis is
  never the authoritative ledger.
- Billing in v1: prepaid credit packs, subscriptions, and manual admin grants.
- Billing catalog source: YAML application catalog with Stripe price IDs mapped
  by config/env.
- API key secret storage: show the clear key only at creation and rotation;
  persist only prefix and hash.
- Price data exposure: sanitized public DTO with provenance and freshness; no
  compensation, affiliate tokens, internal crawler details, or private
  commercial fields.

## Backend module: `b2b-api`

Add `<module>b2b-api</module>` to the root `pom.xml` near `front-api`.

Create `b2b-api/pom.xml` using the open4goods parent and Spring Boot 4.
Reuse dependency style from `front-api/pom.xml`, but keep the module smaller
and focused.

Minimum dependencies:

- open4goods modules: `model`, `product-repository`, `verticals`,
  `serialisation`, `commons`
- Spring Boot starters: `web`, `security`, `oauth2-resource-server`,
  `validation`, `actuator`, `data-redis`, `data-jpa`
- OpenAPI: `springdoc-openapi-starter-webmvc-ui`
- Database/migrations: PostgreSQL JDBC, Flyway
- Billing: `stripe-java`
- Configuration metadata: `spring-boot-configuration-processor`
- Tests: Spring Boot test, Spring Security test, Testcontainers JUnit,
  PostgreSQL, Redis

Application entry point:

- `org.open4goods.b2bapi.B2bApiApplication`
- `@SpringBootApplication(scanBasePackages = "org.open4goods")`
- `@ConfigurationPropertiesScan("org.open4goods")`
- `@EnableCaching`
- `@EnableScheduling`

Use the project layered package conventions. Controllers stay thin, services
own business workflows, repositories own persistence access, DTOs are records,
and configuration properties have metadata.

## Backend configuration

Create:

- `b2b-api/src/main/resources/application.yml`
- `b2b-api/src/main/resources/application-devsec.yml`
- `b2b-api/src/main/resources/b2b-catalog.yml`
- `b2b-api/src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- `b2b-api/src/main/resources/db/migration/V1__product_data_api_init.sql`

Exclude `application-devsec.yml` from packaged jars, as `front-api` does.

Use a dedicated local port, default `8087`.

Catalog defaults in `b2b-catalog.yml`:

```yaml
b2b:
  public-base-url: https://product-data-api.com
  api-base-path: /api/v1
  credits:
    unit-eur: 0.002
    free-grant-credits: 2500
  price:
    freshness-days: 30
  facets:
    product.price:
      path: /api/v1/products/{gtin}/price
      credits: 5
      doc: products/price
      billable-when: fresh-offer
  billing:
    packs:
      starter:
        amount-eur: 20
        credits: 10000
        stripe-price-id: ${B2B_STRIPE_PACK_STARTER_PRICE_ID:}
      growth:
        amount-eur: 100
        credits: 55000
        stripe-price-id: ${B2B_STRIPE_PACK_GROWTH_PRICE_ID:}
      scale:
        amount-eur: 500
        credits: 300000
        stripe-price-id: ${B2B_STRIPE_PACK_SCALE_PRICE_ID:}
    subscriptions:
      starter:
        amount-eur: 20
        monthly-credits: 12000
        rollover-cap-months: 3
        stripe-price-id: ${B2B_STRIPE_SUB_STARTER_PRICE_ID:}
      growth:
        amount-eur: 100
        monthly-credits: 66000
        rollover-cap-months: 3
        stripe-price-id: ${B2B_STRIPE_SUB_GROWTH_PRICE_ID:}
      scale:
        amount-eur: 500
        monthly-credits: 360000
        rollover-cap-months: 3
        stripe-price-id: ${B2B_STRIPE_SUB_SCALE_PRICE_ID:}
```

All public URLs, Stripe IDs, OIDC client IDs/secrets, JWT secret, cookie
settings, allowed origins, Redis, Postgres, and Elasticsearch settings must be
configurable through properties/env vars.

## Public API contract

Implement this endpoint for v1:

```http
GET /api/v1/products/{gtin}/price?language=en|fr
Authorization: Bearer pdapi_...
```

Behavior:

- Validate GTIN before any credit reservation. Reuse
  `org.open4goods.commons.services.BarcodeValidationService`.
- Normalize valid GTINs according to the validation service result.
- Fetch the product with
  `ProductRepository.getByIdWithoutEmbedding(Long productId)`.
- Return `404` when the product is absent. Do not debit credits.
- Return `200` with an empty non-billable price payload when the product exists
  but there is no fresh offer. Do not debit credits.
- Bill only when at least one sanitized offer is newer than
  `b2b.price.freshness-days`, default 30 days.
- Debit `product.price` credits only after billable data is known and durable
  Postgres settlement succeeds.
- Return Problem Detail payloads for all errors.

Use a response envelope for all B2B API responses:

```json
{
  "data": {},
  "meta": {
    "requestId": "req_...",
    "timestamp": "2026-06-02T00:00:00Z",
    "language": "en",
    "creditsConsumed": 5,
    "creditsRemaining": 2495,
    "billable": true,
    "facets": [
      {
        "id": "product.price",
        "credits": 5,
        "served": true,
        "billable": true
      }
    ],
    "freshnessDays": 30,
    "responseTimeMs": 42
  }
}
```

Also include headers:

- `X-Request-Id`
- `X-Credits-Consumed`
- `X-Credits-Remaining`
- `X-Response-Time-Ms`

Document `401`, `402`, and `404` clearly in OpenAPI.

## Sanitized price DTO

Do not expose `front-api` `ProductOffersDto` as-is. Use it only as a mapping
reference.

Create dedicated B2B DTOs. The v1 price response must include:

- `gtin`
- product display name, if available
- brand/model when safely available
- `offersCount`
- `freshOffersCount`
- `bestPrice`
- `bestNewOffer`
- `bestOccasionOffer`
- grouped sanitized offers by condition
- price history/trend summaries when already available without extra heavy work
- source/provenance label per offer
- freshness metadata per offer

Each sanitized offer can include:

- merchant/source label
- title
- URL
- product condition
- amount
- currency
- timestamp
- freshness age in days
- favicon URL if already public-safe

Never expose:

- compensation
- affiliation token
- internal datasource identifiers
- crawler/cache keys
- raw internal scoring/debug fields
- private commercial metadata

Use `@Schema` on every DTO field and record. Add examples for OpenAPI client
generation quality.

## Credit metering and ledger

Implement reservation and settlement as a service-level workflow, not as
controller business logic.

Required flow for a billable-capable request:

1. Resolve API key to organization.
2. Determine maximum potential cost from the facet catalog.
3. Reserve max cost in Redis atomically with Lua. If unavailable or insufficient,
   return `402 Payment Required`.
4. Fetch and map product data.
5. Determine actual billable cost from served, non-empty, fresh data.
6. If actual cost is zero, refund the Redis reservation and return normally.
7. If actual cost is positive, settle synchronously in Postgres:
   - insert an append-only transaction with idempotent `requestId`
   - decrement authoritative buckets/balance
   - fail safely if durable settlement cannot be written
8. Reconcile Redis hot balance from the durable result.
9. Emit a Redis stream/event for dashboards and async analytics.

Do not implement Redis-only ledger persistence.

Postgres model must support:

- `organizations`
- `users`
- `organization_members`
- `api_keys`
- `credit_buckets`
- `credit_transactions`
- `stripe_customers`
- `stripe_checkout_sessions`
- `stripe_subscriptions`
- `stripe_events`
- `invoices`
- `usage_events`
- `admin_audit_events`

Credit bucket rules:

- Free grants, prepaid packs, and manual admin grants do not expire by default.
- Subscription invoice grants create subscription credit buckets.
- Subscription credits roll over while active.
- Retained subscription credits are capped at three monthly grants per plan.
- When applying a subscription grant, expire oldest subscription credits above
  the cap so remaining subscription credits never exceed
  `monthlyCredits * rolloverCapMonths`.
- On cancellation, remaining subscription credits expire 30 days after the
  cancellation time.
- Debits consume expiring buckets first, then non-expiring buckets.
- Every grant, debit, refund, expiration, manual adjustment, and Stripe event
  effect must create an append-only transaction.

## Authentication and authorization

Dashboard/admin auth:

- Implement `POST /api/v1/auth/oidc`.
- Accept provider and ID token.
- Verify Google, Microsoft, GitHub, and Apple using the Infera backend pattern.
- Provision/update user profile on login.
- Create a default organization for first-time users when needed.
- Issue access and refresh JWTs.
- Write HttpOnly session cookies.
- Support bearer JWT for API clients and tests.
- Implement refresh, logout, and `me` endpoints.
- Admin access is computed from configurable admin email allowlist.

Organization RBAC:

- `OWNER`: all permissions, including ownership transfer and destructive org
  actions.
- `ADMIN`: manage members, keys, usage, billing, and grants except ownership
  transfer.
- `DEVELOPER`: create/rotate/revoke own API keys, use playground, read usage.
- `BILLING`: manage payment, invoices, subscriptions, and read balances.

External data API auth:

- API key format: `pdapi_` followed by sufficient random entropy.
- Store only `keyPrefix` and SHA-256 hash.
- Return clear key only from create/rotate responses.
- Reject missing, malformed, unknown, revoked, or disabled keys with `401`.
- Track `lastUsedAt` asynchronously or after successful request processing.
- Cache key hash lookups in Redis with TTL, backed by Postgres.

## Billing

Implement v1 billing in `b2b-api`:

- Stripe Checkout for prepaid packs.
- Stripe Checkout or billing portal flow for subscriptions.
- Stripe webhooks with signature verification.
- Idempotent webhook processing through `stripe_events`.
- Credit grants only after verified successful Stripe events.
- Manual admin grants through admin endpoints.
- Invoice and payment history read APIs.
- Billing catalog read API for frontend pricing pages.
- Subscription cancellation handling and 30-day expiration rule.

Required customer endpoints:

- `GET /api/v1/customer/billing/catalog`
- `POST /api/v1/customer/billing/checkout/pack`
- `POST /api/v1/customer/billing/checkout/subscription`
- `POST /api/v1/customer/billing/portal`
- `GET /api/v1/customer/billing/balance`
- `GET /api/v1/customer/billing/transactions`
- `GET /api/v1/customer/billing/invoices`
- `GET /api/v1/customer/subscriptions`

Required admin endpoints:

- `GET /api/v1/admin/organizations`
- `GET /api/v1/admin/organizations/{organizationId}`
- `GET /api/v1/admin/organizations/{organizationId}/transactions`
- `POST /api/v1/admin/organizations/{organizationId}/credits/grants`
- `GET /api/v1/admin/api-keys`
- `POST /api/v1/admin/api-keys/{apiKeyId}/revoke`
- `GET /api/v1/admin/usage`

## OpenAPI and docs

SpringDoc is the source of truth for the API contract.

Expose:

- Swagger UI
- raw OpenAPI JSON
- Redoc or Scalar UI

Use an API key security scheme named `productDataApiKey` for external data
endpoints and bearer/session auth for dashboard endpoints.

Document every public endpoint with:

- `@Operation`
- `@ApiResponse`
- `@Parameter`
- `@RequestBody` where relevant
- `@SecurityRequirement`
- DTO field-level `@Schema`

Do not manually edit generated OpenAPI clients.

## Frontend module: `b2b-frontend`

Create a new top-level `b2b-frontend/`.

Bootstrap from `/home/goulven/git/infera/apps/frontend`, not from the deprecated
`ui/` project.

Use:

- Nuxt 4
- Vue 3
- Vuetify 4
- TypeScript
- `@nuxtjs/i18n`
- `@nuxtjs/seo`
- `@nuxt/content`
- generated OpenAPI client
- `domains/` mapping layer
- repository-style composables
- server routes for session-backed backend calls

Set i18n:

- strategy: `prefix_except_default`
- default locale: `en`
- English root: `/`
- French root: `/fr/`
- all user-facing strings in JSON locale files
- every page has localized SEO metadata

Public pages:

- `/`: landing page for Product Data API
- `/pricing`
- `/docs`
- `/docs/products/price`
- `/docs/products/price/playground`
- `/docs/products/price/documentation/java`
- `/docs/products/price/documentation/python`
- `/faq`
- `/contact`
- `/legal`
- `/privacy`
- `/terms`

Authenticated customer pages:

- `/auth/login`
- `/dashboard`
- `/dashboard/usage`
- `/dashboard/api-keys`
- `/dashboard/billing`
- `/dashboard/invoices`
- `/dashboard/settings`

Admin pages in the same frontend:

- `/admin`
- `/admin/organizations`
- `/admin/organizations/[organizationId]`
- `/admin/usage`
- `/admin/api-keys`
- `/admin/billing`
- `/admin/audit`

Frontend rules:

- Reuse customer dashboard components in admin views when practical.
- Do not duplicate table, KPI, key-management, billing, or usage widgets.
- API calls go through generated clients and repository/domain mapping.
- Do not expose secrets in browser bundles.
- Playground calls the external data endpoint with a selected API key.
- Playground must show request, response, headers, credits consumed, remaining
  balance, no-data-no-pay states, and error states.
- Pricing page must render YAML-backed catalog values from the backend, not
  hardcoded frontend prices.

## Public documentation content

Use `@nuxt/content` for public docs in `b2b-frontend/content`.

Minimum docs:

- product price API overview
- authentication with `Authorization: Bearer pdapi_...`
- GTIN validation behavior
- no-data-no-pay behavior
- credit billing and freshness rules
- Java quickstart
- Python quickstart
- curl examples
- error codes and Problem Detail examples
- FAQ

All public docs must exist in English and French. English can be the source of
truth, but French pages must not be empty placeholders.

## Repository documentation deliverables

Create or update:

- `b2b-api/AGENTS.md`
- `b2b-api/README.md`
- `b2b-frontend/AGENTS.md`
- `b2b-frontend/README.md`
- `docs/adr/0004-product-data-api-b2b-v1.md`
- `docs/architecture/product-data-api-contract.md`
- `docs/architecture/product-data-api-billing-ledger.md`
- `docs/operations/product-data-api-local-runbook.md`
- `docs/README.md`
- `docs/adr/README.md`

The ADR must record:

- why a separate `b2b-api` and `b2b-frontend` are created
- why v1 is price-facet only
- why Product Data API is the public brand/domain
- why OIDC plus organization API keys is used
- why Postgres is authoritative and Redis is hot-path only
- billing defaults and rollover policy
- sanitized price data policy

The architecture docs must include:

- public API contract
- sanitized DTO policy
- Redis key contract
- Postgres ledger/bucket contract
- Stripe event/idempotency contract
- no-data-no-pay rules
- local run instructions

Update Spring configuration metadata for every new `@ConfigurationProperties`
class.

## Implementation sequence

1. Read repository and reference guides.
2. Add `b2b-api` module and compile an empty app.
3. Add configuration properties, catalog YAML, OpenAPI config, and Problem
   Detail exception handling.
4. Add Flyway migrations, JPA entities, and repositories.
5. Implement OIDC auth, JWT/session cookies, organizations, members, and RBAC.
6. Implement API key create/list/rotate/revoke and Redis lookup cache.
7. Implement credit buckets, Postgres ledger settlement, Redis reservation, and
   reconciliation.
8. Implement the sanitized price facet mapping and public price endpoint.
9. Implement Stripe catalog, checkout, subscriptions, webhooks, invoices, and
   manual admin grants.
10. Add admin/customer REST endpoints.
11. Scaffold `b2b-frontend`.
12. Generate the frontend OpenAPI client from `b2b-api`.
13. Build customer dashboard, API keys, billing, usage, pricing, docs,
   playground, and admin dashboard.
14. Add repo docs, ADR, runbook, and module guides.
15. Run validation commands and fix failures.

## Backend tests

Add focused unit tests for:

- GTIN validation and normalization
- sanitized price DTO mapping
- freshness billing decision
- no-data-no-pay decision
- API key hashing, prefixing, rotation, revocation
- OIDC user provisioning with mocked token verifier
- organization role authorization
- Redis reservation/refund logic
- Postgres bucket debit order
- rollover cap enforcement
- cancellation expiration rule
- Stripe webhook idempotency
- manual admin grant transaction

Add integration tests with Testcontainers for Postgres and Redis:

- unauthenticated price call returns `401`
- malformed/unknown/revoked API key returns `401`
- invalid GTIN returns `400` and consumes zero credits
- missing product returns `404` and consumes zero credits
- product with no fresh offer returns `200`, `billable=false`, zero debit
- product with fresh offer returns `200`, debits exactly 5 credits
- insufficient balance returns `402`
- durable ledger row is written once per billable request
- duplicate Stripe webhook does not duplicate credits
- subscription monthly grant rolls over and enforces the 3-month cap

Mock product repository access for integration tests unless an existing
lightweight Elasticsearch fixture is already available and reliable.

## Frontend tests

Add or update tests for:

- i18n routing `/` and `/fr/`
- localized SEO metadata on public pages
- pricing catalog rendering from backend data
- login/session state
- dashboard balance and usage states
- API key create/rotate/revoke UI
- playground success, 401, 402, and no-data-no-pay states
- admin role gating
- admin manual grant flow
- docs route rendering

Required frontend validation:

```bash
pnpm --dir b2b-frontend lint
pnpm --dir b2b-frontend typecheck
pnpm --dir b2b-frontend test
pnpm --dir b2b-frontend build
```

## Repository validation

At minimum run:

```bash
mvn --offline -pl b2b-api -am test
mvn --offline -pl b2b-api -am install
./scripts/lint.sh
```

If a validation command cannot run because dependencies are missing or services
are unavailable, document the exact command, failure reason, and the smallest
next step needed.

## Acceptance criteria

The implementation is complete only when all of the following are true:

- `b2b-api` compiles in the Maven reactor.
- `b2b-frontend` builds.
- OpenAPI is generated and usable by the frontend.
- Customer can log in with OIDC and land on a dashboard.
- Customer organization receives the free 2500 credit grant once.
- Customer can create an API key and see the clear secret once.
- `GET /api/v1/products/{gtin}/price?language=en` works with
  `Authorization: Bearer pdapi_...`.
- Fresh price data debits 5 credits.
- Invalid GTIN, product not found, and no fresh offer consume zero credits.
- Insufficient credit returns `402`.
- Customer can buy prepaid credits through Stripe Checkout.
- Customer can start a subscription; monthly rollover credits are granted and
  capped at three months.
- Admin can manually grant credits.
- Admin dashboard can inspect organizations, API keys, usage, billing, and
  audit events.
- Public docs and playground are available in English and French.
- Docs, ADR, module guides, config metadata, and runbook are updated.
- Validation commands have been run or their blockers are explicitly recorded.

## Out of scope for v1

Do not implement these in v1 unless they are trivial side effects of reusable
infrastructure:

- full product facet catalog beyond price
- CSV enrichment workflow
- barcode image generation
- product alternatives/KNN facet
- downloadable Java/Python SDK packages published to package repositories
- multi-currency billing
- enterprise invoicing outside Stripe
- separate admin frontend

Prepare the code so these can be added later without redesigning auth, billing,
metering, docs, or frontend structure.

## Handoff requirements

When done, provide:

- concise summary of implemented backend, frontend, billing, and docs changes
- validation commands run and results
- local run commands and URLs
- any known gaps or blocked validations
- any credentials/env vars needed for local development, without revealing
  secrets

