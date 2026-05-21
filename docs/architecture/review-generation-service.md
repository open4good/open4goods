# Review Generation Service

## Purpose

The review-generation service builds French AI-assisted product reviews from
product metadata, fetched markdown sources, extracted attributes, and prompt
templates. The flow is intentionally split into three independently testable
stages plus a global workflow that chains them.

## Stages

### Remote Fetching

The fetching stage validates that the product has a brand and model, builds SERP
queries, fetches selected URLs, converts HTML to markdown, removes configured
noise lines, applies token gates, and persists accepted markdown in
`Product.reviewFacts`.

The first SERP queries target the official product page and official
support/manual evidence. Manufacturer HTML that passes prompt-source gates is
persisted in `Product.reviewFacts`; official/support URLs are also stored on the
product, and official PDFs or extracted manuals are stored in
`Product.resources` with manufacturer tags. PDF resources are not injected into
the prompt as markdown.

Review generation requests the fallback sequence `HTTP`, `PLAYWRIGHT`, then
`PROXIFIED` by passing internal strategy override headers to
`UrlFetchingService`. The fetch service consumes those headers before outbound
requests are sent.

### Attribute Extraction

The attribute stage requires existing `Product.reviewFacts`. It calls
`review-generation-attributes`, then persists extracted attributes into the
product attribute aggregate. It does not fetch remote pages and does not generate
review prose.

### Text Completion

The text stage requires existing `Product.reviewFacts` and persisted product
attributes. It injects those attributes as `EXTRACTED_ATTRIBUTES`, calls
`review-generation`, post-processes citations and URLs, persists the French
`AiReviewHolder`, runs review-generation hooks, and reindexes the product.

### Global Workflow

The user-facing workflow chains the three stages synchronously for one product:
remote fetching, attribute extraction, then text completion. The existing
asynchronous `/review/{id}` endpoint remains available for frontend-triggered
generation and status polling.

## API Endpoints

All endpoints are admin-only back-office endpoints.

| Scope | Endpoint | Behavior |
| --- | --- | --- |
| Product | `POST /review/{id}/fetch` | Fetch and persist `reviewFacts`. |
| Product | `POST /review/{id}/attributes` | Extract and persist attributes from existing `reviewFacts`. |
| Product | `POST /review/{id}/text` | Generate and persist review text from existing `reviewFacts` and attributes. |
| Product | `POST /review/{id}/workflow` | Run fetch, attributes, and text in sequence. |
| Vertical | `POST /review/vertical/{verticalId}/fetch?limit=5` | Run fetch for up to `limit` products and return per-product results. |
| Vertical | `POST /review/vertical/{verticalId}/attributes?limit=5` | Run attribute extraction for up to `limit` products. |
| Vertical | `POST /review/vertical/{verticalId}/text?limit=5` | Run text completion for up to `limit` products. |
| Vertical | `POST /review/vertical/{verticalId}/workflow?limit=5` | Run the full sequence for up to `limit` products. |

Vertical endpoints run synchronously and return a `ReviewGenerationVerticalResult`
containing one `ReviewGenerationStepResult` per product. Per-product failures are
captured in the result payload and do not abort the rest of the vertical run.

## Invariants

- Stage endpoints persist their result by default.
- Attribute extraction and text completion never trigger remote fetching.
- Text completion fails if `reviewFacts` or product attributes are missing.
- The persisted markdown facts remain the contract between fetch and LLM stages.
- Fetch results include post-fetch enrichment status, including whether EPREL
  data was already present, completed by hooks, or still missing.
- Model-native web-search review generation remains a separate single-call mode
  and is not used by these decoupled external-source stage endpoints.
