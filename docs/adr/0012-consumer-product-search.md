---
title: "ADR 0012: Consumer product search"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [13]
---

# ADR 0012: Consumer product search

## Context

Consumer search currently has duplicate list and global-search contracts, optional
query-time text embedding, kNN retrieval, semantic fallback and Java-side ranking.
This adds model and network cost, makes relevance difficult to explain, and splits
results and facets across paths. The source-neutral product program will replace the
legacy document with one GTIN projection containing a dedicated search slice.

Elasticsearch already provides French and English analysis, field weighting, exact
matching and `search_as_you_type`. These primitives cover the current consumer use
cases without a query-time model or vector index.

## Decision

Public consumer product search is lexical. A hotfix first removes all query-time text
embedding, kNN clauses, semantic diagnostics and text-vector generation while the
legacy index remains in service. Legacy request fields `semanticSearch` and
`searchType` remain accepted, deprecated and ignored during that compatibility window.
Existing stored product vectors are not read, migrated or deleted by the hotfix;
image-embedding behavior is unaffected.

The final search reads the `SearchSlice` of the source-neutral GTIN projection through
a neutral port. The slice contains normalized GTIN, localized canonical names, brand,
model, canonical class and category views, localized class labels and reviewed aliases,
model/family search tokens and ids, and low-weight merchant titles. The O4G registry is
the only source of reviewed localized search aliases.

Exact normalized fields serve GTIN, brand and model. Separate French and English text
fields use lowercase and ASCII folding, while suggestions use `search_as_you_type` and
one `bool_prefix` query. Relevance is ordered by exact GTIN, exact brand and model,
canonical name exact or phrase, exact class or alias, model/family tokens, all canonical
name terms, then merchant titles. A locally detected category intent strongly boosts
the matching canonical class. Low-weight fuzziness applies only to sufficiently long
text tokens.

Availability may filter on offer count, but offer count, price and Impact Score never
boost relevance. Default order is `_score` descending then GTIN ascending. An explicit
public sort is primary, followed by score and GTIN; without query text or explicit sort,
the order is `lastChange` descending then GTIN ascending. The target projection has no
`dense_vector`, HNSW, `script_score` or Java-side reranking.

The consumer contract converges on `POST /products/search` for a single product page
with ordinary and localized category facets. Public filter identifiers are stable core
or O4G attribute ids rather than Elasticsearch paths. `GET /products/suggest` remains,
combining in-memory registry categories with at most five Elasticsearch product hits.
The former list endpoint, grouped response, missing-vertical lane and semantic fields
are removed only in the coordinated breaking cutover.

The final index is built and evaluated in shadow, then switched by alias with an alias
rollback. Reintroducing semantic or hybrid retrieval requires a new WorkOrder plus both
a judged relevance benchmark and a production-shaped capacity benchmark. Experimental
hybrid/RRF code is not retained dormant in the production path.

## Consequences

The hotfix stops text-model and query-vector cost without rewriting stored legacy data.
The final path performs one Elasticsearch request per normal search and one per product
suggestion, with deterministic pagination and explainable field contributions. Search
quality becomes a versioned product contract measured on a reviewed French and English
corpus before alias cutover. The final implementation remains blocked until the neutral
projection and model/family grouping exist.
