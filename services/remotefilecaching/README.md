# Remote File Caching Service

Utility for downloading remote resources with local caching and optional
archive extraction.

## Features

- Download files with configurable timeouts.
- Refresh files based on a configurable period.
- Supports GZIP decompression when retrieving archives.
- Logs URLs without their query values; `getResource(url, ttl, logLabel)` logs
  a caller-supplied label instead, for URLs carrying a secret in their path.

## Configuration

```yaml
remote-file-caching:
  connectionTimeout: 30000
  readTimeout: 30000
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is released under the [AGPL v3 license](../../LICENSE).
