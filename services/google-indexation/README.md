# Google Indexation Service

This module provides a reusable client for the Google Indexing API. It supports
service-account authentication, Micrometer metrics, and Actuator health checks.

## Configuration

```yaml
google-indexation:
  enabled: false
  api-url: "https://indexing.googleapis.com/v3/urlNotifications:publish"
  request-timeout: 10s
  batch-size: 50
  # Provide credentials using either inline JSON or a file path
  service-account-json: ${GOOGLE_INDEXATION_SERVICE_ACCOUNT_JSON:}
  service-account-path: ${GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH:}
```

## Metrics

The service records:

- `google.indexation.publish.attempt`
- `google.indexation.publish.success`
- `google.indexation.publish.failure`

## Health

A health indicator is exposed through Spring Boot Actuator. When disabled, the
indicator reports `UP` with `enabled=false`. When enabled, it reports `DOWN` if
credentials are missing or the last request failed.
