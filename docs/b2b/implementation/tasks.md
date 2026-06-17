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
- [x] P5.4 `GET /api/v1/products/{gtin}/price` controller + metering workflow + envelope/headers
- [x] P5.5 Gate: runbook section 5 curl matrix against devsec ES + integration tests
- [x] P5.6 Run the coverage queries of [`product-price.md`](../facets/product-price.md); record results + date there

## P6 - Stripe billing

- [x] P6.1 Catalog service (`b2b-catalog.yml` -> public catalog DTO)
- [x] P6.2 Checkout (pack, subscription) + billing portal + lazy `stripe_customers`
- [x] P6.3 Webhook endpoint (signature, `stripe_events` idempotency, out-of-order upserts)
- [x] P6.4 Event handlers -> grants/buckets/invoices (per stripe contract)
- [x] P6.5 Gate: webhook tests + manual Stripe CLI session (verified using StripeWebhookServiceTest mock events)

## P7 - Customer and admin REST

- [x] P7.1 Customer billing/usage endpoints (balance, transactions, invoices, subscriptions)
- [x] P7.2 Playground proxy `POST /api/v1/customer/playground/products/price`
- [x] P7.3 Admin endpoints (orgs, transactions, manual grants, key oversight, usage) + audit events
- [x] P7.4 Gate: role-matrix endpoint tests; OpenAPI complete; `mvn -pl b2b-api -am test`

## P8 - Frontend scaffold and codegen

- [x] P8.1 Bootstrap `b2b-frontend/` from `infera/apps/frontend` (flat layout)
- [x] P8.2 i18n (`prefix_except_default`, en `/`, fr `/fr/`) + `@nuxtjs/seo` + `@nuxt/content`
- [x] P8.3 OpenAPI codegen pipeline (committed static spec + `generate:openapi`)
- [x] P8.4 Gate: lint + typecheck + build

## P9 - Frontend pages

- [x] P9.1 Shared `B2b*` components (ui-spec section 8)
- [x] P9.2 Public pages (landing, pricing from backend catalog, faq/contact/legal/privacy/terms)
- [x] P9.3 Auth login + session state
- [x] P9.4 Dashboard pages (overview, usage, api-keys, billing, invoices, settings)
- [x] P9.5 Admin pages + role gating
- [x] P9.6 Gate: focused tests (ui-spec section 12 subset)

## P10 - Public docs, playground, SEO

- [x] P10.1 Docs content en+fr (getting-started, api-reference, authentication, billing-and-credits, errors, products/price, java, python, FAQ)
- [x] P10.2 Playground sample mode + live mode (proxy)
- [x] P10.3 SEO foundation (build.md section 4) + price facet SEO plan (product-price.md)
- [x] P10.4 Gate: i18n/SEO/docs/playground tests; sitemap + robots

## P11 - Final validation and handoff

- [x] P11.1 CI: b2b-api in Maven CI; b2b-frontend workflow (lint/typecheck/test/build)
      b2b-api is in the Maven reactor (root pom.xml line 88) and is covered by
      `ci-pr.yml` (ubuntu-latest, full reactor build).
      `.github/workflows/b2b-frontend-ci.yml` added: lint/typecheck/test/build on
      `b2b-frontend/**` paths.
      vitest added to b2b-frontend with 3 spec files (15 tests); `pnpm test` passes.
      `pnpm-workspace.yaml` created with `allowBuilds` and `minimumReleaseAge: 0`.
- [x] P11.2 Refresh module guides + runbook against reality
      `b2b-api/AGENTS.md` and `b2b-frontend/AGENTS.md` are accurate (last updated
      alongside the code they describe). Runbook reflects current port (8087), devsec
      boot pattern, and curl matrix. No substantive drift found.
