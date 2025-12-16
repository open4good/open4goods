# Crawler Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with Crawler-specific rules.

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 3

## Directory structure

- `src/main/java` – crawler implementation
- `src/main/resources` – configuration
- `src/test/java` – unit tests
- `libs` – third party crawler libs

## Purpose

The `crawler` module retrieves product information from external sources and feeds the open4goods data pipeline.

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
mvn --offline -pl crawler -am clean install
```
