# Contribution Service Agents Guide

This microservice provides the contribution (nudges) domain logic and Elasticsearch storage access.

## Technology

- Java 21
- Spring Boot 3

## Directory structure

- `src/main/java` – service code
- `src/main/resources` – configuration and assets
- `src/test/java` – unit tests

## Purpose

Handles contribution votes statistics and persistence.

## Build and test this module only

From this directory:

```bash
mvn --offline clean install
```

Run only the tests with:

```bash
mvn --offline test
```

From the repository root you can also execute:

```bash
mvn --offline -pl services/contribution -am clean install
```