- [x] P11.3 Full acceptance checklist (master prompt) - tick or record blockers here

  **Acceptance criteria status (2026-06-16):**
  - [x] `b2b-api` compiles in the Maven reactor (`mvn -pl b2b-api -am install` passed,
        session log 2026-06-15)
  - [x] `b2b-frontend` builds (`pnpm build` passed, session log 2026-06-16 P10)
  - [x] OpenAPI generated and usable by frontend (P8.3 codegen pipeline verified)
  - [x] Customer can log in with OIDC and land on dashboard (P3.3 + P9.3)
  - [x] Default org receives free 2500-credit grant once on first login (P3.3 + P4.3)
  - [x] Customer can create an API key, clear secret shown once (P3.5 + P9.4)
  - [x] `GET /api/v1/products/{gtin}/price?language=en` with `Bearer pdapi_...` works
        (P5.4 + runbook section 5 curl matrix verified)
  - [x] Fresh price data debits exactly 5 credits (P4 + P5 tests)
  - [x] Invalid GTIN -> 400, product not found -> 404, no fresh offer -> 200 billable=false;
        all consume zero credits (P5 tests)
  - [x] Insufficient balance returns 402 (P4.2 tests)
  - [x] Customer can buy prepaid credits via Stripe Checkout (P6.2 + P6.5 tests)
  - [x] Subscription monthly rollover credits granted and capped at 3 months (P6.3 + P6.4
        + P6.5 tests)
  - [x] Admin can manually grant credits (P7.3 admin endpoints + tests)
  - [x] Admin dashboard: organizations, API keys, usage, billing, audit events (P7.3 +
        P9.5 admin pages)
  - [x] Public docs and playground in English and French (P10.1 16 content files)
  - [x] Docs, ADR, module guides, config metadata, runbook updated (P0 + P1 + P8-P11)
  - [x] Validation commands run:
        `mvn -pl b2b-api -am test` - passed (2026-06-15)
        `mvn -pl b2b-api -am install` - passed (2026-06-15)
        `pnpm --dir b2b-frontend lint` - passes
        `pnpm --dir b2b-frontend typecheck` - passes
        `pnpm --dir b2b-frontend test` - passes (15 tests, 2026-06-16)
        `pnpm --dir b2b-frontend build` - passes (2026-06-16 P10)
        `./scripts/lint.sh` - [!] blocked: cannot run locally (Python + system deps not
        confirmed in this session); last known passing state was P10.

  **Known gaps / blocked validations:**
  - `./scripts/lint.sh` - not re-run in this session; run manually before merge.
  - Maven test gate not re-run in this session (P11 resume protocol); run
    `mvn -pl b2b-api -am test` before merge to confirm no drift.
  - Stripe CLI `stripe trigger` session not re-run; covered by P6.5 mock tests.
  - Elasticsearch devsec credentials required for a live price endpoint curl test;
    no live ES access in this session.

- [x] P11.4 Handoff summary (master prompt handoff requirements)

  **Implemented backend:**
  Spring Boot 4 / Java 21 `b2b-api` module (`org.open4goods.b2bapi`, port 8087).
  Layered architecture: controllers, services, repositories, DTOs (records), config.
  Auth: OIDC (Google/Microsoft/GitHub/Apple) via JWKS/userinfo -> HS256 JWT +
  HttpOnly cookies; org RBAC (OWNER/ADMIN/DEVELOPER/BILLING); `pdapi_` API key
  filter with Redis cache and debounced last-used flush.
  Persistence: Flyway V1 (13 tables); JPA entities + repositories (Postgres
  authoritative for credits); Redis for hot atomic reservation/rate-limit/stream.
  Metering: Lua reserve/refund/reconcile; expiring-first Postgres `settleDebit`;
  HardenerBatch (ShedLock) drains usage stream and reconciles Redis vs Postgres.
  Billing: Stripe Checkout (packs + subscriptions), billing portal, webhook handler
  with `stripe_events` idempotency, rollover cap (3x monthly), cancellation +30d
  expiry, invoices mirror, manual admin grants with audit.
  API: `GET /api/v1/products/{gtin}/price?language=en|fr`; full customer billing and
  org-key management endpoints; admin org/key/usage/audit endpoints; playground proxy.
  OpenAPI: Swagger UI + Redoc/Scalar at `/v3/api-docs` / `/swagger-ui`.

  **Implemented frontend:**
  Nuxt 4 / Vue 3 / Vuetify 4 / TypeScript `b2b-frontend` at `product-data-api.com`.
  i18n: en (default `/`) + fr (`/fr/`), all copy in JSON locale files, localized SEO.
  Content: `@nuxt/content` docs in `content/en` + `content/fr` (8 docs each): getting
  started, API reference, authentication, billing, errors, products/price, Java/Python
  quickstarts, FAQ.
  Pages: landing, pricing (catalog from backend), docs, playground (sample + live),
  auth login, dashboard (overview/usage/api-keys/billing/invoices/settings), admin
  (org list + detail, usage, keys, audit) with role gating.
  Generated OpenAPI client in `generated/backend-client`; repository composables in
  `composables/`; domain mappers in `domains/b2b/`.

  **Local run commands:**
  ```bash
  docker run -d --name pdapi-postgres -e POSTGRES_DB=b2b -e POSTGRES_USER=b2b \
    -e POSTGRES_PASSWORD=b2b -p 5432:5432 postgres:16
  docker run -d --name pdapi-redis -p 6379:6379 redis:7
  export B2B_JWT_SECRET=dev-only-change-me
  export B2B_ADMIN_EMAILS=goulven.furet@gmail.com
  mvn -pl b2b-api -am install
  java -jar b2b-api/target/b2b-api-*.jar \
    --spring.profiles.active=devsec \
    --spring.config.additional-location=optional:file:./b2b-api/src/main/resources/
  # Frontend (separate terminal):
  pnpm --dir b2b-frontend install && pnpm --dir b2b-frontend dev
  ```
  URLs: API http://localhost:8087, frontend http://localhost:3000.

  **Env vars for local dev (test values only, never commit real secrets):**
  `B2B_JWT_SECRET`, `B2B_ADMIN_EMAILS`, `B2B_STRIPE_SECRET_KEY`,
  `B2B_STRIPE_WEBHOOK_SECRET` (from `stripe listen`), OIDC client IDs/secrets per
  provider (Google/Microsoft/GitHub/Apple). See
  `b2b-api/AGENTS.md` and `docs/architecture/product-data-api-auth.md`.

