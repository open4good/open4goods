# UI Module - DEPRECATED ⚠️

> **Status**: This module is being replaced by [frontend](../frontend) (Nuxt 3 / Vue 3).  
> **Action**: Do not add new features. For current UI development, see [frontend/AGENTS.md](../frontend/AGENTS.md).

## Migration Status

This legacy UI module (Thymeleaf + Bootstrap) is being phased out in favor of the modern Nuxt 3 frontend. All new UI development should occur in the `frontend` module.

**Migration Progress**:
- ✅ Blog integration → `frontend/composables/useBlog.ts`
- ✅ Product pages → `frontend/pages/products/`
- 🔄 User authentication flows → In progress
- ⏳ Admin panels → Planned

This module is part of the open4goods multi-module Maven project. 
## Technology

- Java 21
- Spring Boot 4
- Node.js build tools (gulp)

## Directory structure

- `src/main/java` - web controllers
- `src/main/resources` - templates and configuration
- `src/test/java` - unit tests
- `package.json` and `gulpfile.mjs` - frontend build tasks

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
