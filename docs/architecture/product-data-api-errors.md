# Product Data API - Error catalog (RFC 9457)

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> All errors are returned as RFC 9457 Problem Details
> (`application/problem+json`) via a Spring `@RestControllerAdvice` /
> `ProblemDetail`. Every billable-path error states whether it consumes credits.
> The `meta` envelope is **not** used for errors; Problem Detail is the body.

## Problem Detail shape

```jsonc
{
  "type": "https://product-data-api.com/problems/insufficient-credits",
  "title": "Insufficient credits",
  "status": 402,
  "detail": "Organization balance (3) is below the cost of product.price (5).",
  "instance": "/api/v1/products/0885909950805/price",
  "requestId": "pdreq_01HF...",
  "creditsRemaining": 3
}
```

`type` URIs live under `https://product-data-api.com/problems/{slug}` and are
documented (a static docs page per slug is acceptable). `requestId` is always
included and echoed in `X-Request-Id`.

## Catalog

| HTTP | type slug | title | When | Credits |
|---:|---|---|---|---|
| 400 | `invalid-gtin` | Invalid GTIN | GTIN fails checksum/format (`BarcodeValidationService`) | **0** (rejected before reserve) |
| 400 | `invalid-parameter` | Invalid parameter | bad `language` or query param | 0 |
| 401 | `missing-credentials` | Missing API key | no `Authorization: Bearer` header | 0 |
| 401 | `invalid-credentials` | Invalid API key | malformed / unknown / revoked / disabled key | 0 |
| 402 | `insufficient-credits` | Insufficient credits | balance < facet max cost (reservation failed) | **0** (nothing debited) |
| 404 | `product-not-found` | Product not found | valid GTIN, no product in index | **0** (no-data-no-pay) |
| 422 | `validation-error` | Validation error | request body validation (dashboard endpoints) | 0 |
| 429 | `rate-limited` | Too many requests | per-key rate limit exceeded (see ops spec) | 0 |
| 500 | `internal-error` | Internal error | unhandled server error | 0 (reservation released) |
| 503 | `service-unavailable` | Service unavailable | Redis/ES down and `fail-closed=true` | 0 |

Notes:
- **200 + non-billable** is NOT an error: a valid product with no fresh offer
  returns `200` with an empty price payload, `meta.billable=false`,
  `creditsConsumed=0`, and `meta.facets[].served=false`. This is the no-data-no-pay
  happy path, not a Problem Detail.
- `429` includes `Retry-After`.
- On any error after a successful Redis reservation, the reservation is released
  (refunded) in `finally`/`afterCompletion`, so errors never leak credits.

## Mapping table (exceptions -> Problem)

| Exception | HTTP / slug |
|---|---|
| `InvalidGtinException` (wraps `BarcodeValidationService` failure) | 400 invalid-gtin |
| `MissingApiKeyException` | 401 missing-credentials |
| `InvalidApiKeyException` | 401 invalid-credentials |
| `InsufficientCreditsException` | 402 insufficient-credits |
| `ResourceNotFoundException` (from `ProductRepository.getByIdWithoutEmbedding`) | 404 product-not-found |
| `MethodArgumentNotValidException` / `ConstraintViolationException` | 422 validation-error |
| `RateLimitExceededException` | 429 rate-limited |
| `RedisUnavailableException` (fail-closed) | 503 service-unavailable |
| anything else | 500 internal-error (log with `requestId`, no stack to client) |

Document each status in OpenAPI with `@ApiResponse` and a Problem Detail example.
