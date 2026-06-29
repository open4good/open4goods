# Product Data API - Public API contract & sanitized DTO

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> SpringDoc is the source of truth for the live contract; this document is the
> human spec the implementation and the OpenAPI annotations must match. Error
> bodies are in [`product-data-api-errors.md`](product-data-api-errors.md).

## v1 endpoint

```http
GET /api/v1/products/{gtin}/price?language=en|fr
Authorization: Bearer pdapi_...
```

- `{gtin}` - syntactically valid, checksum-correct GTIN (8/12/13/14). Validated
  with `org.open4goods.commons.services.BarcodeValidationService` **before** any
  credit reservation. Invalid -> `400`, zero credits.
- **GTIN -> product id normalization**: the index id (`Product.id`) is a `Long`,
  so leading zeros are not significant - after validation, the GTIN is parsed to
  its numeric value for `ProductRepository.getByIdWithoutEmbedding(Long)`
  (e.g. `0885909950805` resolves the same product as `885909950805`). The
  response `data.gtin` echoes the normalized form from the validation service.
  Verify this rule against `BarcodeValidationService` output at implementation
  time (P5 of the [implementation plan](../b2b/implementation/plan.md)).
- `language` - `en` (default) or `fr`. Affects localized display fields only
  (price data is language-neutral; the display `name` and provenance labels use
  the locale).
- Auth - opaque `pdapi_` key. Missing/malformed/unknown/revoked -> `401`.
- OpenAPI security scheme: `productDataApiKey` (HTTP bearer) for data endpoints;
  bearer/session for dashboard endpoints.

Status codes: `200`, `400`, `401`, `402`, `404`, `429`, `500` - all error bodies
are RFC 9457 Problem Details.

## Response envelope

Every B2B API response uses this envelope. The `data` shape is facet-specific;
`meta` is uniform.

```jsonc
{
  "data": { /* B2bPriceDto - see below */ },
  "meta": {
    "requestId": "pdreq_01HF...",
    "timestamp": "2026-06-09T10:00:00Z",
    "language": "en",
    "creditsConsumed": 5,
    "creditsRemaining": 2495,
    "billable": true,
    "freshnessDays": 30,
    "responseTimeMs": 42,
    "facets": [
      { "id": "product.price", "credits": 5, "served": true, "billable": true }
    ],
    "coverage": [
      { "id": "product.price", "covered": true }
    ]
  }
}
```

### `meta.coverage` (locked decision)

A per-facet declaration of whether the requested facet is *covered* for this
product, independent of whether it was billed. For v1 it always contains
`product.price`. It exists so that, once premium facets ship (impact/energy/
review cover only the curated ~45-50K subset), clients can see availability
honestly without guessing from empty payloads. `covered=false` implies
`billable=false` and `served=false` for that facet.

v1 semantics for `product.price`: the price facet applies to **every** product in
the index, so `covered` is always `true` - a product that exists but has zero or
stale offers is *covered but not served* (`covered=true`, `served=false`,
`billable=false`, no-pay reason `no-fresh-offer`). `covered=false` only becomes
meaningful with curated-subset facets (impact/energy/review).

### Headers

Mirror the metering meta as headers for clients that do not parse the body:

- `X-Request-Id`
- `X-Credits-Consumed`
- `X-Credits-Remaining`
- `X-Response-Time-Ms`

## Sanitized price DTO

Dedicated B2B records under `org.open4goods.b2bapi.dto.product`. They are mapped
**from** `Product` / `AggregatedPrices` reusing the offer-mapping logic of
`front-api` `ProductMappingService` (do not expose `front-api`'s
`ProductOffersDto` directly). Records, `@Schema` on every field with examples.

### `B2bResponse<T>` / `B2bMeta`
Generic wrapper. `B2bResponse<T>(T data, B2bMeta meta)`. `B2bMeta` carries the
fields shown in the envelope above (`requestId`, `timestamp`, `language`,
`creditsConsumed`, `creditsRemaining`, `billable`, `freshnessDays`,
`responseTimeMs`, `List<B2bFacetMeta> facets`, `List<B2bCoverageMeta> coverage`).

### `B2bPriceDto` (the `data` for the price facet)

