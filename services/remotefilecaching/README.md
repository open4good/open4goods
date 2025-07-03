# Remote File Caching Service

Utility for downloading remote resources with local caching and optional
archive extraction.

## Features

- Download files with configurable timeouts.
- Refresh files based on a configurable period.
- Supports GZIP decompression when retrieving archives.

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
