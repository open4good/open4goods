# Facet spec authoring prompt (for AI agents)

Use this prompt to generate the spec for a new Product Data API facet. The
output is one file `docs/b2b/facets/product-<facet-id>.md`.

---

You are an AI agent working in `/home/goulven/git/open4goods`. Write the facet
specification for **`product.<FACET_ID>`** of the Product Data API (B2B brick).

## Inputs to read first

1. [`docs/b2b/00-canonical-decisions.md`](../00-canonical-decisions.md) - locked decisions; they win over everything.
2. [`docs/b2b/facets/_template.md`](_template.md) - your output must follow this structure exactly.
3. [`docs/b2b/facets/product-price.md`](product-price.md) - the canonical example of a finished spec; match its depth and tone.
4. [`docs/b2b/product/facet-catalog.md`](../product/facet-catalog.md) - the facet's planned endpoint, credit tier, source model blocks, and differentiation.
5. [`docs/b2b/business/data-coverage.md`](../business/data-coverage.md) - measured coverage and the ES query method (section 4).
6. [`docs/architecture/product-data-api-contract.md`](../../architecture/product-data-api-contract.md) - envelope, redaction table, coverage semantics.
7. The relevant `Product` model blocks (`model/src/main/java/.../product/`) and the matching `front-api` DTOs (`front-api/.../dto/product/`) listed in the catalogue's mapping table.

## Interactive State Machine Workflow

You must operate as a state machine, stopping to present findings and obtain explicit human approval at each transition before moving to the next state.

### State 1: Market Research & Competitive Analysis (Web Search & Approval)

A business-first approach is mandatory. Before doing any technical work or writing the spec, you must understand the competitive landscape for this specific facet's domain.

1. **Conduct Web Searches**: Use the web search tool to search for competitor APIs offering this type of data (e.g., search for "product reviews API", "ecological impact product API", "energy label API pricing"). Identify:
   - Specific competitors.
   - The features and attributes they return.
   - Their pricing structures, pricing models, and billing terms.
2. **Find Gaps & Uncovered Areas**: Map the findings against nudger's database structure to identify competitive gaps, missing features in competitor APIs, and opportunities where nudger can offer a larger feature surface.
3. **Formulate Value Proposition & Pricing**:
   - Determine how we will perform better (e.g. higher accuracy, cleaner data structure, better coverage).
   - Propose the credit tier and pricing alignment based on the competitor anchors in [`competition.md`](../business/competition.md).
   - Propose target SEO keywords based on competitor gaps.
4. **Checkpoint**: STOP here and present your research findings, proposed competitive angle, credit tier, and SEO queries to the user. Do not proceed to State 2 until the user grants explicit approval.

---

### State 2: Database Measurement & Quality Probing (Measurement & Approval)

Once the positioning is approved, verify the viability and quality of our data. Never copy coverage numbers from older documents without re-measuring.

1. **Verify Field Mappings**: Check field names against the live mapping:
   `curl -s "$ES/products-moustik/_mapping" | jq 'paths(scalars) ...'` or inspect the `Product` model annotations. Note that some blocks are stored but not indexed (e.g., `reviews`, `eprelDatas`, `ranking`); specify pipeline-side counters for these.
2. **Run Coverage Queries**: Execute coverage counts and aggregations on the devsec Elasticsearch (`products-moustik`, see [`local-runbook.md`](../../operations/product-data-api-local-runbook.md)). Follow query patterns in [`data-coverage.md`](../business/data-coverage.md) section 4. Record the query, results, and execution date.
3. **Probe Payload Quality**: Select 3-5 sample GTINs covered by the facet. Fetch their raw payloads (via ES `_doc` or the running endpoint) and inspect them to ensure they are non-empty, accurate, and respect our sanitization standards.
4. **Checkpoint**: STOP here and present your measured coverage numbers, Elasticsearch query details, sample payload checks, and proposed threshold criteria for shipping. Do not proceed to State 3 until the user approves the data quality.

---

### State 3: Spec Drafting (Template Application & Spec Approval)

1. **Draft the Specification**: Write the spec file at `docs/b2b/facets/product-<facet-id>.md` using the [`_template.md`](_template.md) structure. Do not invent or drop sections.
2. **Apply Spec Requirements**:
   - **Reference, do not duplicate**: Link the global redaction table and billing ledger documents. Only define the facet-specific `data` DTO mapping and sanitization rules.
   - Enforce allow-list sanitization (redact compensation, affiliation tokens, internal scorer IDs, etc.).
   - Define a strict facet-specific no-data-no-pay matrix.
   - Detail the SEO plan, target slugs, structured data, and sitemap settings.
3. **Checkpoint**: STOP here and present the complete draft specification to the user. Do not proceed to State 4 until the user reviews and approves the spec document.

---

### State 4: Registry Update & Finalization

1. **Update Product Catalog**: Write the catalog entry in `b2b-catalog.yml` and synchronize [`facet-catalog.md`](../product/facet-catalog.md) if measured coverage or credits deviate from initial plans.
2. **Initialize Task Checklist**: Add a `FACET-<id>` run section to [`implementation/tasks.md`](../implementation/tasks.md) referencing the launch checklist.
3. **Verify Formatting & Linting**: Run `./scripts/lint.sh` to ensure all generated Markdown conforms to repository styles and punctuation formatting.
4. **Handoff**: Provide a concise summary of the final spec, including key statistics, and highlight any unresolved questions from [`00-canonical-decisions.md`](../00-canonical-decisions.md) that require human resolution.
