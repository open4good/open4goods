# Embedding services

The shared `embedding-djl` starter provides **image embeddings only** (`DjlImageEmbeddingService`). Text embeddings
were removed with ADR 0012: public product search is lexical (Elasticsearch) and no query-time or indexing-time text
embedding is computed. The `embedding` `dense_vector` field stays in `product-mappings.json` so stored vectors are
left in place (no reindex); it is no longer written or read for text.

## Configuration

Configuration is driven by the `embedding.*` properties (`DjlEmbeddingProperties`):

- `embedding.vision-model-url` / `embedding.image-input-size` - remote identifier and input size for image embeddings.
- `embedding.predictor-pool-size`, `embedding.async-loading` - predictor pool and loading behaviour.
- `embedding.fail-on-missing-model` - when true, startup fails if the vision model cannot be loaded.

Search endpoints (`/products`, `/products/search`, `/products/suggest`) do not require any `embedding.*` property.
The legacy request fields `semanticSearch` and `searchType` are still accepted, deprecated and ignored.
