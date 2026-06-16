---
title: "Price facet reference"
description: "Full reference for GET /api/v1/products/{gtin}/price - parameters, response schema, freshness rules, and billing."
tags:
  - price
  - facet
  - products
scope: public
---

# Price facet reference

The price facet returns sanitized price offers, freshness metadata, and provenance labels for a product identified by GTIN.

## Endpoint

```http
GET /api/v1/products/{gtin}/price
Authorization: Bearer pdapi_YOUR_KEY_HERE
```

## Parameters

| Parameter | In | Type | Required | Description |
|---|---|---|---|---|
| `gtin` | path | string | Yes | GTIN-8, -12, -13, or -14 |
| `language` | query | string | No | Response language (`en`, `fr`). Default: `en` |

## Example request

```bash
curl -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price?language=en"
```

## Example response (billable)

```json
{
  "meta": {
    "requestId": "req_01HXYZ",
    "gtin": "885909950805",
    "facet": "product.price",
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 995,
    "reason": "fresh-offer",
    "responseTimeMs": 31
  },
  "data": {
    "bestPrice": {
      "price": 699.0,
      "currency": "EUR",
      "merchant": "TechStore FR",
      "offerUrl": "https://shop.example.com/product/abc",
      "condition": "new"
    },
    "offers": [
      {
        "price": 699.0,
        "currency": "EUR",
        "merchant": "TechStore FR",
        "condition": "new",
        "lastSeenDays": 1
      },
      {
        "price": 739.0,
        "currency": "EUR",
        "merchant": "BigBox Online",
        "condition": "new",
        "lastSeenDays": 4
      }
    ],
    "offerCount": 2,
    "freshness": {
      "oldestOfferDays": 4,
      "newestOfferDays": 1,
      "windowDays": 30
    }
  }
}
```

## Example response (non-billable - stale data)

```json
{
  "meta": {
    "requestId": "req_02HABC",
    "gtin": "885909950805",
    "facet": "product.price",
    "billable": false,
    "creditsConsumed": 0,
    "creditsRemaining": 995,
    "reason": "stale-data",
    "responseTimeMs": 12
  },
  "data": null
}
```

## Response schema

### `meta` object

| Field | Type | Description |
|---|---|---|
| `requestId` | string | Unique request ID for support |
| `gtin` | string | Normalized GTIN (leading zeros preserved) |
| `facet` | string | Always `product.price` |
| `billable` | boolean | `true` if credits were consumed |
| `creditsConsumed` | integer | Credits debited (0 if non-billable) |
| `creditsRemaining` | integer | Balance after this request |
| `reason` | string | Billing decision code |
| `responseTimeMs` | integer | Server processing time in ms |

### `data.bestPrice` object

| Field | Type | Description |
|---|---|---|
| `price` | number | Lowest current price |
| `currency` | string | ISO 4217 currency code |
| `merchant` | string | Sanitized merchant display name |
| `offerUrl` | string | Direct offer URL (may be affiliate-wrapped) |
| `condition` | string | `new` or `used` |

### `data.offers[]` array

Each offer entry:

| Field | Type | Description |
|---|---|---|
| `price` | number | Offer price |
| `currency` | string | ISO 4217 currency code |
| `merchant` | string | Sanitized merchant name |
| `condition` | string | `new` or `used` |
| `lastSeenDays` | integer | Days since this offer was last observed |

### `data.freshness` object

| Field | Type | Description |
|---|---|---|
| `oldestOfferDays` | integer | Age of oldest offer in `offers[]` |
| `newestOfferDays` | integer | Age of newest offer in `offers[]` |
| `windowDays` | integer | Current freshness window (30) |

## Freshness and billing

An offer is considered fresh if it was observed within the last **30 days**. If all known offers for a product are older than 30 days, the response is non-billable (`reason: stale-data`).

## GTIN normalization

GTINs are normalized before lookup:
- Leading zeros are preserved for GTIN-13 and GTIN-14.
- GTIN-8 and GTIN-12 (UPC-A) are accepted and validated with checksum.
- Invalid checksums return `400 Bad Request` immediately, before any credit reservation.

## Rate limits

| Plan | Requests per second |
|---|---|
| Free | 1 |
| Starter | 10 |
| Pro | 50 |
| Enterprise | Custom |

## Quickstarts

- [Java quickstart](/docs/products/price/documentation/java)
- [Python quickstart](/docs/products/price/documentation/python)
- [Live playground](/docs/products/price/playground)
