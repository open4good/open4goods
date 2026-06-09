# Product Data API (B2B) - Canonical decisions

> **Authority document.** This file is the single source of truth for every
> decision that the B2B planning docs disagreed on. When any other document
> (including `b2B.md`, `b2b-facets.md`, `b2b-ui.md`, the architecture specs, conflicts
> with this page, **this page wins**. Every spec under `docs/architecture/` and
> `docs/operations/` for the B2B brick links back here.

## 1. Source of truth

- [`b2B.md`](b2B.md) is the **canonical master implementation prompt**. It
  remains useful as an early exploration of the metering flow and facet pricing,
  but every decision it makes that contradicts `b2B.md` is dead.

### Superseded decisions (peaceful-peacock -> canonical)

| Topic | Superseded (peaceful-peacock) | Canonical (b2B.md) |
|---|---|---|
| Dashboard auth | Email/password + BCrypt signup | **OIDC** (Google, Microsoft, GitHub, Apple) + JWT/session cookies |
| Account model | Flat `account` table | **`organizations` + `users` + `organization_members`** (multi-tenant, org owns credits/keys/billing) |
| External API auth header | `X-API-Key: <opaque>` | **`Authorization: Bearer pdapi_...`** |
| Endpoint shape | Root aggregator `GET /api/products/{gtin}?components=` | **Per-facet subresource**: `GET /api/v1/products/{gtin}/price` |
| API base path | `/api/...` | **`/api/v1/...`** |
| Public domain | `b2b.nudger.fr` | **`product-data-api.com`** |
| Brand | nudger B2B | **Product Data API** |
| Credit store | Single Redis/Postgres `credit_balance` | **Postgres `credit_buckets` (authoritative)** + Redis hot mirror; expiring-first debit |
| Catalog file | `b2b-facets.yml` | **`b2b-catalog.yml`** (shape per `b2B.md`) |

The peaceful-peacock metering pattern (reserve-max -> settle-actual -> refund,
Redis Lua, `HardenerBatch`) **survives** and is formalised in
[`../architecture/product-data-api-billing-ledger.md`](../architecture/product-data-api-billing-ledger.md)
and [`../architecture/product-data-api-redis-contract.md`](../architecture/product-data-api-redis-contract.md).

## 2. Locked product decisions (v1)

1. **v1 = price facet only.** `GET /api/v1/products/{gtin}/price`. The catalog,
   metering, docs, dashboard, and playground patterns must generalise to future
   facets (identity, attributes, impact, energy, review) without redesign.
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

## 4. Build & platform invariants (from root `pom.xml` / `AGENTS.md`)

- Java **21**, Spring Boot **4.0.6** (parent `pom.xml`). The
  `front-api/AGENTS.md` "Spring Boot 3" wording is **stale** - ignore it for
  versioning; it is still a good reference for controller/OpenAPI/Javadoc style.
- Backend package root: `org.open4goods.b2bapi`. Default local port **8087**.
- `application-devsec.yml` is excluded from packaged jars (as `front-api` does).

## 5. Finalized Infrastructure & Security Choices (v1)

1. **Cookie Domain Wildcard Configuration**: Session cookies for dashboard authentication will use `SameSite=Lax`, `Secure`, and `Domain=.product-data-api.com` configuration to allow sharing auth state across the frontend (`dashboard.product-data-api.com`) and the API backend (`api.product-data-api.com`).
2. **Distributed Scheduler Locking**: To prevent concurrent execution of `HardenerBatch` across clustered spring boot nodes, the application will use **Redis-based locks** (via ShedLock's Redis provider `shedlock-provider-redis-spring`).
3. **Out-of-Order Stripe Webhook Resolution**: Webhook events such as `invoice.paid` and `checkout.session.completed` are processed idempotently. In cases where webhooks arrive out-of-order, the application will lazily upsert subscription and customer placeholder records in the database.
4. **JWT Security Key Algorithm**: Dashboard tokens are signed using a symmetric shared secret with the **HS256** algorithm, matching the security convention implemented in `front-api`.

## Related

- [`b2B.md`](b2B.md) - canonical master prompt
- [`b2b-facets.md`](b2b-facets.md) - data coverage & facet catalogue
- [`b2b-conccurrence.md`](b2b-conccurrence.md) - competitive study
- [`b2b-ui.md`](b2b-ui.md) - frontend UX spec
- [`b2b-frontend-build.md`](b2b-frontend-build.md) - frontend build & codegen
- Architecture specs under [`../architecture/`](../architecture/) (data model,
  redis contract, billing ledger, API contract, errors, auth, stripe, ops)
- ADR [`0005-product-data-api-b2b-v1`](../adr/0005-product-data-api-b2b-v1.md)

