# Nudger Front API — AGENTS Guide

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
- Nested DTOs follow the same rules — no exceptions.

---

## 7. Validation checklist
1. `mvn spring-boot:run`  
2. Open `/swagger-ui.html`  
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



## 9. Documentation

Follow java / spring documentation best practices :
* Use javadoc at class and at level field
* Use inline code comments to leverage human 
* Keep README.md and AGENTS.md up to date

---

## 10. Quick references

| Action          | URL / Command        |
|-----------------|----------------------|
| Raw spec        | `GET /v3/api-docs`   |
| Swagger UI      | `GET /swagger-ui.html` |
| Build module    | `mvn --offline clean install`  |
| Tests only      | `mvn --offline test`           |

---
## 11. Contract-first workflow
- Les contrôleurs et DTOs sont la **source unique** du contrat OpenAPI.
- Toute évolution utilisée par le frontend doit être implémentée ici (contrôleur ou DTO), puis validée via `mvn --offline clean install`.
- Le fichier `/v3/api-docs/front` obtenu sert ensuite à régénérer le client dans le dépôt `frontend` (`pnpm generate:api`).
- Ne jamais modifier manuellement la spécification ou le code généré.

END
