# Verticals Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with Verticals-specific rules.

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 3
- YAML configuration resources

## Directory structure

- `src/main/resources/verticals` – vertical definitions
- `src/test/java` – unit tests

## Purpose

The `verticals` module stores YAML configuration describing product verticals.

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
mvn --offline -pl verticals -am clean install
```