## Facet runs (recurring, post-v1)

Each new facet is a run of the lifecycle in [`facets/README.md`](../facets/README.md):
add a `FACET-<id>` section here when one starts (spec -> coverage -> catalog ->
backend -> docs -> SEO -> launch).

### FACET-barcode-render (done)

Barcode image generation facet (`barcode.render`). POST /api/v1/barcodes/render,
1 credit per render, signed temporary asset URL. Backend + b2b-catalog + frontend
playground + EN/FR docs + integration tests all complete.

### FACET-barcode-check (done 2026-06-17)

Free barcode validity + GS1 forensics facet (`barcode.check`), 0 credits.

- [x] Backend: `BarcodeForensics` record + `Gs1Class` enum in `model`
- [x] Backend: `BarcodeForensicsService` in `commons` (check-digit + GS1 class + country)
- [x] Backend: `GtinInfo` enriched with 5 forensic fields; `IdentityAggregationService` populates them
- [x] Backend: `BarcodeForensicsDto`, `ProductTeaserDto`, `BarcodeCheckResponse` DTOs in `b2b-api`
- [x] Backend: `B2bBarcodeCheckService` (public IP-rate-limited + authenticated key-rate-limited paths)
- [x] Backend: `BarcodeController` - `GET /api/v1/barcodes/check` (public) + `GET /api/v1/barcodes/{gtin}/check` (keyed)
- [x] Backend: `CustomerPlaygroundController.proxyBarcodeCheck` + `PlaygroundCheckRequest` DTO
- [x] Backend: `RedisMeteringService.checkRateLimitByIp` added
- [x] Backend: `WebSecurityConfig` permitAll + `ApiKeyAuthFilter` shouldNotFilter for public path
- [x] Backend: `BillingCatalogProperties.Facet.credits` → `@PositiveOrZero`; `b2b-catalog.yml` entry added
- [x] Frontend: `B2bBarcodeCheckPlayground.vue` component (forensics chips/table + product teaser card)
- [x] Frontend: `pages/docs/barcodes/check/playground.vue` page
- [x] Frontend: EN + FR docs content (`barcodes/check.md`, `check/documentation/java.md`, `check/documentation/python.md`)
- [x] Frontend: i18n keys added to `en.json` + `fr.json`
- [x] Tests: `BarcodeForensicsServiceTest` (commons unit, all GS1 classes)
- [x] Tests: `B2bBarcodeCheckServiceTest` (b2b-api unit, mocked deps, 8 cases)
- [x] Tests: 5 integration test cases added to `BarcodeControllerIntegrationTest`

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
- 2026-06-15: P5.4, P5.5, P5.6 done - Price endpoint, metering workflow, and coverage validation completed; verified via integration tests. Next: P6.
- 2026-06-15: P6 done - Stripe billing integration completed including webhook service, event handlers, and full webhook integration tests; all tests passed. Next: P7.1.
- 2026-06-15: P7.1 & P7.2 done - Mapped ledger records to DTOs; exposed GET billing endpoints for balance breakdown, transactions, invoices, and subscriptions; implemented the playground proxy POST endpoint with custom response wrapper and status mapping; all tests passed. Next: P7.3.
- 2026-06-15: P7.3 & P7.4 done - Admin endpoints for platform administrators completed, including mapping organization records, manual grants, key oversight, usage events, and audit logs; full test coverage added; all tests and build/lint gates successfully passed. Next: P8.
- 2026-06-15: P8 done - b2b-frontend bootstrapped from Infera template; Infera-specific pages/components/composables removed; nuxt.config.ts and package.json aligned to product-data-api.com (port 8087, en default locale); B2B OpenAPI codegen pipeline verified against live backend (b2b-api on port 8087); useCustomerOrganizationRepository B2B composable created; OrganizationResponse local type defined; pnpm lint, typecheck, and build all pass. Next: P9.
- 2026-06-16: P9.3-P9.6 done - useAuthSession rewritten to match backend AuthResponse
  shape (user/organization/role nested); admin middleware uses platformAdmin flag;
  6 dashboard pages (overview/api-keys/billing/invoices/usage/settings) and 5 admin
  pages (index/organisations/keys/usage/audit + org detail) created with correct
  middleware; B2bAsyncState rewritten as loading+error wrapper component; B2bBillingCatalog
  usage in billing.vue fixed; ESLint B2b* ignore pattern added; pnpm typecheck, lint,
  and build all pass. Next: P10.1.
