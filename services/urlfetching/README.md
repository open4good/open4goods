# URL Fetching Service

Provides asynchronous URL retrieval with domain-specific strategies. Can
record responses for testing and supports HTTP, proxified and Playwright
browser-backed fetchers.

## Features

- Configurable fetching strategy per domain.
- Headless Playwright Chromium rendering for JavaScript-heavy pages.
- Optional response recording for mock generation.
- Metrics and health indicators.
- Structured `URL_FETCH` logs for strategy selection, status, duration, HTML
  size, and markdown size.

## Configuration

```yaml
urlfetcher:
  threadPoolSize: 10
  domains:
    example.com:
      userAgent: "Mozilla/5.0"
      strategy: PLAYWRIGHT
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
  domains:
    configured-domain.example:
      strategy: PLAYWRIGHT
      userAgent: "Mozilla/5.0"
      timeout: 15000
```

Reusable service modules should not carry business-domain defaults.

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
