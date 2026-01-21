# Text Embedding Service

## Overview

This feature integrates semantic search capabilities by computing text embeddings for products using DJL (Deep Java Library).
These embeddings are stored in ElasticSearch as `dense_vector` fields, enabling semantic similarity queries.

## Components

### 1. DjlTextEmbeddingService

Provided by the shared `embedding-djl` module in `org.open4goods.embedding.service` and auto-configured for API and front-api.

- **Default models**: `intfloat/multilingual-e5-small` (text) with a multimodal CLIP fallback.
- **Tokenizer**: Uses `ai.djl.huggingface:tokenizers`.
- **Output**: Normalised embeddings (default dimension 512) with mean pooling.

### 2. NamesAggregationService Integration

The embedding is computed during the product aggregation phase (`onProduct`).

- **Trigger**: Runs whenever a product has enough descriptive text (vertical is optional).
- **Input Text**: Construction of `Vertical Prefix (Category) + Product Name + Top Offer Names + Popular Attribute name/value pairs`.
- **Throttling**: Text truncated to 1000 characters (model limit ~512 tokens).

### 3. Product Model & Storage

- **Java Model**: `Product` class has a `float[] embedding` field.
- **ElasticSearch**: `product-mappings.json` defines `embedding` as `dense_vector` with `cosine` similarity.

## Configuration

Configure via `embedding.*` properties (see `embedding-djl` module):

- `embedding.text-model-url` / `embedding.multimodal-model-url` to choose remote identifiers (primary + fallback).
- `embedding.fail-on-missing-model=true` makes startup fail if neither model loads.
- Health details are exposed by `DjlEmbeddingHealthIndicator` when Spring Boot Actuator is enabled.

## Deployment

- **Dependencies**: Requires `ai.djl.huggingface:tokenizers` and the PyTorch engine.
- **Resources**: First run downloads models from the configured DJL model URLs.
- **Performance**: Computation is CPU-based. Latency per item is approx 10-50ms depending on CPU.

## Troubleshooting

- **Missing Tokenizer**: Ensure `ai.djl.huggingface:tokenizers` is in the classpath.
- **Memory**: Vectors are large. ElasticSearch storage size will increase.
- **Logs**: Check `DjlTextEmbeddingService` logs for initialization errors and health indicator output.
