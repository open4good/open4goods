# Nudger Front API - AGENTS Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with front-API-specific rules for Spring Boot 3 and SpringDoc.

## 1. Mission
Provide a **clear and fully documented REST API** for the Nuxt 3 frontend, using Spring Boot 3 and SpringDoc.  
The OpenAPI contract is generated directly from the annotated controllers and DTOs.

---
Main product endpoints are implemented by `ProductController`.

## 2. Architecture (hexagonal + cache)
~~~text
Controller  →  Service  →  Repository/Client
               ↘ cache  ↙
~~~
- **Controller** : HTTP, auth, validation, always returns a `ResponseEntity`.  
- **Service** : orchestration, localisation, statistics, cache abstraction.  
- **Repository/Client** : persistence access or open4goods services.

---

## 3. Tech stack

| Layer     | Choice              | Notes                                   |
|-----------|---------------------|-----------------------------------------|
| JDK       | 21 +                | No preview features                     |
| Framework | Spring Boot 3       | Exclude spring-boot-starter-webflux     |
| Docs      | springdoc-openapi   | Swagger UI auto-enabled                 |
| Build     | Maven multi-module  | **open4goods** parent                   |

---

## 4. Layout
~~~text
src/main/java       ← REST & service code
src/main/resources  ← configuration
src/test/java       ← unit tests
~~~

---

## 5. Build & Test
~~~bash
# Build the module
mvn --offline clean install

# Run only the tests
mvn --offline test

# From the repo root
mvn --offline -pl nudger-front-api -am clean install
~~~

### Configuration properties

The module exposes configurable security settings via
`front.security.*` mapped by `SecurityProperties`:

- `front.security.enabled` – toggle Spring Security.
- `front.security.cors-allowed-hosts` – list of allowed CORS origins.

Rate limiting is configured via `front.rate-limit.*` mapped by
`RateLimitProperties`:

- `front.rate-limit.anonymous` – requests per minute for unauthenticated users.
- `front.rate-limit.authenticated` – requests per minute for authenticated users.

---

## 6. SpringDoc Rules

### 6.1 Controller annotations

| Required               | Why                                 |
|------------------------|-------------------------------------|
| `@Operation`           | summary, description                |
| `@ApiResponse`         | document every HTTP status code     |
| `@Parameter`           | path/query/header parameters        |
| `@RequestBody`         | POST/PUT payload                    |
| `@SecurityRequirement` | auth or captcha                     |
| `@Tag`                 | group controllers                   |

Every controller MUST declare at the class level :
```
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
```

> **Rule**: add `schema = @Schema(...)` for every `@Parameter` to specify the type.
> Missing schemas lead to `UNKNOWN_PARAMETER_NAME` fields in the generated Nuxt client.
>
> Example:
> ```java
> @Parameter(name = "page[number]", in = ParameterIn.QUERY,
>     description = "Zero-based page index",
>     schema = @Schema(type = "integer", minimum = "0"))
> ```

**Example**
~~~java
@Operation(
    summary = "Get product offers",
    description = "List offers for a GTIN, sorted by total price.",
    parameters = {
        @Parameter(
            name = "gtin",
            description = "GTIN 8–14 digits",
            required = true,
            example = "00012345600012")
    },
    responses = {
        @ApiResponse(responseCode = "200", description = "Offers found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OfferDto.class, type = "array"))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    }
)
public ResponseEntity<List<OfferDto>> getOffers(@PathVariable String gtin) { … }
~~~

### 6.4 Domain language contract

- Every exposed controller method **must** declare a required
  `@RequestParam DomainLanguage domainLanguage` query parameter and document it
  in the `@Operation.parameters` array.
- Document the localisation hint by adding an `X-Locale` header to successful
  `@ApiResponse` entries.
- Pass the `DomainLanguage` argument through to the service layer even if it is
  not used yet.

### 6.2 DTO annotations  
*Every field* must have `@Schema`.

~~~java
public record OfferDto(
    @Schema(description = "Merchant name", example = "Amazon")
    String datasourceName,

    @Schema(description = "Displayed offer title", example = "Oral-B Electric Toothbrush")
    String offerName,

    @Schema(description = "Total price", example = "29.99", minimum = "0")
    double price,

    @Schema(description = "ISO 4217 currency", example = "EUR",
            allowableValues = {"EUR", "USD", "GBP"})
    String currency,

    @Schema(description = "Product URL", format = "uri",
            example = "https://example.com/product/123")
    String url
) {}
~~~
*Prefer Java `record` for immutability.*

### 6.3 Enums & nested DTOs
- Enums: either `allowableValues`, or annotate the `enum` class directly.  
- Nested DTOs follow the same rules - no exceptions.

---

## 7. Validation checklist
1. `mvn spring-boot:run`
2. Open `/swagger-ui.html` (requires XWiki credentials via Basic auth)
3. Verify:  
   - all endpoints are listed  
   - each DTO field has description & example  
   - 4xx/5xx responses documented  
   - arrays & nested objects resolved correctly  

---

## 8. Coding standards

| Rule                               | Reason                          |
|------------------------------------|---------------------------------|
| Always return `ResponseEntity`     | Explicit status & headers       |
| Ban `Map<String,Object>`           | Schema clarity                  |
| Mark nullable fields explicitly    | Client safety                   |
| Use constants for descriptions     | DRY & consistency               |
| Test controllers & inner services  | No contract regressions         |
| Controller logging                 | INFO on method entry, WARN for validation issues, ERROR for critical failures |



## 9. Documentation

Follow java / spring documentation best practices :
* Use javadoc at class, field and method level
* Use inline code comments to leverage human
* Keep README.md and AGENTS.md up to date

### 9.1 documentation baseline

* Every class MUST expose detailed Javadoc at class level
  and for each public or private method. Mention parameters, return values and error handling when relevant.
* Add light inline comments when business rules are non obvious. Prefer short, focused comments over redundant prose.

### 9.2 Spring configuration metadata

* Whenever a `@ConfigurationProperties` class is created or updated, mirror its structure in
  `src/main/resources/META-INF/additional-spring-configuration-metadata.json` so IDE auto-completion stays accurate.
* Nested collections should use the `[].property` notation (e.g. `front.partners.mentors.partners[].name`). Include
  defaults and descriptions when available.

---

## 10. Quick references

| Action          | URL / Command        |
|-----------------|----------------------|
| Raw spec        | `GET /v3/api-docs` (Basic auth)  |
| Swagger UI      | `GET /swagger-ui.html` (Basic auth) |
| Build module    | `mvn --offline clean install`  |
| Tests only      | `mvn --offline test`           |

---
## 11. Contract-first workflow
- Les contrôleurs et DTOs sont la **source unique** du contrat OpenAPI.
- Toute évolution utilisée par le frontend doit être implémentée ici (contrôleur ou DTO), puis validée via `mvn --offline clean install`.
- Le fichier `/v3/api-docs/front` obtenu sert ensuite à régénérer le client dans le dépôt `frontend` (`pnpm generate:api`).
- Ne jamais modifier manuellement la spécification ou le code généré.

END
