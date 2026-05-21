# Wikidata Service

Fetches Wikidata entities by GTIN (P3962) or brand+model label, caches them in
Elasticsearch, and exposes services for product enrichment.

## Features

- SPARQL search by GTIN-13 (P3962).
- Brand + model SPARQL fallback when no GTIN is found.
- ES-backed cache with configurable refresh interval.
- Parses: labels/aliases, images (P18), videos (P10), website (P856),
  release date (P577), numeric claims (width, height, mass, depth),
  and Wikipedia sitelinks.
- Respects Wikidata data-access policy: identifiable User-Agent, 429/Retry-After handling.

## Configuration

```yaml
wikidata:
  user-agent: "myapp/1.0 (https://myapp.example; contact@myapp.example) Spring-RestClient"
  politeness-delay-ms: 300
  refresh-in-days: 30
  brand-model-fallback-enabled: true
  languages:
    - en
    - fr
```

## Build & Test

```bash
mvn --offline clean install
mvn --offline test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is licensed under the [AGPL v3](../../LICENSE).
