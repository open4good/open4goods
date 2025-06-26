# Nudger Front API

This module provides the REST endpoints used by the Nuxt 3 frontend. It exposes data aggregated by other open4goods modules and is secured with JWT tokens.
The former `/api/v1/search` endpoint was removed from this module.
Use the main `api` service for search operations.

## Default Port

The application runs on **port 8082** by default (`server.port` in `application.yml`). This keeps it separate from the main API on port 8081.

## Security

Security is enabled by default using JWT authentication. For local testing it can be disabled by setting
`front.security.enabled=false` in `application.yml` or via an environment variable.
To allow the Nuxt frontend running on a different host during development, configure the list of allowed
CORS origins using the `front.security.cors-allowed-hosts` property. By default it permits requests from
`http://localhost:8082`.

## Local development tips

When starting the application locally you may be tempted to call the beta API
(`https://beta.front-api.nudger.fr`). That server does not set CORS headers for
`http://localhost:8082`, which leads the browser to reject the requests. Ensure
your frontend uses the local API base URL instead:

```bash
export API_URL=http://localhost:8082
```

With this configuration all calls stay on the same origin and the built-in CORS
rules apply correctly.

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
