# ADR 0002: Product Model Identity Confidence

## Status

Accepted

## Context

Product model identifiers are consumed across aggregation, EPREL completion,
review generation, official-resource fetching, API responses, and search
validation. The repository already stores a canonical `Product.model()` and
alternate `Product.akaModels`, but model values historically came from several
call sites with different cleaning rules.

The old canonical election also favored the shortest surviving model value. That
made weak candidates from merchant titles or noisy datasource references able to
replace better evidence, and it made sibling variants such as `4D 511` and
`4D 515` unsafe for pre-fetch searches.

## Decision

Centralize model identity handling in `ProductModelCandidateHelper`. All call
sites that persist, promote, rank, or validate model candidates use the shared
normalization, candidate quality checks, and source-aware scoring.

The persisted data shape is unchanged:

- `Product.model()` remains the canonical model.
- `Product.akaModels` remains the alternate model list.
- Existing API fields and storage mappings remain intact.

Model persistence is source aware:

- Code-like manufacturer references can be accepted from normal datasource
  ingestion.
- Named models are persisted only from strong evidence: EPREL, official
  manufacturer metadata, official manufacturer text, confirmed official URL
  tokens, or structured catalogue identifiers.
- Weak title-inferred candidates can participate in searches, but they do not
  promote named models into persisted product identity by themselves.

Canonical election is deterministic and based on evidence confidence plus
candidate quality. The source priority is:

1. EPREL model identifiers.
2. Official manufacturer metadata and explicit official text.
3. Official URL or resource tokens confirmed by manufacturer content.
4. Structured identifiers such as Icecat, MPN, SKU, or brand part code.
5. Datasource referential model values.
6. Title-inferred candidates.
7. Unknown or weak evidence.

Sibling drift is treated conservatively. Candidates that share a family prefix
but differ in the terminal numeric variant are not promoted from weak evidence.
Pre-fetch search candidates are hardened before use: conflicting sibling
alternates are removed when a canonical candidate exists, and ambiguous sibling
families are omitted when no authoritative winner exists.

EPREL and review-generation use the same candidate hardening and model-zone
matching helpers. EPREL GTIN matches remain authoritative, while multiple EPREL
sibling variants are rejected unless scoring or GTIN evidence produces one safe
winner.

## Consequences

- Product identity is cleaner before it reaches storage or API consumers.
- Strong official and EPREL evidence can promote readable named models without a
  schema migration.
- Weak merchant-title noise, storage variants, dimensions, pure numeric
  references, and sibling drift are less likely to contaminate canonical models.
- Search and fetch validation use the same hardened candidates as persistence,
  reducing false positives around neighbouring product variants.
- Callers that know the evidence source should use the source-aware
  `addModel(value, source)` or `promoteModel(value, source)` overloads.
