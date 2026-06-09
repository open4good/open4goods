# ADR 0005 - Product Data API (B2B) v1

**Status**: Accepted
**Date**: 2026-06-09
**Context**: Productising the nudger product dataset as a freemium B2B API

---

## Context

open4goods/nudger.fr owns a large, differentiated Elasticsearch product index:
~33.9M GTINs with multi-merchant offers, plus a curated premium subset
(~45-50K products) carrying data no competitor has - ImpactScore(c),
EU energy labels (EPREL), repairability index, sourced AI reviews, and
multi-taxonomy referentials (Google/EPREL/ICECAT/ETIM/Wikidata). The competitive
study ([`../b2b/b2b-conccurrence.md`](../b2b/b2b-conccurrence.md)) shows the
"price data API" market (PriceAPI, SerpApi, Bright Data, Winamaz...) sells
commodity scraped data at ~30 $/1000 requests and none of them carry the
proprietary nudger data.

We want to monetise this via a self-serve B2B API with a contractual edge
("No data, no pay"), starting with the commodity price facet as a loyal loss
leader and reserving margin for proprietary facets later.

The decision set was split across `b2B.md` (newer) and an older planning note;
this ADR ratifies the canonical decisions captured in
[`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).

---

## Decision

1. **Two new modules in the monorepo.** `b2b-api` (Spring Boot 4 / Java 21,
   `org.open4goods.b2bapi`) and `b2b-frontend` (Nuxt 4 / Vuetify 4, bootstrapped
   from `infera/apps/frontend`). They are separate from `front-api`/`frontend`
   because the audience (paying developers/businesses), auth model (OIDC +
   organizations + metered API keys), billing, and public brand
   (`product-data-api.com`) are distinct, and we do not want B2B billing/metering
   risk in the consumer site's deployment.

2. **v1 is price-facet only.** One billable endpoint
   `GET /api/v1/products/{gtin}/price`. The facet catalogue, metering, docs,
   dashboard, and playground are built as reusable patterns so future facets
   (identity, attributes, impact, energy, review, taxonomy) add a YAML entry +
   mapping + doc page without redesigning auth/billing/metering.

3. **Public brand & domain = "Product Data API" / `product-data-api.com`**,
   English default at `/`, French mirror at `/fr/`. This keeps the grey-area
   sourcing discourse off the nudger consumer brand and targets an
   international developer audience.

4. **OIDC dashboard auth + organization-scoped API keys.** Dashboard/admin users
   authenticate via OIDC (Google, Microsoft, GitHub, Apple) with JWT + HttpOnly
   session cookies. Machine clients use opaque `pdapi_` keys
   (`Authorization: Bearer`). Organizations own credits, keys, billing, members,
   and usage; roles are `OWNER`, `ADMIN`, `DEVELOPER`, `BILLING`. Chosen over a
   flat email/password account model for multi-tenant B2B reality and because
   Infera already provides reusable OIDC verifier services.

5. **Postgres is authoritative; Redis is hot-path only.** Postgres holds the
   credit buckets and the append-only transaction ledger. Redis holds atomic
   reservation counters, the API-key lookup cache, and a usage event stream, and
   is reconciled from Postgres. Redis is **never** the ledger of record. This
   gives correct, auditable billing under multi-instance concurrency.

6. **Billing defaults & rollover.** Credits priced at ~0.002 EUR. Free grant
   2500 credits, once per organization. Prepaid packs and manual admin grants do
   not expire. Subscription invoice grants create subscription buckets that roll
   over while active, capped at `monthlyCredits * rolloverCapMonths` (3); on
   cancellation, remaining subscription credits expire 30 days later. Debits
   consume expiring buckets first, then non-expiring. Stripe (packs +
   subscriptions + webhooks) ships in v1, specified from scratch.

7. **No data, no pay + GTIN-first strict.** Credits are debited only for fresh,
   served, non-empty data of the requested facet, after durable settlement.
   Invalid GTIN / 404 / no-fresh-offer cost zero. v1 accepts only valid GTINs.

8. **Sanitized price data only.** A dedicated B2B DTO exposes offers, prices,
   provenance, and freshness. It **never** exposes `compensation`,
   `affiliationToken`, internal datasource identifiers, crawler/cache keys, or
   internal scoring/debug fields.

---

## Consequences

- **Reuse**: `BarcodeValidationService` (GTIN), `ProductRepository.getByIdWithoutEmbedding`,
  the offer-mapping logic of `front-api` `ProductMappingService`, the `front-api`
  controller/OpenAPI/security style, and Infera's OIDC/JWT/ApiKey services and
  Nuxt frontend stack are reused to keep v1 small.
- **New infrastructure**: the B2B brick introduces Postgres + Redis + Stripe to
  the platform's operational surface. A local runbook and an ops/observability
  spec are required (see Related).
- **Billing correctness is the main risk.** The Redis-reservation /
  Postgres-bucket reconciliation and idempotent settlement are specified
  explicitly and must be covered by Testcontainers integration tests.
- **Stripe has no in-house precedent** - the webhook/idempotency/grant-mapping
  contract is authored from zero and must be validated with the Stripe CLI.

---

## Related

- [Canonical decisions](../b2b/00-canonical-decisions.md)
- [Data model & DDL](../architecture/product-data-api-data-model.md)
- [Redis key contract](../architecture/product-data-api-redis-contract.md)
- [Billing ledger & bucket algorithm](../architecture/product-data-api-billing-ledger.md)
- [API contract & sanitized DTO](../architecture/product-data-api-contract.md)
- [Error catalog](../architecture/product-data-api-errors.md)
- [Auth & OIDC](../architecture/product-data-api-auth.md)
- [Stripe billing contract](../architecture/product-data-api-stripe-contract.md)
- [Rate limiting & observability](../architecture/product-data-api-ops.md)
- [Local runbook](../operations/product-data-api-local-runbook.md)
