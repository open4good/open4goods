# URL Fetching Service

Provides asynchronous URL retrieval with domain-specific strategies. Can
record responses for testing and supports HTTP, proxified and Selenium
based fetchers.

## Features

- Configurable fetching strategy per domain.
- Optional response recording for mock generation.
- Metrics and health indicators.

## Configuration

```yaml
urlfetcher:
  threadPoolSize: 10
  domains:
    example.com:
      userAgent: "Mozilla/5.0"
      strategy: SELENIUM
    another.com:
      strategy: PROXIFIED
  record:
    enabled: false
    destinationFolder: "src/test/resources/urlfetching/mocks"
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for more details.
This module is licensed under the [AGPL v3](../../LICENSE).
