# Review Generation Service

## Purpose

The review-generation module now hosts the generic product enrichment pipeline
without renaming the Maven artifact. It builds French AI-assisted product
content from product metadata, discovered source URLs, fetched markdown,
extracted attributes, and prompt templates.

The canonical flow is:

```text
discover-urls -> fetch-sources -> attributes -> text
```

The `/enrichment/...` API is the primary contract. Legacy `/review/...`
endpoints remain available as deprecated delegates for existing callers.

## Product Enrichment

Source discovery stores a single URL inventory on `Product.sourceUrls`. Each
entry tracks the canonical URL, host, title, snippet, SERP rank, provider,
query/task metadata, detected source type, fetch status, fetch strategy/status
code, markdown, token count, content hash, and rejection reason.

Deprecated fields are still present for compatibility:

- `Product.officialUrl`
- `Product.officialSupportUrls`
- `Product.reviewFacts`
- `Product.reviewFetchDiagnostics`

Compatibility adapters mirror fetched `sourceUrls` into `reviewFacts` so the
existing prompt keys can be kept while attributes and text move to the generic
source inventory.

## Stages

### Remote Fetching

The fetching stage validates that the product has a brand and model, reads
existing `Product.sourceUrls` when discovery has already run, fetches selected
URLs, converts HTML to markdown, removes configured noise lines, applies token
gates, updates URL statuses, and persists accepted markdown on the URL entry.
When no source inventory exists, it falls back to the previous Google Custom
Search discovery path.

The first SERP queries target the official product page and official
support/manual evidence. Manufacturer HTML that passes prompt-source gates is
persisted in `Product.sourceUrls`; official/support URLs are also exposed
through legacy adapters, and official PDFs or extracted manuals are stored in
`Product.resources` with manufacturer tags. PDF resources are not injected into
the prompt as markdown during normal complete fetches.

If normal HTML/review retrieval fails quality thresholds, preprocessing can
build an explicit limited fallback instead of throwing immediately. It first
adds one synthetic `STRUCTURED_FACTS` source when at least three canonical
trusted EPREL/IceCat facts exist, then may add extracted text from official,
product-relevant PDFs with exact GTIN/model evidence. These fallback facts are
persisted in `Product.sourceUrls`/`Product.reviewFacts` and diagnostics expose
`LIMITED_STRUCTURED`, `LIMITED_OFFICIAL_PDF`, or
`LIMITED_STRUCTURED_AND_PDF`.

### URL Discovery

DataForSEO Standard is used in asynchronous batch mode. The default query avoids
`site:` operators for cost control and lets classification/prioritisation happen
after results are returned:

```text
BRAND "MODEL" ("fiche technique" OR caractéristiques OR test OR guide OR comparatif)
```

Tasks are submitted in chunks of up to 100. Local job state is persisted under
the review-generation batch folder and can be polled manually or by the
scheduled poller. Results parse `organic` and `featured_snippet` items, dedupe
by canonical URL, and cap storage to 20 URLs per product.

Review generation requests the fallback sequence `HTTP`, `PLAYWRIGHT`, then
`PROXIFIED` by passing internal strategy override headers to
`UrlFetchingService`. The fetch service consumes those headers before outbound
requests are sent.

### Attribute Extraction

The attribute stage requires fetched `Product.sourceUrls` (or legacy
`Product.reviewFacts`). It calls
`review-generation-attributes`, then persists extracted attributes into the
product attribute aggregate. It does not fetch remote pages and does not generate
review prose.

### Text Completion

The text stage requires fetched `Product.sourceUrls` (or legacy
`Product.reviewFacts`) and persisted product attributes. It injects those
attributes as `EXTRACTED_ATTRIBUTES`, calls
`review-generation`, post-processes citations and URLs, persists the French
`AiReviewHolder`, runs review-generation hooks, and reindexes the product.

### Global Workflow

The user-facing workflow chains the stages synchronously for one product:
fetching, attribute extraction, then text completion. URL discovery remains a
separate asynchronous step because DataForSEO Standard results are polled.

## API Endpoints

All endpoints are admin-only back-office endpoints.

| Scope | Endpoint | Behavior |
| --- | --- | --- |
| Product | `POST /enrichment/{id}/urls/discover?force=false` | Submit DataForSEO URL discovery. |
| Vertical | `POST /enrichment/vertical/{verticalId}/urls/discover?limit=100&force=false` | Submit discovery tasks in chunks of up to 100. |
| Job | `POST /enrichment/discovery/jobs/{jobId}/poll` | Poll one DataForSEO discovery job. |
| Job | `GET /enrichment/discovery/jobs/{jobId}` | Read local discovery job state. |
| Product | `POST /enrichment/{id}/fetch` | Fetch and persist source markdown. |
| Product | `POST /enrichment/{id}/attributes` | Extract and persist attributes from fetched sources. |
| Product | `POST /enrichment/{id}/text` | Generate and persist text from fetched sources and attributes. |
| Product | `POST /enrichment/{id}/workflow` | Run fetch, attributes, and text in sequence. |
| Vertical | `POST /enrichment/vertical/{verticalId}/fetch?limit=5` | Run fetch for up to `limit` products and return per-product results. |
| Vertical | `POST /enrichment/vertical/{verticalId}/attributes?limit=5` | Run attribute extraction for up to `limit` products. |
| Vertical | `POST /enrichment/vertical/{verticalId}/text?limit=5` | Run text completion for up to `limit` products. |
| Vertical | `POST /enrichment/vertical/{verticalId}/workflow?limit=5` | Run the full sequence for up to `limit` products. |

Deprecated `/review/...` equivalents remain available for all fetch,
attributes, text, workflow, and legacy batch endpoints.

Vertical endpoints run synchronously and return a `ReviewGenerationVerticalResult`
containing one `ReviewGenerationStepResult` per product. Per-product failures are
captured in the result payload and do not abort the rest of the vertical run.

## Invariants

- Stage endpoints persist their result by default.
- Attribute extraction and text completion never trigger remote fetching.
- Text completion fails if fetched sources or product attributes are missing.
- `Product.sourceUrls` is the durable contract between discovery, fetch, and LLM
  stages; `reviewFacts` remains a temporary compatibility view.
- Limited fallback reviews keep `enoughData=true`, but `resultQuality` and
  `dataQuality` must disclose the limited evidence and absence of independent
  tests, community feedback, prices, and dates.
- Fetch results include post-fetch enrichment status, including whether EPREL
  data was already present, completed by hooks, or still missing.
- Model-native web-search review generation remains a separate single-call mode
  and is not used by these decoupled external-source stage endpoints.
