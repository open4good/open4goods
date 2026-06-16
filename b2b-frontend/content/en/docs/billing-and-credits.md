---
title: "Billing and credits"
description: "How the credit system works, what gets billed, the no-data-no-pay guarantee, and how to buy more credits."
tags:
  - billing
  - credits
  - no-data-no-pay
scope: public
---

# Billing and credits

Product Data API uses a credit system where you are charged only for requests that return usable fresh data. This is the **no-data-no-pay** guarantee.

## How credits work

Credits are consumed per facet request. The cost depends on the facet:

| Facet | Credits per request (billable) |
|---|---|
| `product.price` | 5 credits |

A credit is deducted only when the API returns a billable response - meaning the facet returned fresh, usable data for the requested product.

## No-data-no-pay

The following situations cost **zero credits**:

| Situation | HTTP status | Reason |
|---|---|---|
| Invalid GTIN checksum | 400 | `invalid-gtin` |
| Product not found in our index | 404 | `product-not-found` |
| Product found but no price offers | 200 | `no-data` |
| All offers exceed the freshness window | 200 | `stale-data` |

The freshness window for `product.price` is **30 days**. If all known offers for a product are older than 30 days, the response is non-billable.

## Billable cases

A request is billable when:

1. The GTIN is valid.
2. The product is in our index.
3. At least one price offer is within the freshness window.

The `meta` object in every response tells you exactly what happened:

```json
{
  "meta": {
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 995,
    "reason": "fresh-offer"
  }
}
```

## Free credits

Every new account receives **2,500 free credits** after the first login. These are valid for 12 months from creation.

## Buying credits

### Credit packs

One-time credit purchases. Buy as many as you need, use them at your own pace.

### Subscriptions

Monthly credits with automatic renewal. Unused credits roll over up to a configurable cap. Cancelling a subscription queues the remaining credits to expire 30 days after the end of the current period.

Manage packs and subscriptions at [Dashboard → Billing](/dashboard/billing).

## Credit expiry order

When a request is billed, credits are consumed from the earliest-expiring bucket first. This ensures your oldest credits are used before newer ones expire.

## 402 Insufficient credits

If your balance is zero and a billable request arrives, the API returns:

```json
{
  "type": "https://product-data-api.com/errors/payment-required",
  "title": "Payment Required",
  "status": 402,
  "detail": "Insufficient credits to fulfill this request."
}
```

No data is returned and no credits are consumed on a 402.

## Idempotency

If the same `requestId` is retried within the debit window, the credit debit is not repeated. This protects against double-billing from network retries.
