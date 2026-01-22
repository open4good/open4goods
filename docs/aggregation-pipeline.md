<!--
  Aggregation pipeline audit & hardening
  Scope: /api module aggregation services (realtime + batch)
-->

# Aggregation Pipelines Audit

## Functionality

### Realtime aggregation
- **Ingress**: `DataFragmentStoreService` validates and standardizes incoming `DataFragment` objects, then enqueues them for async processing.
- **Worker**: `DataFragmentAggregationWorker` dequeues fragments and calls `aggregateAndstore`.
- **Aggregation**: `AggregationFacadeService.updateOne(...)` applies the realtime pipeline by calling `StandardAggregator.onDatafragment(...)`.
- **Realtime pipeline order** (configurable via YAML):
  1. Identity (GTIN validation, barcode country/type)
  2. Taxonomy (categories → vertical + taxonomy)
  3. Attributes (indexing + brand/model cleaning)
  4. Names (i18n names + embeddings)
  5. Price (offers + history)
  6. Media (resources + Icecat URL cleaning)

### Batch aggregation
- **Entry points**: `BatchService.batch(...)` (scheduled) and admin endpoints in `BatchController`.
- **Steps**:
  1. Classification (identity + taxonomy)
  2. Completion (external data enrichment)
  3. Sanitisation aggregation
  4. Scoring pipeline (clean → attribute → sustainalytics → data quality → eco score → aggregated scores)
- **Batch scoring** uses `ScoringBatchedAggregator` which runs lifecycle hooks (`init` → `onProduct` → `done`).

## Design

### Hardening changes implemented
- **Non-blocking worker batching**: The realtime worker now polls with a timeout and drains remaining items without waiting for a full batch, preventing indefinite blocking when traffic is low and preserving fragment order.
- **Lifecycle-aware sanitisation**: `StandardAggregator` now offers a batch entry point that triggers `init/done` hooks so sanitisation steps can safely accumulate state when needed.
- **YAML-driven pipeline composition**: `AggregationFacadeService` now reads service ordering from `open4goods.aggregation.pipelines.*`, allowing per-environment tuning without code changes.
- **Strict pipeline validation**: Unknown/blank service IDs are rejected at startup to avoid silent misconfiguration.

### Functional enhancements & checks
- **Fragment loss avoidance**: Replacing `HashSet` with ordered `List` buffers prevents accidental de-duplication of fragments during aggregation.
- **Configuration-first design**: Pipelines can now be re-ordered, extended, or trimmed via YAML without rewiring Java constructors.

## Guides

### How to add an aggregation service
1. **Create the service**
   - Implement `AbstractAggregationService`.
   - Add `onDataFragment` for realtime updates and/or `onProduct` for product-level aggregation.
2. **Register the service**
   - Add a new service identifier in `AggregationFacadeService`’s `buildService(...)` switch.
   - Add the identifier to the appropriate pipeline list in `application.yml`.
3. **Verify with tests**
   - Add or extend unit tests to confirm service ordering and lifecycle hooks are respected.

### How to use it in batch
1. Add your service ID to `open4goods.aggregation.pipelines.sanitisation` if it is a product-cleanup step.
2. For scoring logic, add your service ID to `open4goods.aggregation.pipelines.scoring`.
3. Trigger batch via `/batch/` or `/score/{vertical}` in `BatchController` to verify behavior.

### How to use it in realtime
1. Add your service ID to `open4goods.aggregation.pipelines.realtime`.
2. Run the ingestion pipeline; the realtime worker will apply the service to each incoming `DataFragment`.

