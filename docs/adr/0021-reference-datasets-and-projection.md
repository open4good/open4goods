---
title: "ADR 0021: Reference datasets and projection onto products"
status: accepted
normative: true
audience: PROJECT_SCOPED
---

# ADR 0021: Reference datasets and projection onto products

Supersedes ADR-0010.

## Context

ADR 0010 gave ingestion to `services/data-reference`: a source-record store, a canonical
registry, a resolver and a projection index, with usage policies granting content types
to `NUDGER_WEB`, `B2B_API` and `ODBL_EXPORT`. That module was discarded at the October 2026
reset; only unmerged remote branches remain. The B2B surface was removed on 2026-11-08.

The pipeline that runs on `main` is different. Feeds create `DataFragment`s that
`DataFragmentStoreService` aggregates into `Product`. Completion services (`EprelCompletionService`,
`IcecatCompletionService`, ...) build synthetic fragments and call
`StandardAggregator.onDatafragment` directly. EPREL already keeps a full catalogue in its own
index, `eprel-products`, and matches products against it.

The owner wants maximal aggregation of the open datasets that carry a GTIN or an ISBN, with
transparent sourcing; legal review comes later. The plans are in
[`docs/datasources/integration/`](../datasources/integration/README.md).

## Decision

1. **One reference index per source.** Each dataset is imported whole into
   `ref-<id>-<timestamp>`, behind the alias `ref-<id>`. The switch is refused when the new
   version loses more rows than `min-rows-ratio` allows, as `IcecatIndexVersionManager` does
   for Icecat. The new module `services/reference-datasets` owns download, parsing and these
   indexes through one `ReferenceDatasetProvider` per source, emitting a neutral
   `ReferenceRecord`. It never depends on `api` (ADR 0007 boundary).
2. **Projection is a separate step, in `api`.** `ReferenceProjectionService` iterates the
   dataset, not the product index, and reads products by batches of GTIN
   (`ProductRepository.multiGetForAggregation`). A known product receives a synthetic
   `DataFragment` through `StandardAggregator.onDatafragment`. An unknown identifier creates
   the product through `DataFragmentStoreService` (`CREATE`, the default since 2026-11-09);
   a source can be set to `ENRICH_EXISTING` instead.
3. **Projection whitelist per source.** A source declares which product parts it may write
   (attributes, names, brand, resources, recalls, labels). A non-merchant source never
   writes a price or a category.
4. **Transparent sourcing.** Each product carries one `SourceAttribution` per source (record id,
   record URL, source update date, retrieval date, content hash). The source configuration
   carries its licence and attribution text. Licences are attribution metadata only: no
   deny-by-default policy engine and no per-surface grants.
5. **One identifier filter.** A shared GTIN/ISBN validity filter runs in every provider. No
   product is created from a code it rejects.
6. **EPREL stays as is.** `eprel-products` and the EPREL completion already follow this
   pattern; extending EPREL to new product groups does not require a `ref-<id>` index.

### Amendment (2026-11-15): streaming projection by default, index only when needed

The owner decided on 2026-11-15 to replace point 1 and to make point 5 precise.

**Point 1, replaced.** A source is read as a stream, not imported into an index first:

- The provider reads the downloaded file from `RemoteFileCachingService`, or pages through the
  source API. Each `ReferenceRecord` goes straight to the projection: identifier check,
  `multiGetForAggregation` by batches, then enrichment or creation (point 2, unchanged).
- Each source keeps a small state snapshot on disk: identifier, content hash and state of each
  record, plus the row count. The next run diffs against it to find changed and removed
  records. The `min-rows-ratio` guard compares the new row count with the snapshot and stops
  the run before any write.
- Records without a usable identifier (brand and model only) are loaded in memory at each run
  and matched against the products of the vertical, as completion services do.
- A dedicated Elasticsearch index `ref-<id>` is created only when a source must be searched at
  scale, as EPREL is (`eprel-products`, brand and model lookups over a large catalogue). The
  source plan states why; the index then follows the original point 1 (versioned index, alias
  switch, `min-rows-ratio`).

Why: the per-source indexes of the compact sources (about 200,000 documents in total) cost
little disk, but each one adds an index lifecycle (versions, alias, rollback) for data that is
already on disk. The real size risks are in the products index; see the consequences below.

**Point 5, made precise.** The identifier filter is the existing `BarcodeValidationService`,
strengthened, not a separate filter in each provider. It gives one of two verdicts:

- **invalid** (wrong check digit, too few significant digits once leading zeros are removed,
  known placeholder codes): rejected everywhere, as `UNKNOWN` codes are today;
- **restricted** (`Gs1Class` `RESTRICTED_INTERNAL` or `COUPON`, today computed by
  `BarcodeForensicsService` but never used): never creates a product; an existing product keeps
  being aggregated, so its page is not frozen.

## Consequences

- A rule or mapping change can be replayed from `ref-<id>` without downloading the source
  again. Each source adds one index; storage is measured per source in its import report.
- `CREATE` brings pages outside the seven verticals. Before the first source in `CREATE`, they
  need a sitemap rule and attribute labels, and the identifier filter must be in place.
- The products index mapping is `"dynamic": "strict"`: `sourceAttributions` and any new block
  must be mapped before the first deployment.
- ADR 0010's registry, typed assertions, resolver and `surfaceGrants` are not built. If a later
  legal review restricts a source, the restriction goes into that source's whitelist and
  attribution, not into a separate policy engine.

### Consequences of the 2026-11-15 amendment

- The first consequence above changes: a rule or mapping change is replayed from the cached
  download, not from an index. A source without an index has no Kibana view; its import report
  and snapshot replace it.
- The size to watch is the products index: products created by `CREATE` (up to about 100,000
  for the compact sources, millions for the book and Web Data Commons dumps, hence
  `max-created-per-run`), the `_source` size added per product, and the 1,500-field limit of
  `product-settings.json`. The content-hash skip is mandatory, since a projection rewrites the
  whole product document.
- `eprel-products` is the existing reference index to measure first: production imports every
  EPREL group and never deletes a record.
