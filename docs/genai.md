# GenAI configuration (OpenAI + Gemini)

This document describes the Spring AI configuration keys and batch settings for the prompt service.

## Providers

The API service defaults to the OpenAI Spring AI chat model:

```yaml
spring:
  ai:
    model:
      chat: ${SPRING_AI_CHAT_MODEL:openai}
      embedding: none
      image: none
      audio:
        speech: none
        transcription: none
      moderation: none
```

Set `SPRING_AI_CHAT_MODEL=google-gen-ai` only when Gemini should be the active
Spring AI chat model. In that mode, provide either `GEMINI_API_KEY` for the
Gemini Developer API or Vertex project/location credentials for Vertex AI.

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
        # For Vertex AI mode, omit api-key and set project-id/location instead.
        # project-id: ${GCP_PROJECT_ID}
        # location: ${GCP_LOCATION:europe-west4}
        chat:
          options:
            model: gemini-3.1-pro-preview
```

Gemini grounded search is enabled only for interactive prompts when
`retrievalMode: MODEL_WEB_SEARCH` and `allowWebSearch=true`. Grounding metadata
is collected from the Spring AI response metadata.

For Gemini 3 models, the provider keeps structured output enabled together with
Google Search grounding. The Spring AI `BeanOutputConverter` JSON schema is
converted to the Vertex schema shape before the request is sent, including
uppercase Vertex type enums (`OBJECT`, `ARRAY`, `STRING`, etc.). For older Gemini
models, grounded prompts still skip `responseSchema` and rely on prompt
instructions plus application-side JSON validation/repair.

Current references:
- Gemini 3.1 Pro Preview model capabilities: https://ai.google.dev/gemini-api/docs/models/gemini-3.1-pro-preview
- Gemini structured outputs with tools: https://ai.google.dev/gemini-api/docs/structured-output#structured_outputs_with_tools
- Vertex structured output schema limits: https://cloud.google.com/vertex-ai/generative-ai/docs/multimodal/control-generated-output

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

## Review generation retrieval pipeline (2026-05)

The review-generation service now defaults to application-managed retrieval and keeps model-native grounding optional (`review.generation.use-grounded-prompt=false`).

Pipeline:
1. Google Search API query generation and retrieval.
2. URL scoring/dedup and capped selection (`review.generation.max-urls-per-product`, default `10`).
3. Intelligent multi-strategy fetching fallback:
   - attempt 1: direct HTTP
   - attempt 2: local Playwright strategy
   - attempt 3: external anti-bot provider strategy (ZenRows)
4. Markdown extraction and token filtering.
5. Facts persistence on `Product.reviewFacts` with URL, markdown, language, freshness timestamp, strategy and hash.
6. Merge new facts with existing facts by URL, preserving latest content.

Config knobs:
- `review.generation.max-urls-per-product`
- `review.generation.facts-max-stored`
- `review.generation.fact-max-markdown-chars`
- `review.generation.use-grounded-prompt`

This enables reprocessing from stored facts without requiring another web fetch pass.
