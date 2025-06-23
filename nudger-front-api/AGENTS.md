# Nudger-front-api Agents Guide

This module is part of the open4goods multi-module Maven project. It aims at delivering the API consumed by the nuxt 3 / vue 3 frontend, which is fully based on the open api provided by this project.

this project uses commons dependencies (model, search service, micro services dependencies) in order to provide a clean, localized, and UX / UI (nuxt) friendly

General layer is :

* Exposition controller, with cached abstraction
* Call view rendering service
* view rendering service operates data retrieving through open4goods modules, data filtering, data localisation, audit and stats, with cache abstraction


## Technology

- Java 21+
- Spring Boot 3

## Directory structure

- `src/main/java` – REST API code
- `src/main/resources` – configuration
- `src/test/java` – unit tests

## Purpose

The `nudger-front-api` module exposes the websit REST API protected with JWT tokens.

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
mvn -pl nudger-front-api -am clean install
```


## Guidelines

TODO : Explain and challenge the request/response pattern. I want to scale it on other system objects

TODO : Explain and challenge the "MVC" spring architecture : controller -> rendering service -> repository
