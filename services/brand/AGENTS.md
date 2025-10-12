# Brand Service Agents Guide

This microservice hosts brand resolution and scoring logic reused across applications.

## Technology

- Java 21
- Spring Boot 3

## Directory structure

- `src/main/java` – brand domain, services, and repositories
- `src/test/java` – unit tests

## Build and test this module only

From this directory:

```
mvn --offline clean install
```

Run only the tests with:

```
mvn --offline test
```

From the repository root you can also execute:

```
mvn --offline -pl services/brand -am clean install
```
