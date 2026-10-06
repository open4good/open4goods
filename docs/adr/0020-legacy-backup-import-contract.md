---
title: "ADR 0020: Legacy backup import contract"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [15]
---

# ADR 0020: Legacy backup import contract

## Context

The pinned legacy product backup (see
[product-backup-input-contract.md](../operations/product-backup-input-contract.md))
must become `SourceRecordHeadStore` and legacy price backfill data without
calling `BackupService.importProducts` or writing the active `ProductRepository`,
and without inventing provenance, rights, or a winner between conflicting
duplicates. A large gzip JSONL input and a long-running write also need a
resumability and backpressure contract that survives crashes and repeated
partitions.

## Decision

`LegacyBackupImportService` exposes five explicit operations, each requiring a
`datasetId` and the pinned manifest path; none run on application boot:

- `INVENTORY` validates manifest versions and per-file digest/line count.
- `SAMPLE` adds a dry-run classification of a bounded sample.
- `APPLY` performs a resumable write from the last committed checkpoint.
- `STATUS` reports checkpoint progress for a dataset.
- `CANCEL` requests a running `APPLY` to stop after its current batch.

Progress is a `(fileIndex, committedLines)` cursor in `IngestionCheckpointStore`
(owner `legacy-backup-import-v1`). Resume reopens the gzip from the start and
decompresses/discards already-committed lines; it never seeks a compressed
offset. A checkpoint advances only once every bulk result in its batch is
reconciled, so a crash mid-batch repeats work instead of losing or duplicating
accepted records.

Record ids are deterministic (`gtin:<value>`), making the target store's own
`find`-before-write the dedup oracle instead of an in-process map: an
identical repeat is a no-op; a conflicting repeat (same GTIN, different
payload) is dead-lettered on both sides for human review, independent of
processing or thread arrival order. Field disposition mirrors
`pin_product_backup.py`'s `field_disposition()`: GTIN identity, a small
native scalar subset, and price fields convert; Amazon-named and
unmapped-attribute fields are dead-lettered rather than invented.

## Consequences

Dead letters (NDJSON, one file per dataset, under
`api.legacybackupimport.dead-letter-folder`, outside Git) are the only record
of excluded or conflicting input; reasons are `MALFORMED_JSON_LINE`,
`MISSING_OR_INVALID_GTIN`, `FORBIDDEN_SOURCE_CONTENT`,
`CONFLICTING_DUPLICATE_GTIN`, `ATTRIBUTE_MAPPING_DEFERRED`, and
`BULK_APPLY_FAILURE`. Full per-attribute mapping through the 94-entry
`legacy-attribute-migrations.json` registry is deferred; affected fields
surface as `ATTRIBUTE_MAPPING_DEFERRED` dead letters rather than being
silently dropped.