| Field | Type | Source (`Product` / `AggregatedPrices`) | Notes |
|---|---|---|---|
| `gtin` | String | `Product.id` (normalised) | always present |
| `name` | String (nullable) | `Product.names` (localized) | display name if available |
| `brand` | String (nullable) | brand, when safely available | |
| `model` | String (nullable) | model, when safely available | |
| `offersCount` | Integer | `Product` / `price.offers.size()` | |
| `freshOffersCount` | Integer | offers with `timestamp` within `freshnessDays` | drives billable |
| `bestPrice` | B2bOfferDto (nullable) | `AggregatedPrices.getMinPrice()` / best across conditions | |
| `bestNewOffer` | B2bOfferDto (nullable) | `AggregatedPrices.bestNewOffer()` | |
| `bestOccasionOffer` | B2bOfferDto (nullable) | `AggregatedPrices.bestOccasionOffer()` | |
| `offersByCondition` | Map<ProductCondition, List<B2bOfferDto>> | `AggregatedPrices.sortedOffers(condition)` | sanitized offers grouped |
| `newTrend` / `occasionTrend` | B2bPriceTrendDto (nullable) | `AggregatedPrices.getTrends()` | "already available, no heavy work" |
| `newHistorySummary` / `occasionHistorySummary` | B2bPriceHistorySummaryDto (nullable) | `getHistoryLowest/Highest/Average` | summary only in price facet; full series is the future `price-history` facet |

### `B2bOfferDto` (sanitized offer)

| Field | Type | Source (`AggregatedPrice` / `Price`) | Notes |
|---|---|---|---|
| `merchant` | String | `AggregatedPrice.shortDataSourceName()` | label only, never raw datasource id |
| `title` | String | `AggregatedPrice.offerName` | |
| `url` | String | `AggregatedPrice.url` | public/affiliated public-safe URL |
| `condition` | ProductCondition | `AggregatedPrice.productState` | NEW / OCCASION |
| `amount` | Double | `Price.getPrice()` | |
| `currency` | Currency | `Price.getCurrency()` | |
| `timestamp` | Instant | `Price.getTimeStamp()` | |
| `freshnessAgeDays` | Integer | derived from `timestamp` | |
| `faviconUrl` | String (nullable) | derived, only if already public-safe | optional |

### Redaction table (NEVER expose)

| Internal field | Where | Why hidden |
|---|---|---|
| `compensation` | `AggregatedPrice.compensation` (`model/.../price/AggregatedPrice.java:23`) | private commercial metadata |
| `affiliationToken` | `AggregatedPrice.affiliationToken` (`:36`) | affiliate secret |
| raw `datasourceName` | `AggregatedPrice.datasourceName` | internal datasource id; expose only `shortDataSourceName()` |
| `quantityInStock`, `shippingTime`, `shippingCost` | `AggregatedPrice` | not collected reliably; out of v1 contract (do not imply stock/shipping data) |
| crawler/cache keys, internal scoring/debug | `Product` blocks | internal |

The mapper must construct `B2bOfferDto` field-by-field (allow-list), not copy the
source object, so new internal fields are never leaked by default.

## Forward-compatible facet catalogue

v1 ships only `product.price` as billable. The catalogue (`b2b-catalog.yml`,
shape in [`master-prompt.md`](../b2b/implementation/master-prompt.md)) is structured so future facets reuse metering/docs/playground
without redesign. Coverage figures come from
[`../b2b/business/data-coverage.md`](../b2b/business/data-coverage.md); credit
tiers from [`../b2b/product/facet-catalog.md`](../b2b/product/facet-catalog.md).
Each shipped facet has a dedicated lifecycle spec under
[`../b2b/facets/`](../b2b/facets/README.md).

| Facet | Endpoint | v1? | Credits | Coverage |
|---|---|---|---:|---|
| `product.price` | `/products/{gtin}/price` | **yes** | 5 | ~33.9M |
| `product.identity` | `/products/{gtin}/identity` | future | 1 | ~34M |
| `product.attributes` | `/products/{gtin}/attributes` | future | 4 | ~34M |
| `product.images` / `product.documents` | `.../images` `.../documents` | future | 3 | ~125K |
| `product.price-history` | `.../price/history` | future | 8 | subset |
| `product.impact` | `.../impact` | future | 15 | ~45-50K (exclusive) |
| `product.energy` | `.../energy` | future | 10 | ~47K (exclusive) |
| `product.taxonomy` | `.../taxonomy` | future | 15 | curated (exclusive) |

## Playground proxy (session-authenticated)

The dashboard playground's live mode never holds a clear API key in the browser.
It calls a session-authenticated proxy that executes the real external call with
a selected key and returns the executed request (key masked), the response
body/headers, and the metering outcome:

```http
POST /api/v1/customer/playground/products/price
Content-Type: application/json
Cookie: session

{ "apiKeyId": "key_...", "gtin": "0885909950805", "language": "en" }
```

Response shape and UX requirements: [`../b2b/frontend/ui-spec.md`](../b2b/frontend/ui-spec.md)
section 7.5. Role gating: `DEVELOPER` and above ([auth spec](product-data-api-auth.md)).

## OpenAPI requirements

Every endpoint documents `@Operation`, `@ApiResponse` (200/400/401/402/404/429),
`@Parameter`, `@SecurityRequirement(productDataApiKey)`, and DTO field-level
`@Schema` with examples (the examples feed frontend client-generation quality).
Expose Swagger UI, raw OpenAPI JSON, and Redoc/Scalar. Do not hand-edit generated
clients.
