---
title: "Price history facet reference"
description: "Full reference for GET /api/v1/products/{gtin}/price/history - parameters, response schema, cursor pagination, and billing."
tags:
  - price-history
  - facet
  - products
scope: public
---

# Price history facet reference

The price history facet returns sanitized, multi-provider historical price data for a product identified by GTIN: daily rollups over up to five years, or sparse per-change events over the last 31 days.

## Endpoint

```http
GET /api/v1/products/{gtin}/price/history
Authorization: Bearer pdapi_YOUR_KEY_HERE
```

## Parameters

| Parameter | In | Type | Default | Description |
|---|---|---|---|---|
| `gtin` | path | string | required | GTIN-8, -12, -13, or -14 |
| `language` | query | string | `en` | Response language (`en`, `fr`) |
| `from` | query | ISO-8601 instant | `to` minus 30 days | Inclusive UTC start of the range |
| `to` | query | ISO-8601 instant | now | Exclusive UTC end of the range |
| `granularity` | query | `DAY` \| `CHANGE` | `DAY` | See below |
| `provider` | query | string | none | Public provider label to filter on |
| `condition` | query | `NEW` \| `OCCASION` \| `UNKNOWN` | none | Product condition filter |
| `currency` | query | ISO 4217 code | none | Currency filter |
| `limit` | query | integer | 100 | Page size, bounded 1..500 |
| `cursor` | query | opaque string | none | Continuation from a previous page's `nextCursor` |

### Granularity

| Granularity | Max window | Point shape |
|---|---|---|
| `DAY` | 5 years | Daily rollup: min/max/close amount and offer count |
| `CHANGE` | 31 days | Sparse event: time, amount, and availability state |

Requesting a window wider than the granularity's maximum, an inverted range, or a future-only range returns a `400 Bad Request` with reason `invalid-date-range`.

### Cursor pagination

`cursor` is fully opaque: always pass back the exact `nextCursor` value returned by a previous page. Never construct one by hand. Replaying a cursor against different filters, or sending a forged/corrupted value, always fails with a `400 Bad Request` (`cursor-mismatch`) - never a server error.

## Example request

```bash
curl -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price/history?language=en"
```

## Example response (billable, DAY granularity)

```json
{
  "meta": {
    "requestId": "req_01HXYZ",
    "gtin": "885909950805",
    "facet": "product.price-history",
    "billable": true,
    "creditsConsumed": 8,
    "creditsRemaining": 987,
    "reason": "has-history",
    "responseTimeMs": 44
  },
  "data": {
    "gtin": "0885909950805",
    "from": "2026-05-16T00:00:00Z",
    "to": "2026-06-15T00:00:00Z",
    "granularity": "DAY",
    "series": [
      {
        "provider": "Merchant Feed Partner",
        "condition": "NEW",
        "currency": "EUR",
        "dayPoints": [
          {
            "date": "2026-06-14",
            "minAmount": 749.00,
            "maxAmount": 819.99,
            "closeAmount": 799.99,
            "offerCount": 3
          }
        ],
        "changePoints": null
      }
    ],
    "nextCursor": null
  }
}
```

## Example response (non-billable - no history)

```json
{
  "meta": {
    "requestId": "req_02HABC",
    "gtin": "885909950805",
    "facet": "product.price-history",
    "billable": false,
    "creditsConsumed": 0,
    "creditsRemaining": 987,
    "reason": "no-price-history",
    "responseTimeMs": 9
  },
  "data": null
}
```

## Response schema

### `meta` object

| Field | Type | Description |
|---|---|---|
| `requestId` | string | Unique request ID for support |
| `gtin` | string | Normalized GTIN |
| `facet` | string | Always `product.price-history` |
| `billable` | boolean | `true` if credits were consumed |
| `creditsConsumed` | integer | Credits debited (0 or 8) |
| `creditsRemaining` | integer | Balance after this request |
| `reason` | string | Billing decision code |
| `responseTimeMs` | integer | Server processing time in ms |

### `data` object

| Field | Type | Description |
|---|---|---|
| `gtin` | string | Normalized GTIN |
| `from` / `to` | string | Effective, served UTC range (half-open) |
| `granularity` | string | Effective granularity actually served |
| `series[]` | array | One entry per distinct provider/condition/currency combination |
| `nextCursor` | string \| null | Opaque continuation, absent on the last page |

### `data.series[]` entry

| Field | Type | Description |
|---|---|---|
| `provider` | string | Public merchant label. Internal source ids are never exposed |
| `condition` | string | `NEW`, `OCCASION`, or `UNKNOWN` |
| `currency` | string | ISO 4217 currency code |
| `dayPoints[]` | array \| null | Populated when `granularity` is `DAY` |
| `changePoints[]` | array \| null | Populated when `granularity` is `CHANGE` |

`dayPoints[]` entries: `date`, `minAmount`, `maxAmount`, `closeAmount`, `offerCount`.

`changePoints[]` entries: `time`, `amount`, `state`.

## Billing

The facet costs **8 credits**, debited only when at least one policy-allowed point is served. Invalid GTIN, absent product, an empty or denied range, and an all-policy-denied result all consume zero credits.

## Sourcing

Only sources explicitly reviewed and authorized for price-history redistribution are served. A provider without an authorized, public-facing label never appears in a response - deny-by-default, redacted before billing is decided.

## Quickstarts

- [Live playground](/docs/products/price-history/playground)
- Client code follows the same authentication and error-handling pattern as the [price facet quickstarts](/docs/products/price/documentation/java) - point the request at `/price/history` instead of `/price` and parse the `series[]` shape above.
