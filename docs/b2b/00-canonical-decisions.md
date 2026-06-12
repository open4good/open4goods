# Product Data API (B2B) - Canonical decisions

> **Authority document.** This file is the single source of truth for every
> decision that the B2B planning docs disagreed on. When any other document
> (including [`master-prompt.md`](implementation/master-prompt.md),
> [`facet-catalog.md`](product/facet-catalog.md), [`ui-spec.md`](frontend/ui-spec.md),
> or the architecture specs) conflicts with this page, **this page wins**. Every
> spec under `docs/architecture/` and `docs/operations/` for the B2B brick links
> back here. Start at [`README.md`](README.md) for the corpus map.

## 1. Source of truth

- [`implementation/master-prompt.md`](implementation/master-prompt.md) is the
  **canonical master implementation prompt**. The earlier exploration plan
  (`prompt-b2b-on-peaceful-peacock`, a local planning note) remains useful as an
  early study of the metering flow and facet pricing, but every decision it makes
  that contradicts the master prompt is dead.
- The phased execution plan and the living task state live in
  [`implementation/plan.md`](implementation/plan.md) and
  [`implementation/tasks.md`](implementation/tasks.md).

### Superseded decisions (peaceful-peacock -> canonical)

| Topic | Superseded (peaceful-peacock) | Canonical (master prompt) |
|---|---|---|
| Dashboard auth | Email/password + BCrypt signup | **OIDC** (Google, Microsoft, GitHub, Apple) + JWT/session cookies |
| Account model | Flat `account` table | **`organizations` + `users` + `organization_members`** (multi-tenant, org owns credits/keys/billing) |
| External API auth header | `X-API-Key: <opaque>` | **`Authorization: Bearer pdapi_...`** |
| Endpoint shape | Root aggregator `GET /api/products/{gtin}?components=` | **Per-facet subresource**: `GET /api/v1/products/{gtin}/price` |
| API base path | `/api/...` | **`/api/v1/...`** |
| Public domain | `b2b.nudger.fr` | **`product-data-api.com`** |
| Brand | nudger B2B | **Product Data API** |
| Credit store | Single Redis/Postgres `credit_balance` | **Postgres `credit_buckets` (authoritative)** + Redis hot mirror; expiring-first debit |
| Catalog file | `b2b-facets.yml` | **`b2b-catalog.yml`** (shape per the master prompt) |

The peaceful-peacock metering pattern (reserve-max -> settle-actual -> refund,
Redis Lua, `HardenerBatch`) **survives** and is formalised in
[`../architecture/product-data-api-billing-ledger.md`](../architecture/product-data-api-billing-ledger.md)
and [`../architecture/product-data-api-redis-contract.md`](../architecture/product-data-api-redis-contract.md).

## 2. Locked product decisions (v1)

1. **v1 = price facet only.** `GET /api/v1/products/{gtin}/price`. The catalog,
   metering, docs, dashboard, and playground patterns must generalise to future
   facets (identity, attributes, impact, energy, review) without redesign. Each
   shipped facet gets a dedicated spec under [`facets/`](facets/README.md);
   [`facets/product-price.md`](facets/product-price.md) is the canonical example.
2. **GTIN-first strict.** v1 accepts only a syntactically valid, checksum-correct
   GTIN as the product key. `asin` / `mpn` / `merchant_sku` / `keyword`
   resolution is **out of scope** for v1 (future facet/endpoint).
3. **No data, no pay.** Invalid GTIN, product not found, and empty/stale
   price-only data consume **zero** credits. Billing happens only after durable
   Postgres settlement of fresh, served data.
4. **`meta.coverage`** is added to the response envelope: a per-facet object
   declaring whether the requested facet is *covered* for this product, so
   clients see premium-facet availability honestly (relevant once premium facets
   ship; for v1 it always reports `product.price`). See
   [`../architecture/product-data-api-contract.md`](../architecture/product-data-api-contract.md).
5. **Full Stripe billing in v1**: prepaid packs + subscriptions + webhooks +
   manual admin grants. There is **no reference implementation** in Infera, so it
   is specified from scratch in
   [`../architecture/product-data-api-stripe-contract.md`](../architecture/product-data-api-stripe-contract.md).

## 3. Documented open questions (NOT resolved here)

These are deliberately deferred. Do not silently pick a default; raise them
before implementing the affected surface.

- **Review-facet exposure depth** (future): full multi-level AI review vs
  summary + sources only, to bound editorial liability.
- **Live `/status` page**: deferred; do not block v1 on uptime infrastructure.
- **Public price comparator** ("our EUR/call vs PriceAPI/SerpApi") on the
  pricing landing: marketing decision, deferred.
