# OpenData service

Generates and exposes the Open4goods GTIN and ISBN datasets as CSV files compressed into ZIP archives, with their
manifests (rows, bytes, sha256, fill rate per column, ODbL licence).

- Generation runs in the `api` only (`open-data-config.generation-enabled`, weekly cron and `POST /opendata/generate`
  in the api, GOU-2248).
- Architecture and manifest contract: [docs/architecture/opendata.md](../../docs/architecture/opendata.md).
- Operations: [docs/operations/opendata-generation.md](../../docs/operations/opendata-generation.md).
