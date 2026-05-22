# URL Fetching Service

Provides asynchronous URL retrieval with domain-specific strategies. Can
record responses for testing and supports HTTP, proxified and Playwright
browser-backed fetchers.

## Features

- Configurable fetching strategy per domain.
- Headless Playwright Chromium rendering for JavaScript-heavy pages.
- JSON-LD, meta tag, and itemprop extraction before markdown conversion.
- Optional GTIN validation against structured metadata.
- Optional response recording for mock generation.
- Metrics and health indicators.
- Structured `URL_FETCH` logs for strategy selection, status, duration, HTML
  size, and markdown size.

## Configuration

```yaml
urlfetcher:
  threadPoolSize: 10
  # Replay failed or empty Playwright fetches through this global proxy.
  playwrightProxyFallbackEnabled: true
  proxy:
    scheme: http
    host: rotating-proxy.example
    port: 8080
    username: "${URLFETCHER_PROXY_USERNAME:}"
    password: "${URLFETCHER_PROXY_PASSWORD:}"
  domains:
    example.com:
      userAgent: "Mozilla/5.0"
      strategy: PLAYWRIGHT
      timeout: 15000
    another.com:
      strategy: PROXIFIED
  record:
    enabled: false
    destinationFolder: "src/test/resources/urlfetching/mocks"
```

Applications configure domain-specific fetch behavior in their own YAML. The
API project owns the French authoritative product-evaluation domains in
`api/src/main/resources/application.yml`:

```yaml
urlfetcher:
  playwrightProxyFallbackEnabled: true
  proxy:
    scheme: http
    host: rotating-proxy.example
    port: 8080
  domains:
    configured-domain.example:
      strategy: PLAYWRIGHT
      userAgent: "Mozilla/5.0"
      timeout: 15000
```

Reusable service modules should not carry business-domain defaults.

## Structured Metadata

Every fetcher extracts structured metadata before HTML is converted to
markdown. The response includes:

- `metadataAttributes`: attributes from `application/ld+json`, selected
  `meta` tags, and `[itemprop]` elements, each with a source marker.
- `extractedGtins`: normalized GTIN values found in JSON-LD or itemprop data.
- `rejected` and `rejectionReason`: set when validation rejects a response.

Callers that know the expected product GTIN can pass
`X-Open4goods-Expected-Gtin`. If the page exposes structured GTIN metadata and
none of the values match the requested GTIN, the service returns a rejected
response with status `409` and empty markdown. If the page exposes no GTIN, the
fetch is not rejected.

## Product Cache Contract

Fetched review facts are stored on products by the review-generation/product
layer, not by this module. This keeps `urlfetching` independent from product
persistence and avoids forcing every low-level URL fetch to write to
Elasticsearch.

The intended cache policy is:

- Use existing `Product.reviewFacts` as the product-scoped cache.
- Treat facts as fresh for 6 months by default.
- Pass `X-Open4goods-Force-Fetch: true` from callers that intentionally bypass
  cached facts.
- Do not refetch stale URLs until a product workflow actually needs that source.

The force header is removed before outbound HTTP requests, so it is only a
local orchestration signal.

Callers can request an explicit Playwright proxy replay with
`X-Open4goods-Playwright-Proxy: true`. The header is also stripped before the
origin request.

## Playwright Runtime

Install Chromium for local or CI environments before running live Playwright
fetches:

```bash
mvn -pl services/urlfetching exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium"
```

Fetcher logs use stable key/value messages:

```text
URL_FETCH strategy=PLAYWRIGHT url=https://example.test phase=start timeoutMs=15000 headless=true
URL_FETCH strategy=PLAYWRIGHT url=https://example.test phase=complete statusCode=200 durationMs=812 htmlChars=42153 markdownChars=11840
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for more details.
This module is licensed under the [AGPL v3](../../LICENSE).