- **Production deployment**: only the local runbook exists
  ([`../operations/product-data-api-local-runbook.md`](../operations/product-data-api-local-runbook.md)).
  Hosting, DNS, TLS, CI/CD deploy pipeline, and managed Postgres/Redis for
  `product-data-api.com` are unspecified; decide before launch (not before code).
- **GDPR / PII**: the platform stores user emails, OIDC profiles, and Stripe
  customer data. Data-retention policy, DPA wording for `/privacy` and `/terms`,
  and deletion workflows are unspecified; decide before public launch.

## 4. Build & platform invariants (from root `pom.xml` / `AGENTS.md`)

- Java **21**, Spring Boot **4.0.6** (parent `pom.xml`). The
  `front-api/AGENTS.md` "Spring Boot 3" wording is **stale** - ignore it for
  versioning; it is still a good reference for controller/OpenAPI/Javadoc style.
- Backend package root: `org.open4goods.b2bapi`. Default local port **8087**.
- `application-devsec.yml` is excluded from packaged jars (as `front-api` does).

## 5. Finalized Infrastructure & Security Choices (v1)

1. **Single frontend domain.** One Nuxt site serves all public, dashboard
   (`/dashboard`), and admin (`/admin`) routes at **`product-data-api.com`**; the
   backend is exposed at **`api.product-data-api.com`**. There is **no**
   `dashboard.` subdomain (earlier wording mentioning one is superseded). Session
   cookies use `SameSite=Lax`, `Secure`, and `Domain=.product-data-api.com` so the
   browser can call the API subdomain with `credentials: 'include'`.
2. **Distributed Scheduler Locking**: To prevent concurrent execution of `HardenerBatch` across clustered spring boot nodes, the application will use **Redis-based locks** (via ShedLock's Redis provider `shedlock-provider-redis-spring`).
3. **Out-of-Order Stripe Webhook Resolution**: Webhook events such as `invoice.paid` and `checkout.session.completed` are processed idempotently. In cases where webhooks arrive out-of-order, the application will lazily upsert subscription and customer placeholder records in the database.
4. **JWT Security Key Algorithm**: Dashboard tokens are signed using a symmetric shared secret with the **HS256** algorithm, matching the security convention implemented in `front-api`.
5. **Reservation crash recovery is reconciliation-based.** Redis reservations are
   plain `DECRBY` against the org's hot balance mirror; there is **no per-request
   reservation TTL** (the shared balance key cannot expire individual
   reservations). Recovery from a crash between reserve and settle is: refund in
   `finally`/`afterCompletion`, post-settlement `reconcile` from Postgres, and the
   periodic `HardenerBatch` reconciliation. See the
   [redis contract](../architecture/product-data-api-redis-contract.md).
6. **DEBIT idempotency index is composite.** A billable request may debit several
   buckets (one ledger row per bucket touched, sharing `request_id`), so the
   partial unique index is `UNIQUE(request_id, bucket_id) WHERE type='DEBIT'`,
   combined with an `exists`-on-`request_id` pre-check inside the settlement
   transaction as the request-level guard. See the
   [data model](../architecture/product-data-api-data-model.md) and
   [billing ledger](../architecture/product-data-api-billing-ledger.md) specs.
7. **Request id prefix is `pdreq_`** (e.g. `pdreq_01HF...`), everywhere: response
   envelope `meta.requestId`, `X-Request-Id` header, Problem Details, ledger
   `request_id`, logs.

## Related

- [`README.md`](README.md) - corpus map and reading order
- [`implementation/master-prompt.md`](implementation/master-prompt.md) - canonical master prompt
- [`implementation/plan.md`](implementation/plan.md) / [`implementation/tasks.md`](implementation/tasks.md) - phased plan & living task state
- [`facets/README.md`](facets/README.md) - facet lifecycle, template, authoring prompt
- [`product/facet-catalog.md`](product/facet-catalog.md) - prioritised facet catalogue & credit tiers
- [`business/data-coverage.md`](business/data-coverage.md) - measured data coverage (+ ES queries)
- [`business/competition.md`](business/competition.md) - competitive study
- [`frontend/ui-spec.md`](frontend/ui-spec.md) - frontend UX spec
- [`frontend/build.md`](frontend/build.md) - frontend build & codegen
- Architecture specs under [`../architecture/`](../architecture/) (data model,
  redis contract, billing ledger, API contract, errors, auth, stripe, ops)
- ADR [`0005-product-data-api-b2b-v1`](../adr/0005-product-data-api-b2b-v1.md)
