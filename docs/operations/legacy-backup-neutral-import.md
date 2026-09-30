---
title: "Resumable legacy backup to neutral data importer"
normative: false
audience: PROJECT_SCOPED
---

# Resumable legacy backup to neutral data importer

`LegacyBackupImportService` (module `api`, package
`org.open4goods.api.services.migration.legacybackup`), exposed through
`LegacyBackupImportController` under `/migration/legacy-backup/*`, imports the
pinned legacy backup (see
[product-backup-input-contract.md](product-backup-input-contract.md)) into
the neutral `SourceRecordHeadStore` and legacy price backfill stores, kept
separate from `BackupService.importProducts` and the active `ProductRepository`.

## Operations

Each call takes an explicit `datasetId` and pinned manifest path.

- `INVENTORY`: validates manifest versions and per-file digest/line count.
- `SAMPLE`: `INVENTORY` plus a dry-run classification of a bounded sample.
- `APPLY`: resumable write from the last committed checkpoint, batching
  checkpoint advances per `api.legacybackupimport.batch-size`.
- `STATUS`: checkpoint progress for a dataset.
- `CANCEL`: requests a running `APPLY` to stop after its current batch.

## Resumability

A `(fileIndex, committedLines)` cursor lives in `IngestionCheckpointStore`
(owner `legacy-backup-import-v1`). Resume reopens a gzip from the start and
decompresses/discards already-committed lines rather than seeking a
compressed offset. Writes are idempotent (deterministic record ids), so a
redone batch after a crash costs time, not correctness.

## Conversion and deduplication

Field disposition mirrors `pin_product_backup.py`'s `field_disposition()`.
GTIN identity, a small native scalar subset (name, brand, model...) and price
fields convert; Amazon-named and unmapped-attribute fields land in dead
letters instead of being invented.

The record id is `gtin:<value>`, so `find`-before-write against the target
store is the dedup oracle: an identical repeat is a no-op; a conflicting
repeat (same GTIN, different payload) goes to dead letters on both sides for
review, independent of processing order.

## Dead letters

NDJSON, one file per dataset, under `api.legacybackupimport.dead-letter-folder`
(private, outside Git). Reasons: `MALFORMED_JSON_LINE`,
`MISSING_OR_INVALID_GTIN`, `FORBIDDEN_SOURCE_CONTENT`,
`CONFLICTING_DUPLICATE_GTIN`, `ATTRIBUTE_MAPPING_DEFERRED`, `BULK_APPLY_FAILURE`.

## Follow-up scope

Full per-attribute mapping through the 94-entry
`legacy-attribute-migrations.json` registry is pending; affected fields show
up as `ATTRIBUTE_MAPPING_DEFERRED` dead letters for now.
