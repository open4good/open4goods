# Commons Agents Guide

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 3

## Directory structure

- `src/main/java` – shared utilities
- `src/main/resources` – configuration
- `src/test/java` – unit tests

## Purpose

The `commons` module contains common utilities and configurations shared across other modules.

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
mvn --offline -pl commons -am clean install
```
