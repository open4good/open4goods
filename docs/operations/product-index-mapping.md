# Products index mapping at startup (GOU-1835)

This note says which application writes the settings and mapping of the
products index `products-moustik`, when, and which log lines to look for when
it fails.

## Who writes it

Only the `api` writes the products index mapping, once per startup, after its
Spring context is created (`ProductIndexMappingRunner`, an
`ApplicationRunner`).

- Index absent: created with `product-settings.json` and
  `product-mappings.json` (module `model`).
- Index present: `product-mappings.json` is put on it (`PUT _mapping`). A
  mapping change only adds fields; existing fields never change.

`ui` and `front-api` never write to the products index at startup. The
`Product` document has `createIndex = false` and `alwaysWriteMapping = false`,
so building the `ElasticProductRepository` proxy (in `ui` and `api`) no longer
calls Elasticsearch. Before GOU-1835, that constructor ran `exists` then
`PUT _mapping` in `ui` and `api`, and any transient Elasticsearch failure
there failed the whole startup (GOU-1812, 2026-11-07). `front-api` never built
that proxy and never wrote this mapping.

A new field of `product-mappings.json` therefore reaches the production index
when the `api` restarts, not when `ui` restarts.

## Retries and startup

At most 3 attempts. The delay before the second one is
`api.product-index-mapping.retry-delay` (default `2s`), doubled before the
third. After the third failure the `api` keeps starting: the index exists in
production and its current mapping stays usable. Only fields added since the
last successful startup are missing from the mapping until the next restart.

## Log lines

All lines come from `org.open4goods.api.services.ProductIndexMappingRunner` in
the `api` log and start with `Products index mapping: index products-moustik`.

| Level | Message | Meaning |
| --- | --- | --- |
| INFO | `action CREATED, attempt n/3, <d> ms` | Index created with settings and mapping. |
| INFO | `action MAPPING_UPDATED, attempt n/3, <d> ms` | Mapping put on the existing index. |
| WARN | `attempt n/3 failed` | One attempt failed. The line carries the full stack trace, `Caused by` included: read the last `Caused by` for the root cause (timeout, 401/403, `cluster_block_exception`...). |
| ERROR | `failed after 3 attempts, <d> ms, mapping not applied; the api keeps starting` | All attempts failed. The `api` runs with the mapping already in the index. |
| ERROR | `interrupted after n attempt(s)` | The JVM was stopping during a retry wait. |

```bash
grep -A60 'Products index mapping' /opt/open4goods/logs/<api log file>
```

## After an ERROR

1. Read the `Caused by` of the three WARN lines.
2. Check the index state, read only, with the Ops account:
   `GET products-moustik/_settings?filter_path=**.blocks` (no block expected)
   and `GET _cluster/health/products-moustik`.
3. Once Elasticsearch is healthy, restart the `api` through the usual release
   procedure: the next startup puts the mapping again. Nothing else depends on
   it; `ui` and `front-api` do not need a restart.
