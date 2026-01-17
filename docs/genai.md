# GenAI configuration (OpenAI + Gemini)

This document describes the Spring AI configuration keys and batch settings for the prompt service.

## Providers

### OpenAI (interactive + batch)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      batch:
        endpoint: https://api.openai.com/v1/batches
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
```

OpenAI grounded search uses the Responses API with the `web_search` tool when
`retrievalMode: MODEL_WEB_SEARCH` is configured for a prompt.

OpenAI batch defaults to `https://api.openai.com/v1/batches` and can be overridden
via `spring.ai.openai.batch.endpoint` (legacy fallback: `gen-ai-config.batch-api-endpoint`).

### Gemini (interactive)

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
        chat:
          options:
            model: gemini-2.5-flash
```

Gemini grounded search is enabled only for interactive prompts when
`retrievalMode: MODEL_WEB_SEARCH` and `allowWebSearch=true`. Grounding metadata
is collected from the Spring AI response metadata.

## Batch (OpenAI + Vertex Gemini)

Batch jobs **only** support `EXTERNAL_SOURCES`. If a prompt is configured with
`MODEL_WEB_SEARCH`, the batch service rejects the request and instructs you to
inject sources instead.

### Vertex Gemini batch configuration

```yaml
vertex:
  batch:
    project-id: ${GCP_PROJECT_ID}
    location: ${GCP_LOCATION:europe-west4}
    bucket: ${VERTEX_BATCH_BUCKET}
    output-prefix: vertex-batch-output
    poll-interval: 30s
    credentials-json: ${VERTEX_SERVICE_ACCOUNT_JSON}

gen-ai-config:
  batch-poll-interval: 30s
```

Notes:
- The service account JSON **content** (not a file path) is the preferred
  mechanism for batch authentication.
- The batch input/output artifacts are stored in the configured GCS bucket.
- Prompt batches must use `EXTERNAL_SOURCES` only.

## Prompt retrieval modes

| Mode | Interactive behavior | Batch behavior |
|------|----------------------|----------------|
| `EXTERNAL_SOURCES` | Application injects sources | ✅ Supported |
| `MODEL_WEB_SEARCH` | Provider-native grounding | ❌ Rejected |

## Batch prompt selection (to confirm)

1 - Identify the prompt keys intended for batch processing (GTIN or GTIN-derived flows).
2 - Ensure those prompt YAML files explicitly set `retrievalMode: EXTERNAL_SOURCES`.
3 - Keep interactive prompts that require grounding on `MODEL_WEB_SEARCH`.

## Troubleshooting

- **No provider bean created**: verify the correct `spring.ai.*` properties are present for the desired provider.
- **OpenAI batch failures**: verify `spring.ai.openai.api-key` and the OpenAI batch endpoint.
- **Vertex batch failures**: verify `vertex.batch.project-id`, `vertex.batch.location`,
  and `vertex.batch.bucket`, plus service account JSON content.
