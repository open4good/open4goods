# Admin Agents Guide

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 3 (Spring Boot Admin)

## Directory structure

- `src/main/java` – application code
- `src/main/resources` – configuration
- `src/test/java` – unit tests

## Purpose

The `admin` module provides the administration console based on Spring Boot Admin to monitor open4goods services.

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
mvn -pl admin -am clean install
```
