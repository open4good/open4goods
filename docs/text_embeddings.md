# Text Embedding Service

## Overview

This feature integrates semantic search capabilities by computing text embeddings for products using the **DistilCamemBERT** model via DJL (Deep Java Library).
These embeddings are stored in ElasticSearch as `dense_vector` fields, enabling semantic similarity queries.

## Components

### 1. DjlTextEmbeddingService

A Spring Service located in `org.open4goods.api.services.completion.text`.

- **Model**: Loading `cmarkea/distilcamembert-base` via HuggingFace hub (or local cache).
- **Tokenizer**: Uses `ai.djl.huggingface:tokenizers`.
- **Output**: 768-dimensional float vector.
- **Pooling**: Mean pooling is applied to sentence tokens to produce a single vector.

### 2. NamesAggregationService Integration

The embedding is computed during the product aggregation phase (`onProduct`).

- **Trigger**: Only runs if the product has a valid `vertical` assigned.
- **Input Text**: Construction of `Vertical Prefix (Category) + Product Name`.
- **Throttling**: Text truncated to 1000 characters (model limit ~512 tokens).

### 3. Product Model & Storage

- **Java Model**: `Product` class has a `float[] embedding` field.
- **ElasticSearch**: `product-mappings.json` defines `embedding` as `dense_vector` with `cosine` similarity.

## Configuration

The model is currently hardcoded as `cmarkea/distilcamembert-base`.
The cache directory for models is handled by DJL (default `~/.djl/cache`).

## Deployment

- **Dependencies**: Requires `ai.djl.huggingface:tokenizers` and an Engine (PyTorch or OnnxRuntime).
- **Resources**: First run will download the model (~260MB). Ensure the server has internet access or pre-populate the cache.
- **Performance**: Computation is CPU-based. Latency per item is approx 10-50ms depending on CPU.

## Troubleshooting

- **Missing Tokenizer**: Ensure `ai.djl.huggingface:tokenizers` is in the classpath.
- **Memory**: Vectors are large. ElasticSearch storage size will increase.
- **Logs**: Check `DjlTextEmbeddingService` logs for initialization errors.
