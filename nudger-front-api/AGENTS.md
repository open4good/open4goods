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

Follow the same strict layering used in the rest of the project:

```
controller → service → repository
```

The controller only handles HTTP concerns and delegates all business logic to the
`service` layer. Services orchestrate caching and data retrieval using repositories.
Repositories provide access to persistence or remote APIs.





## 📘 SpringDoc OpenAPI

### 🎯 Purpose

This agent’s mission is to ensure **high-quality OpenAPI documentation** is automatically generated from Spring-based REST APIs, enabling:

- ✅ clear communication between frontend/backend  
- ✅ strong developer experience for integrators  
- ✅ compatibility with SDK and client code generation  
- ✅ clarity for QA and test automation  

---

## 1. ✅ Controller Annotation Strategy

Always annotate **every public REST endpoint** using the full set of `@Operation`, `@ApiResponse`, and `@Parameter` annotations.

### Example

```java
@Operation(
    summary = "Get product offers",
    description = "Returns a list of commercial offers for a product, sorted by total price.",
    parameters = {
        @Parameter(
            name = "gtin",
            description = "Global Trade Item Number (8 to 14 digits)",
            required = true,
            example = "00012345600012"
        )
    },
    responses = {
        @ApiResponse(responseCode = "200", description = "Offers found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OfferDto.class, type = "array"))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    }
)
```

### Required controller annotations

| Annotation           | Purpose                                                  |
|----------------------|----------------------------------------------------------|
| `@Operation`         | Summary, description, parameters, and linked responses   |
| `@ApiResponse`       | Document all possible HTTP response codes                |
| `@Parameter`         | Clarify path/query/header params with examples           |
| `@RequestBody`       | Document body schemas (POST, PUT)                        |
| `@SecurityRequirement` | Required for any endpoint needing auth/captcha        |
| `@Tag`               | Group controllers logically for Swagger UI navigation    |

---

## 2. ✅ DTO Annotation Strategy

All DTOs (returned or received) must be fully annotated at the field level using `@Schema`.

### Example

```java
public record OfferDto(
    @Schema(description = "Name of the data source (e.g. Amazon)", example = "Amazon")
    String datasourceName,

    @Schema(description = "Offer title as displayed by the merchant", example = "Oral-B Electric Toothbrush")
    String offerName,

    @Schema(description = "Total offer price", example = "29.99", minimum = "0")
    double price,

    @Schema(description = "Currency code (ISO 4217)", example = "EUR", allowableValues = {"EUR", "USD", "GBP"})
    String currency,

    @Schema(description = "Product URL", example = "https://example.com/product/123", format = "uri")
    String url
) {}
```

### Guidelines per field

| Property         | What to include                        |
|------------------|----------------------------------------|
| `description`    | Functional purpose of the field        |
| `example`        | Realistic, usable value                |
| `format`         | e.g. `uri`, `email`, `date-time`       |
| `nullable=true`  | If the field can be null               |
| `minimum`/`maximum` | For numeric values                  |
| `pattern`        | Regex (when applicable)                |
| `allowableValues`| For enums or restricted strings        |

---

## 3. ⚙️ Enum and Nested DTOs

### Enums (option 1)

```java
@Schema(allowableValues = {"EUR", "USD", "GBP"})
String currency;
```

### Enums (option 2)

Annotate the enum class directly:

```java
public enum Currency { EUR, USD, GBP }
```

### Nested DTOs

Every DTO used inside another DTO must also follow the same annotation standards to ensure full schema generation.

---

## 4. 🧪 Local Verification

Start the app:

```bash
mvn spring-boot:run
```

Check output:

- `/v3/api-docs` – raw JSON spec  
- `/swagger-ui.html` – human-readable UI  

### Validate:

- ✅ Every endpoint is documented  
- ✅ All DTO fields have descriptions and examples  
- ✅ Error cases (400, 404, 500) are fully described  
- ✅ All arrays and nested objects are resolved cleanly  

---

## 5. 🔒 Additional Best Practices

| Rule                               | Reason                                         |
|------------------------------------|------------------------------------------------|
| Avoid `Map<String, Object>`        | Prefer typed DTOs for clarity                  |
| Prefer `record` for immutable DTOs | Concise and enforced schema                    |
| Don’t skip nullable/optional fields| Explicitness is key for clients                |
| Define success & error responses   | Prevents incomplete contracts                  |
| Reuse constants for descriptions   | DRY principle, improves consistency            |

---

