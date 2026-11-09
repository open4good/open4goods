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

### Deploy order for a new field (GOU-1882, `impactScoreMeta`)

The root of the mapping is `strict`: a document carrying a field the index
mapping does not know yet is **rejected**. When a release adds a field that
the batch writes (for example `impactScoreMeta`, ImpactScore V2 design 1.3):

1. Deploy and restart the `api` first (a full release does), so the mapping
   is put before the first batch writes the field.
2. Check the `api` log: `action MAPPING_UPDATED` (or `CREATED`), not
   `failed after 3 attempts`.
3. Check the mapping, read only, with the Ops account:
   `GET products-moustik/_mapping/field/impactScoreMeta.*` must list the new
   properties.
4. Only then let the batch run. On beta first, then production.

If step 2 fails, the batch rejects every product it scores until the next
successful `api` restart: restart the `api` (see "After an ERROR") before the
next batch.

## Retries and startup

At most 3 attempts. The delay before the second one is
`api.product-index-mapping.retry-delay` (default `2s`), doubled before the
third. After the third failure the `api` keeps starting: the index exists in
production and its current mapping stays usable. Only fields added since the
last successful startup are missing from the mapping until the next restart.

## Log lines

All lines come from `org.open4goods.api.services.ProductIndexMappingRunner` in
the `api` log and start with `Products index mapping: index products-moustik`.
Beta and prod set `org.open4goods` to `warn`; `api/src/main/resources/application.yml`
keeps this logger at `INFO` so the result line stays visible.

| Level | Message | Meaning |
| --- | --- | --- |
| INFO | `action CREATED, attempt n/3, <d> ms` | Index created with settings and mapping. |
| INFO | `action MAPPING_UPDATED, attempt n/3, <d> ms` | Mapping put on the existing index. |
| WARN | `attempt n/3 failed` | One attempt failed. The line carries the full stack trace, `Caused by` included: read the last `Caused by` for the root cause (timeout, 401/403, `cluster_block_exception`...). |
| ERROR | `failed after 3 attempts, <d> ms, mapping not applied; the api keeps starting` | All attempts failed. The `api` runs with the mapping already in the index. |
| ERROR | `interrupted after n attempt(s)` | The JVM was stopping during a retry wait. |

```bash
grep -A60 'Products index mapping' /opt/open4goods/logs/nohup-api.log
```

## After an ERROR

1. Read the `Caused by` of the three WARN lines.
2. Check the index state, read only, with the Ops account:
   `GET products-moustik/_settings?filter_path=**.blocks` (no block expected)
   and `GET _cluster/health/products-moustik`.
3. Once Elasticsearch is healthy, restart the `api` through the usual release
   procedure: the next startup puts the mapping again. Nothing else depends on
   it; `ui` and `front-api` do not need a restart.
