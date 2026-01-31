# Nudger Front API

This module provides the REST endpoints used by the Nuxt 3 frontend. It exposes data aggregated by other open4goods modules and is secured with JWT tokens.
The former `/api/v1/search` endpoint was removed from this module.
The main REST endpoints are provided by the `ProductController` class.
Use the main `api` service for search operations.

### Content endpoints

- `GET /blocs/{blocId}` – return a small HTML bloc from XWiki.
- `GET /pages/{xwikiPageId}` – return a rendered XWiki page with metadata.

## Default Port

The application runs on **port 8082** by default (`server.port` in `application.yml`). This keeps it separate from the main API on port 8081.

## Security

Security is enabled by default using JWT authentication. For local testing it can be disabled by setting
`front.security.enabled=false` in `application.yml` or via an environment variable.
To allow the Nuxt frontend running on a different host during development, configure the list of allowed
CORS origins using the `front.security.cors-allowed-hosts` property. You can override the default value
via the `FRONT_SECURITY_CORS_ALLOWED_HOSTS` environment variable. By default it permits requests from
`http://localhost:8082`.

Additional properties configure token generation:

- `front.security.jwt-secret` – secret key used to sign JWT tokens.
- `front.security.access-token-expiry` – duration before an access token expires (default `PT30M`).
- `front.security.refresh-token-expiry` – duration before a refresh token expires (default `P7D`).

With this configuration all calls stay on the same origin and the built-in CORS
rules apply correctly.

## Rate limiting

Requests are limited using an in-memory token bucket. Defaults can be changed in
`application.yml`:

- `front.rate-limit.anonymous` – requests per minute allowed for unauthenticated users.
- `front.rate-limit.authenticated` – requests per minute allowed for authenticated users.

When limits are exceeded the API responds with HTTP `429 Too Many Requests`.

## Google Indexation

The front API can notify the Google Indexing API after AI review generation
succeeds. The integration is disabled by default and must be enabled explicitly.

Front API dispatch configuration:

- `front.google-indexation.enabled` – master toggle (default `false`).
- `front.google-indexation.realtime-enabled` – dispatch immediately on success.
- `front.google-indexation.batch-enabled` – dispatch queued URLs on schedule.
- `front.google-indexation.batch-interval` – schedule delay (default `30m`).
- `front.google-indexation.batch-size` – maximum URLs per batch.
- `front.google-indexation.max-attempts` – retry limit for failed URLs.
- `front.google-indexation.site-base-url` – public base URL used to build product URLs.

Google Indexing API credentials:

- `google-indexation.service-account-json` – raw service account JSON.
- `google-indexation.service-account-path` – file path to the service account JSON.

Credentials are required when `front.google-indexation.enabled=true`. You can
provide them via environment variables:

```bash
export FRONT_GOOGLE_INDEXATION_ENABLED=true
export FRONT_GOOGLE_INDEXATION_SITE_BASE_URL=https://nudger.fr
export GOOGLE_INDEXATION_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
```

## API documentation

Access to the Swagger UI (`/swagger-ui.html`) and the raw OpenAPI specification
(`/v3/api-docs`) requires valid XWiki credentials. Use HTTP Basic
authentication when requesting these endpoints. Example commands:

```bash
curl -u XWIKI_USER:XWIKI_PASS http://localhost:8082/swagger-ui.html
curl -u XWIKI_USER:XWIKI_PASS http://localhost:8082/v3/api-docs
```

To call secured REST endpoints you must obtain a JWT by authenticating with the
same credentials:

```bash
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"XWIKI_USER","password":"XWIKI_PASS"}'
```

The response returns `accessToken` and `refreshToken`; include the access token
in subsequent requests using the `Authorization: Bearer <token>` header.

## Domain localisation contract

Every HTTP endpoint now requires a `domainLanguage` query parameter. The value
is constrained to the `DomainLanguage` enum (`fr`, `en`) and is echoed back via
the `X-Locale` response header so the frontend can confirm which locale was
resolved. While localisation behaviour is not implemented yet, controllers and
DTOs are annotated to document which fields will eventually depend on this
parameter.

## Building

From this directory run:

```bash
mvn clean install
```

Or build from the repository root:

```bash
mvn -pl nudger-front-api -am clean install
```

## Testing

Execute the tests with:

```bash
mvn test
```

## Observability

Each request is logged with endpoint, status, client IP and user subject. Metrics counters are exposed via Spring Boot Actuator at /actuator/metrics.

## Migration notes

DTO classes such as `ProductDto` are now implemented using Java records. This
means all fields are immutable and accessed via record accessors
(e.g. `dto.base()`). Ensure any custom code or tests no longer rely on
setters.

## Blog service loading

Blog posts are fetched from XWiki by the `BlogService`. Posts are now loaded asynchronously when the application starts, so the API becomes available immediately. See [../docs/blog_service_async_loading_plan.md](../docs/blog_service_async_loading_plan.md) for details.
