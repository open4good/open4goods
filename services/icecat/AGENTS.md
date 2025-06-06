# Icecat Service Agents Guide

This microservice is part of the open4goods project.

## Technology

- Java 21
- Spring Boot 3

## Directory structure

- `src/main/java` – service code
- `src/main/resources` – configuration and assets
- `src/test/java` – unit tests

## Purpose

Provides access to Icecat data and related configuration.

## Build and test this module only

From this directory:

```bash
mvn clean install
```

Run only the tests with:

```bash
mvn test
```

From the repository root you can also execute:

```bash
mvn -pl services/icecat -am clean install
```
