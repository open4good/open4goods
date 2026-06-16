---
title: "API reference"
description: "All Product Data API endpoints, the shared response envelope, and common conventions."
tags:
  - api-reference
  - endpoints
scope: public
---

# API reference

The Product Data API exposes a versioned REST API at `https://api.product-data-api.com`. All endpoints return JSON.

## Base URL

```
https://api.product-data-api.com
```

## Authentication

Every request requires an API key in the `Authorization` header:

```http
Authorization: Bearer pdapi_YOUR_KEY_HERE
```

See [Authentication](/docs/authentication) for key creation and rotation details.

## Response envelope

All data endpoints wrap their payload in a shared envelope:

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
  "data": { ... }
}
```

| Field | Type | Description |
|---|---|---|
| `meta.requestId` | string | Unique request identifier for support queries |
| `meta.gtin` | string | Normalized GTIN used for the lookup |
| `meta.facet` | string | Facet identifier (`product.price` in v1) |
| `meta.billable` | boolean | Whether credits were consumed |
| `meta.creditsConsumed` | integer | Credits debited for this request |
| `meta.creditsRemaining` | integer | Balance after this request |
| `meta.reason` | string | Billing decision reason code |
| `meta.responseTimeMs` | integer | Server processing time |

## Billing reason codes

| Code | Meaning |
|---|---|
| `fresh-offer` | Usable fresh data returned - billed |
| `invalid-gtin` | Checksum validation failed - not billed |
| `product-not-found` | No matching product - not billed |
| `no-data` | Product found but no offers - not billed |
| `stale-data` | Offers exist but all exceed freshness window - not billed |
| `insufficient-credits` | Insufficient credits to reserve - 402 returned |

## Endpoints

### Price facet

```
GET /api/v1/products/{gtin}/price
```

Returns fresh price offers for the given GTIN. See [Price facet reference](/docs/products/price) for the full schema.

**Parameters**

| Parameter | In | Type | Description |
|---|---|---|---|
| `gtin` | path | string | GTIN-8, -12, -13, or -14 |
| `language` | query | string | Response language code (`en`, `fr`). Default: `en` |

### Customer billing

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/customer/billing/catalog` | Public billing catalog (no auth) |
| `GET` | `/api/v1/customer/billing/balance` | Credit balance and buckets |
| `GET` | `/api/v1/customer/billing/transactions` | Ledger transaction history |
| `GET` | `/api/v1/customer/billing/invoices` | Stripe invoice list |
| `POST` | `/api/v1/customer/billing/checkout/pack` | Start pack checkout |
| `POST` | `/api/v1/customer/billing/checkout/subscription` | Start subscription checkout |
| `POST` | `/api/v1/customer/billing/portal` | Open billing portal |

### API key management

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/customer/api-keys` | List API keys for the org |
| `POST` | `/api/v1/customer/api-keys` | Create a new API key |
| `POST` | `/api/v1/customer/api-keys/{id}/rotate` | Rotate a key (old secret invalidated) |
| `POST` | `/api/v1/customer/api-keys/{id}/revoke` | Permanently revoke a key |

### Auth

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/oidc` | Exchange OIDC token for session |
| `POST` | `/api/v1/auth/refresh` | Refresh session tokens |
| `POST` | `/api/v1/auth/logout` | Invalidate session |
| `GET` | `/api/v1/auth/me` | Current session user |

## Error responses

All errors follow [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457). See [Error handling](/docs/errors).

## Versioning

The current API version is `v1`. Breaking changes will increment the version prefix. The v1 contract is stable for production use.
