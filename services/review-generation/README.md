# Review Generation Service

Generates AI-assisted product reviews using OpenAI. It searches the web
for sources, fetches content, and composes prompts either in realtime or
via the batch API.

## Features

- Google search integration to build context.
- URL fetching with concurrency control.
- Realtime and batch review generation flows.
- Scheduled processing of batch jobs.
- Exposes health metrics.

## Configuration

```yaml
review:
  generation:
    batchFolder: "/opt/open4goods/batch-ids/"
    threadPoolSize: 10
    maxQueueSize: 1000
    maxSearch: 5
    regenerationDelayDays: 30
    retryDelayDays: 7
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is provided under the [AGPL v3 license](../../LICENSE).
