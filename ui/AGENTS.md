# Ui Agents Guide

THIS MODULE IS OUTDATED, A MIGRATION ON A NEW NUXT3 / VUE 3 FRONT IS BEING IMPLEMENTED.
DO NOT WASTE TIME ON THIS SUBPROJECT, IGNORE IT


This module is part of the open4goods multi-module Maven project. 
## Technology

- Java 21
- Spring Boot 3
- Node.js build tools (gulp)

## Directory structure

- `src/main/java` – web controllers
- `src/main/resources` – templates and configuration
- `src/test/java` – unit tests
- `package.json` and `gulpfile.mjs` – frontend build tasks

## Purpose

The `ui` module provides the web user interface built with Thymeleaf and Bootstrap.

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
mvn --offline -pl ui -am clean install
```
