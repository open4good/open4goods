---
title: "Facet spec - `product.price-history`"
normative: false
audience: PROJECT_SCOPED
---

# Facet spec - `product.price-history`

> Status: IN BUILD (GOU-28)
> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md) ·
> Lifecycle: [`README.md`](README.md) · Catalogue:
> [`facet-catalog.md`](../product/facet-catalog.md) · Common contract:
> [API contract](../../architecture/product-data-api-contract.md)
>
> Rights and sourcing decisions for this facet (public provider labels,
> per-source B2B redistribution) were recorded on [GOU-28](/GOU/issues/GOU-28)
> and its rights dossier, not re-litigated here.

## 1. Overview and value proposition

Multi-provider historical price data for a GTIN: daily rollups over up to five
years, or sparse per-change events over the last 31 days, keyed by public
provider label, condition, and currency. This is the persistence tier that
`product.price`'s `newHistorySummary`/`occasionHistorySummary` fields only
summarize - `product.price-history` exposes the full time series behind that
summary, for merchants building trend charts, repricing tools, or historical
market analysis.

## 2. Sourcing and coverage

Only sources reviewed and explicitly authorized for `PRICE` content on
`B2B_API` in `SourceUsagePolicyRegistry` are ever served - deny-by-default.
As of this facet's rights decision (GOU-28), `merchant-feed` and
`legacy-product-backup` are authorized; `amazon-paapi-quarantine` remains
explicitly out of scope. A provider whose usage policy is later revoked or
whose public label is unreviewed disappears from responses immediately - no
migration or backfill is needed, since the redaction happens at read time
(section 5).

Coverage is a strict subset of `product.price`'s ~33.9M GTINs: only products
with at least one observation from an authorized source, over the requested
window, are served. Re-measure at launch using the same method as
[`product-price.md`](product-price.md) section 2, scoped to the
`o4g-daily-provider-rollup-*` / `o4g-price-change-*` data streams instead of
the Product index (this facet's query plan never touches the Product index -
GOU-28 AC8).

## 3. Endpoint and credits

- Endpoint: `GET /api/v1/products/{gtin}/price/history`
- Credits: **8** (L tier, [`facet-catalog.md`](../product/facet-catalog.md)
  section 3) - priced above the baseline `price` facet for the cost of
  persisting and serving a time series, below the exclusive `impact`/`taxonomy`
  tier.
- Billable when: `has-history` - at least one policy-allowed point is served.

Catalog entry:

```yaml
b2b:
  facets:
    product.price-history:
      path: /api/v1/products/{gtin}/price/history
      credits: 8
      doc: products/price-history
      billable-when: has-history
```

### Query parameters

| Parameter | In | Type | Default | Notes |
|---|---|---|---|---|
| `gtin` | path | string | required | GTIN-8/12/13/14 |
| `language` | query | string | `en` | localized display fields only |
| `from` | query | ISO-8601 instant | `to` minus 30 days | inclusive, UTC |
| `to` | query | ISO-8601 instant | now | exclusive, UTC (half-open range) |
| `granularity` | query | `DAY` \| `CHANGE` | `DAY` | see section 3.1 |
| `provider` | query | string | none | reviewed public provider selector, never an internal source id |
| `condition` | query | `NEW` \| `OCCASION` \| `UNKNOWN` | none | |
| `currency` | query | ISO 4217 code | none | |
| `limit` | query | integer | 100 | bounded `PriceHistoryQuery.MIN_PAGE_SIZE`..`MAX_PAGE_SIZE` (1..500, see below) |
| `cursor` | query | opaque string | none | continuation from a previous page's `nextCursor`; never construct one by hand |

### 3.1 Granularity

| Granularity | Max window | Source | Point shape |
|---|---|---|---|
| `DAY` | 5 years | `o4g-daily-provider-rollup-*` (daily rollups) | min/max/close amount + offer count |
| `CHANGE` | 31 days | `o4g-price-change-*` (sparse change events) | time, amount, availability state |

An out-of-range window for the requested granularity (or an unparseable
`from`/`to`, an inverted range, or a future-only range) returns a `400`
`invalid-date-range` Problem Detail, matching
[`product-data-api-errors.md`](../../architecture/product-data-api-errors.md).

### 3.2 `limit` bound (GOU-100)

The public bound is **1..500**, not 1..1000 as originally drafted in this
issue's acceptance criteria - `PriceHistoryQuery.MAX_PAGE_SIZE` in
`org.open4goods.pricehistory.model` is the single source of truth, and this
page, the OpenAPI schema, generated clients, and the playground all read from
that constant rather than restating either number. See
[GOU-100](/GOU/issues/GOU-100) for the arbitration record.

### 3.3 Cursor

The `cursor` a client passes back must be exactly the `nextCursor` value a
previous page of this same endpoint returned - it is opaque by contract, not
just by convention. Internally it is AES-256-GCM authenticate-encrypted
(server-only key) over the raw Elasticsearch resume position plus a
fingerprint of every other query parameter, so:

- internal provider/offer coordinates are never recoverable from it (AC4);
- replaying it against different filters, or a forged/corrupted value, always
  fails authentication and returns a `400` `cursor-mismatch` - never a `500`,
  regardless of what a client sends (AC2).

## 4. Sanitization (allow-list)

