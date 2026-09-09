# UI Module - Deployed static and open-data service

> **Status**: This legacy Thymeleaf module remains deployed behind `static.nudger.fr`.
> Do not remove or bypass it without a WorkOrder that replaces each responsibility.

The module is the current producer of product and brand images, dynamic sitemaps and
the open-data download. New customer-facing UI belongs in `frontend`, while fixes and
migrations for these deployed responsibilities remain valid work here.

## Technology and structure

- Java 21 and Spring Boot 4
- Thymeleaf plus the existing Node/gulp asset build
- Java sources under `src/main/java`, resources under `src/main/resources` and tests
  under `src/test/java`

Use JavaLens and the Maven MCP servers as required by the root guide. Preserve the
controller-to-service-to-repository layering and add tests for every behavior change.

## Verification

From this directory:

```bash
mvn --offline clean install
```

From the repository root:

```bash
mvn --offline -pl ui -am clean install
```
