# Favicon Service

Retrieves and caches website favicons. Provides a REST API to query and
fetch icons with optional fallback mechanisms.

## Features

- Resolve favicon URLs from HTML pages or direct mappings.
- Cache icons in memory and on disk via `RemoteFileCachingService`.
- Exposes metrics and health status.
- REST endpoints:
  - `GET /favicon/exists?url=...` – check if a favicon is available.
  - `GET /favicon?url=...` – retrieve the favicon bytes.
  - `DELETE /favicon/cache` – clear the cache.

## Configuration

```yaml
favicon:
  cacheFolder: "/opt/open4goods/.cached/"
  urlTimeout: 5000
  fallbackUrl: "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url={url}&size=64"
  domainMapping:
    example.com: "http://www.example.com/favicon.ico"
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is licensed under the [AGPL v3](../../LICENSE).
