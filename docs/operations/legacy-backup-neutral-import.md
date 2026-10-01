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
[product-backup-input-contract.md](product-backup-input-contract.md)) into the
neutral `SourceRecordHeadStore` and legacy price backfill stores.

The operations, resumability, conversion, deduplication, and dead-letter
contracts are [ADR 0020](../adr/0020-legacy-backup-import-contract.md).
