# Nudger Front API

This module provides the REST endpoints used by the Nuxt 3 frontend. It exposes data aggregated by other open4goods modules and is secured with JWT tokens.

## Default Port

The application runs on **port 8082** by default (`server.port` in `application.yml`). This keeps it separate from the main API on port 8081.

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
