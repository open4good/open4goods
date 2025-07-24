# Static Service Agents Guide

This module exposes a minimal Spring Boot application dedicated to serving static
resources such as images under a dedicated domain.

## Build and Test

```bash
mvn --offline clean install
```

Run tests only:

```bash
mvn --offline test
```

From the repository root you can also execute:

```bash
mvn --offline -pl static -am clean install
```
