# Model Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with Model-specific rules.

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 3

## Directory structure

- `src/main/java` – domain model classes
- `src/main/resources` – configuration
- `src/test/java` – unit tests

## Purpose

The `model` module defines domain objects shared across other modules.

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
mvn --offline -pl model -am clean install
```
