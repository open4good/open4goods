# Embedding services

The project provides a shared **DJL-based text embedding starter** (`embedding-djl`). It loads text and multimodal text models and exposes a `TextEmbeddingService` bean to consuming modules (API and front-api).

## Configuration

Configuration is driven by the `embedding.*` properties (`DjlEmbeddingProperties`):

- `embedding.text-model-path` / `embedding.multimodal-model-path` — local model paths (default `/opt/open4goods/models/text-embedding` and `/opt/open4goods/models/multimodal-embedding`). When `embedding.prefer-local-models=true`, these paths are validated and used first.
- `embedding.text-model-url` / `embedding.multimodal-model-url` — remote identifiers used when no local model is available.
- `embedding.fail-on-missing-model` — when true, startup fails if neither model can be loaded after applying the local/remote rules.
- `embedding.pooling-mode`, `embedding.normalize-outputs`, `embedding.engine` — translator and engine tuning knobs.

A health indicator (`DjlEmbeddingHealthIndicator`) publishes which model was loaded and its resolved path/URL when Spring Boot Actuator is present.

## Usage

Add the `embedding-djl` module as a dependency and configure the properties above. When local assets under `/opt/open4goods` are missing, the service logs a warning and falls back to the remote models (unless `embedding.fail-on-missing-model=true`).
