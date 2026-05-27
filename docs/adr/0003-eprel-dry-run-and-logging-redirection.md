# ADR 0003: EPREL Dry-Run Endpoints and Logging Redirection

## Status
Accepted

## Context
1. **Empty/Silent Logs in Production**: The completion services (e.g. `EprelCompletionService`, `AmazonCompletionService`, etc.) extend `AbstractCompletionService` but defined their own local, shadowed private/protected static loggers. Consequently, the dynamic file-appender logger initialized programmatically by the parent class (`AbstractCompletionService`) was bypassed, causing execution logs to be lost.
2. **Missing EPREL Iterative Debugging Tool**: Testing EPREL resolution and model sanitization previously required executing the entire enrichment pipeline. This lacked granularity and feedback for evaluating specific GTINs or resolving specific matching failures.
3. **Asynchronous Enrichment Logic**: Verify re-triggering logic when scraping is run and EPREL matching was previously skipped or failed.

## Decision
1. **Inherited Logger Pattern**:
   - Refactored `AbstractCompletionService` to expose its dynamic, file-scoped `Logger` as `protected final Logger logger`.
   - Removed shadowed, class-based logger fields from all extending subclasses (`EprelCompletionService`, `AmazonCompletionService`, `IcecatCompletionService`, `ResourceCompletionService`, `WikidataCompletionService`). This redirects all logging statements through the parent's logger, resolving the empty file logs issue in `/opt/open4goods/logs/`.
2. **EPREL Dry-Run APIs**:
   - Created `/api/completion/eprel/gtin/{gtin}/dry-run` and `/api/completion/eprel/product/{id}/dry-run` endpoints in `CompletionController`.
   - The endpoints execute EPREL resolution and scoring logic without modifying the database or indexing state, returning a detailed JSON response (e.g. matched candidate info, model parameters, scores, and validation status).
3. **Stabilized Sanitization Rules**:
   - Fixed a bug in `ProductModelCandidateHelper.sanitizeModelName` where revisions or model parts matching suffix patterns starting with `-` followed by a number (e.g. `-2`, `-4A`) were aggressively truncated.
4. **Verified Async Completion Re-trigger**:
   - Verified that `PostFetchEnrichmentHook` correctly uses the `hadEprelBeforeFetch` flag to conditionally re-run completion if the product lacked EPREL enrichment prior to scraping.

## Consequences
- Dynamic loggers correctly write to their respective log files (e.g. `completion-eprelcompletionservice.log`).
- Developers can use dry-run endpoints to instantly trace, test, and troubleshoot matching logic on any GTIN.
- Complex model matching is now stable and less prone to false negatives due to aggressive trimming.
