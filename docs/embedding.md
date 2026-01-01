# Embedding services

The project provides a shared **DJL-based text embedding starter** (`embedding-djl`). It loads text and multimodal text models and exposes a `DjlTextEmbeddingService` bean to consuming modules (API and front-api).

## Configuration

Configuration is driven by the `embedding.*` properties (`DjlEmbeddingProperties`):

- `embedding.text-model-url` / `embedding.multimodal-model-url` — remote identifiers for text embeddings (primary + fallback).
- `embedding.vision-model-url` / `embedding.image-input-size` — remote identifier and input size for image embeddings.
- `embedding.fail-on-missing-model` — when true, startup fails if neither text model nor multimodal fallback can be loaded.
- `embedding.pooling-mode`, `embedding.engine` — translator and engine tuning knobs (embeddings are always L2-normalized).

A health indicator (`DjlEmbeddingHealthIndicator`) publishes which model was loaded and its resolved path/URL when Spring Boot Actuator is present.

## Usage

Add the `embedding-djl` module as a dependency and configure the properties above. The service loads remote DJL models directly and logs failures on startup when configured to fail fast.