The `data` shape (`B2bPriceHistoryDto`): `gtin`, effective `from`/`to`,
effective `granularity`, `series[]`, `nextCursor`. Each series
(`B2bPriceHistorySeriesDto`) carries `provider` (public label only),
`condition`, `currency`, and either `dayPoints[]` or `changePoints[]`
depending on `granularity`.

| Hidden | Why |
|---|---|
| internal `provider_id` / source id | only the reviewed public label from `PublicProviderLabelRegistry` is exposed, matching `product.price` |
| `provider_offer_id`, internal `event_id` | never leave the read adapter; not present in the DTO, and unrecoverable from the opaque cursor |
| affiliation tokens, compensation | not modeled in the price-history time series at all |
| `policy_ref`, `content_hash`, crawler/cache keys | internal usage-policy/debug metadata, dropped in the adapter's domain mapping |

A denied source-usage policy, or a provider with no reviewed public label,
removes that series from the response entirely (AC4) - it is never returned
redacted-in-place, and its removal is decided **before** billability (AC5),
so an all-denied result is a `no-price-history` no-pay case, not a partial
paid one.

## 5. No-data-no-pay matrix

| Condition | HTTP | `billable` | no-pay reason |
|---|---:|---|---|
| invalid GTIN (checksum/format) | 400 | - | `invalid-gtin` (rejected before reserve) |
| invalid `from`/`to`/`granularity`/`limit`/cursor mismatch | 400 | - | `invalid-parameter` / `invalid-date-range` / `cursor-mismatch` (rejected before reserve) |
| valid GTIN, product absent from index | 404 | - | `not-found` |
| product exists, no point from an authorized source in range | 200 | false | `no-price-history` |
| all candidate points denied by source-usage policy or missing public label | 200 | false | `no-price-history` |
| at least one policy-allowed point served | 200 | true | - (debit 8) |

`meta.coverage` reports `product.price-history` as covered for every
existing product, consistent with the covered-but-not-served envelope used by
`product.price`.

## 6. Docs pages

| Page | en | fr |
|---|---|---|
| Facet reference | `/docs/products/price-history` | `/fr/docs/products/price-history` |
| Playground | `/docs/products/price-history/playground` | mirror |

Reference page outline: endpoint + auth header; parameter table (section 3);
DAY vs CHANGE windows and point shapes; the cursor contract (section 3.3,
framed for integrators - "opaque, pass it back exactly as received", not the
AEAD internals); the no-data-no-pay matrix (section 5); error examples.
Shared billing/auth/error concepts link to `/docs/billing-and-credits`,
`/docs/authentication`, `/docs/errors` rather than being restated.

## 7. SEO plan

Target queries (intent: developers searching for historical price data):

| Query (en) | Query (fr) | Capturing page |
|---|---|---|
| product price history API, historical price API | API historique de prix, API prix historique | `/docs/products/price-history` |
| GTIN price trend API / EAN price history | API tendance prix EAN | `/docs/products/price-history` |
| price tracking API | API suivi de prix | `/pricing` |

- Slugs as in section 6; localized titles/descriptions, hreflang en/fr pairs,
  canonical URLs, following the pattern already live for `/docs/products/price`.
- Copy guardrails: only claim coverage for authorized sources (section 2);
  never imply full-market or all-merchant history.

## 8. Examples

```bash
# billable success (DAY, default window)
curl -s -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price/history?language=en"

# CHANGE granularity, explicit window
curl -s -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price/history?granularity=CHANGE&from=2026-06-01T00:00:00Z&to=2026-06-15T00:00:00Z"

# no-data-no-pay: product exists, no point from an authorized source
curl -i -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/<uncovered-gtin>/price/history"

# resume a page: cursor must be the exact nextCursor from a previous response
curl -i -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price/history?cursor=<opaque>"
```

## 9. Playground behavior

Same pattern as `product.price` ([`product-price.md`](product-price.md)
section 9): sample mode with a canned fixture GTIN and a canned
`no-price-history` response; live mode via the session-authenticated proxy,
displaying the executed request, response body/headers, `billable`, credits
consumed/remaining, and the no-pay reason. Additionally surfaces a
granularity toggle (`DAY`/`CHANGE`) and a "load next page" action that resends
the previous response's `nextCursor` verbatim - the playground never lets a
user hand-edit the cursor field, reinforcing that it is opaque.

## 10. Launch checklist

- [ ] Coverage re-measured at launch (section 2) and results updated
- [ ] Catalog entry live; pricing page renders 8 credits from the backend catalog
- [ ] Backend endpoint + full test matrix green (unit + Testcontainers adapter IT)
- [ ] OpenAPI complete; frontend client regenerated
- [ ] Docs pages en + fr published (no placeholder French)
- [ ] Playground sample + live modes working, including pagination
- [ ] SEO plan executed (metadata, hreflang, sitemap, structured data, internal links)
- [ ] `meta.coverage` reports `product.price-history` on every response
- [ ] Usage/no-pay-reason metrics visible (ops dashboards)

## 11. Open questions

- Whether `provider`/`condition`/`currency` filters should be exposed in the
  playground as separate fields or folded into a single "advanced filters"
  disclosure - deferred to playground implementation, no contract impact.
- A public provider whose usage policy transitions from allowed to denied
  mid-range: today the whole series for that provider simply disappears from
  subsequent responses (deny-at-read-time); whether historical exports should
  instead freeze at the policy version in force when a page was first served
  is a product question, not yet raised with Goulven.