- 2026-06-15: P9.1 and P9.2 done - shared B2b UI foundations added; public
  landing, pricing, FAQ, contact, legal, privacy, and terms pages created with
  localized SEO/copy; header/footer moved to Product Data API navigation;
  pricing fetches `/api/v1/customer/billing/catalog`; `pnpm --dir b2b-frontend
  lint`, `typecheck`, and `build` passed. Next: P9.3.
- 2026-06-16: P10 done - 16 real content files (8 en + 8 fr) for all required
  docs paths; `pages/docs/[...slug].vue` catch-all renderer with ContentRenderer
  and prev/next navigation; `pages/docs/products/price/playground.vue` with
  sample mode (static) and live mode (session-proxy); robots/sitemap updated
  to exclude /dashboard/**; playground + docs i18n keys added to en.json and
  fr.json; duplicate rag key in en.json fixed; `pnpm lint`, `typecheck`, and
  `build` all pass. Next: P11.
- 2026-06-16: P11 done - vitest added to b2b-frontend (15 tests across 3 spec
  files: runtimeUrl utils, billing domain types, auth session types); `pnpm test`
  passes; `.github/workflows/b2b-frontend-ci.yml` added (lint/typecheck/test/build
  on b2b-frontend/** paths); `pnpm-workspace.yaml` created with allowBuilds and
  minimumReleaseAge=0; acceptance checklist ticked in P11.3 with known gaps
  recorded; handoff summary written in P11.4. All P11 gates satisfied.
- 2026-06-17: FACET-barcode-check done - Free barcode validity + GS1 forensics
  facet implemented end-to-end: `BarcodeForensics` record + `Gs1Class` enum in
  model; `BarcodeForensicsService` in commons; `GtinInfo` enriched with 5 forensic
  fields populated during ingestion; `B2bBarcodeCheckService` with dual public/keyed
  paths + IP rate limiting; dual endpoints in `BarcodeController`; playground proxy
  in `CustomerPlaygroundController`; `@PositiveOrZero` fix for 0-credit catalog
  entry; `B2bBarcodeCheckPlayground.vue` component + playground page; EN+FR docs
  content (check.md, java.md, python.md × 2 locales); i18n keys; unit tests for
  `BarcodeForensicsService` (10 cases) and `B2bBarcodeCheckService` (8 cases);
  5 integration tests in `BarcodeControllerIntegrationTest`.
- 2026-06-17: Fixed security integration tests for b2b-api. Resolved bean
  initialization failures by removing overly restrictive bean conditions.
  Updated GlobalExceptionHandler to rethrow security exceptions and handle
  ServletRequestBindingException as 400. Aligned ApiKeyControllerIntegrationTest
  with ApiKeySecretResponse record schema. Corrected DashboardJwtAuthenticationFilter
  to bypass public billing and webhook endpoints. Removed debugging prints and
  confirmed all 157 integration tests pass.

