# Nudger Front API

This module provides the REST endpoints used by the Nuxt 3 frontend. It exposes
data aggregated by other open4goods modules and is secured with JWT tokens.

DTOs located under `org.open4goods.nudgerfrontapi.dto` are written as Java
**records** and each field is annotated with `@Schema` to keep the generated
OpenAPI contract in sync with the code.

## Default Port

The application runs on **port 8082** by default (`server.port` in `application.yml`). This keeps it separate from the main API on port 8081.

## Security

Security is enabled by default using JWT authentication. For local testing it can be disabled by setting
`front.security.enabled=false` in `application.yml` or via an environment variable.

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
