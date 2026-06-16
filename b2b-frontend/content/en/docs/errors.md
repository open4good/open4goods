---
title: "Error handling"
description: "Problem Detail error format, all error types with examples, and how to handle them in your integration."
tags:
  - errors
  - error-handling
  - http
scope: public
---

# Error handling

All errors from Product Data API follow [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457). Every error response has a consistent JSON body and HTTP status code.

## Error response format

```json
{
  "type": "https://product-data-api.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "API key is missing or invalid.",
  "instance": "/api/v1/products/0885909950805/price"
}
```

| Field | Description |
|---|---|
| `type` | URI identifying the error type |
| `title` | Human-readable short description |
| `status` | HTTP status code |
| `detail` | Specific detail for this occurrence |
| `instance` | The request path that triggered the error |

## HTTP status codes

### 400 Bad Request - Invalid GTIN

```json
{
  "type": "https://product-data-api.com/errors/invalid-gtin",
  "title": "Invalid GTIN",
  "status": 400,
  "detail": "GTIN '12345' failed checksum validation.",
  "instance": "/api/v1/products/12345/price"
}
```

**What to do:** Validate GTINs on your side before sending requests. Use EAN-13 or UPC-A/UPC-E checksums. A 400 does not consume credits.

### 401 Unauthorized - Missing or invalid key

```json
{
  "type": "https://product-data-api.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "API key is missing or invalid.",
  "instance": "/api/v1/products/0885909950805/price"
}
```

**What to do:** Verify the `Authorization: Bearer pdapi_...` header is present and the key is active. Keys become 401 if they are revoked or expired.

### 402 Payment Required - Insufficient credits

```json
{
  "type": "https://product-data-api.com/errors/payment-required",
  "title": "Payment Required",
  "status": 402,
  "detail": "Insufficient credits to fulfill this request.",
  "instance": "/api/v1/products/0885909950805/price"
}
```

**What to do:** Add credits at [Dashboard → Billing](/dashboard/billing). Consider monitoring `meta.creditsRemaining` in responses to alert before exhaustion.

### 404 Not Found - Product not found

```json
{
  "type": "https://product-data-api.com/errors/product-not-found",
  "title": "Product Not Found",
  "status": 404,
  "detail": "No product found for GTIN '0000000000000'.",
  "instance": "/api/v1/products/0000000000000/price"
}
```

**What to do:** The product is not in our index. A 404 does not consume credits. If you expect this product to be indexed, contact support with the GTIN and the merchant source.

### 429 Rate Limited

```json
{
  "type": "https://product-data-api.com/errors/rate-limited",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Rate limit exceeded. Retry after 1 second.",
  "instance": "/api/v1/products/0885909950805/price"
}
```

**What to do:** Respect the `Retry-After` response header. Implement exponential backoff in your integration.

### 500 Internal Server Error

```json
{
  "type": "https://product-data-api.com/errors/internal",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Use the requestId to contact support.",
  "requestId": "req_01HXYZ"
}
```

**What to do:** Retry with exponential backoff. Include the `requestId` if you contact support.

## Recommended error handling

```python
import httpx

def get_price(gtin: str, api_key: str):
    headers = {"Authorization": f"Bearer {api_key}"}
    r = httpx.get(
        f"https://api.product-data-api.com/api/v1/products/{gtin}/price",
        headers=headers
    )

    if r.status_code == 400:
        raise ValueError(f"Invalid GTIN: {gtin}")

    if r.status_code == 401:
        raise PermissionError("Invalid API key")

    if r.status_code == 402:
        raise RuntimeError("Insufficient credits")

    if r.status_code == 404:
        return None  # Product not found - no credits consumed

    r.raise_for_status()
    payload = r.json()

    if not payload["meta"]["billable"]:
        return None  # Non-billable response (stale/empty)

    return payload["data"]
```

## Non-billable 200 responses

A `200 OK` response is not always billable. Check `meta.billable` before assuming credits were consumed. A non-billable 200 means the product was found but no fresh data was available.
