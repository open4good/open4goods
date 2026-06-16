---
title: "Getting started"
description: "Sign up, create your first API key, and make your first price query in minutes."
tags:
  - getting-started
  - quickstart
scope: public
---

# Getting started

Product Data API is a metered REST API that returns fresh product price data by GTIN. You are billed only when the facet returns usable fresh data - invalid GTINs, missing products, and stale offers cost zero credits.

## 1. Sign up

Create a free account at [product-data-api.com/auth/login](/auth/login). Sign in with Google, Microsoft, GitHub, or Apple. Your account receives **2,500 free credits** automatically after first login.

## 2. Create your first API key

Go to [Dashboard → API Keys](/dashboard/api-keys) and click **Create key**.

- Choose a descriptive name (e.g., `dev-test`).
- The clear secret is shown **once**. Copy it immediately and store it securely.
- Your key has the prefix `pdapi_` and can be used in the `Authorization` header.

## 3. Query the price facet

```bash
curl -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price"
```

Replace `0885909950805` with a real GTIN-13 or GTIN-14.

## 4. Understand the response

A successful response looks like:

```json
{
  "meta": {
    "requestId": "req_01HXYZ",
    "gtin": "885909950805",
    "facet": "product.price",
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 2495,
    "reason": "fresh-offer",
    "responseTimeMs": 34
  },
  "data": {
    "bestPrice": {
      "price": 699.0,
      "currency": "EUR",
      "merchant": "TechStore FR"
    },
    "offerCount": 3,
    "freshness": {
      "oldestOfferDays": 4,
      "newestOfferDays": 1
    }
  }
}
```

If no fresh data exists, `billable` is `false` and `creditsConsumed` is `0`.

## Non-billable cases

| Situation | HTTP status | Credits |
|---|---|---|
| Invalid GTIN checksum | 400 | 0 |
| Product not found | 404 | 0 |
| All offers stale (>30 days) | 200 | 0 |
| Fresh offer returned | 200 | 5 |

## Next steps

- [API reference](/docs/api-reference) - full endpoint contract
- [Authentication](/docs/authentication) - key security and rotation
- [Billing and credits](/docs/billing-and-credits) - credit rules in detail
- [Price facet reference](/docs/products/price) - full response schema
- [Playground](/docs/products/price/playground) - test live calls in the browser
