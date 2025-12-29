# Icecat Service Refactoring & Extension Plan

## Executive Summary

Refactor the Icecat integration to consolidate HTTP concerns, add robust testing, and extend with OAuth 2.0-protected Retailer API for categories.

---

## Phase 1 — Cleanup & Harden (No Behavior Change)

### 1.1 Create Centralized HTTP Client Infrastructure

**Goal**: Isolate all HTTP concerns into a single, testable component.

**Files to Create**:
```
services/icecat/src/main/java/org/open4goods/icecat/
├── client/
│   ├── IcecatHttpClient.java          # Centralized HTTP client
│   ├── IcecatHttpClientConfig.java    # Client configuration (timeouts, retries)
│   └── exception/
│       ├── IcecatApiException.java    # Base exception
│       ├── IcecatRateLimitException.java
│       └── IcecatAuthenticationException.java
```

**Implementation Details**:
- Use Spring's `RestClient` (Spring 6.1+) or `WebClient` for modern HTTP handling
- Configure connection pool, timeouts (connect: 10s, read: 30s)
- Implement retry logic with exponential backoff (3 retries, 1s/2s/4s delays)
- Add request/response logging at DEBUG level
- Thread-safe, singleton bean

**Current Code to Refactor**:
- `RemoteFileCachingService.downloadTo()` (Basic Auth downloads) → delegate to `IcecatHttpClient`
- `IcecatCompletionService` line 154: `IOUtils.toString(new URL(url))` → use `IcecatHttpClient`

---

### 1.2 Eliminate Code Duplication

**Problem**: `getCachedFile()` is duplicated in 3 places:
- `IcecatService.java:190-214`
- `FeatureLoader.java:123-143`
- `CategoryLoader.java:179-199`

**Solution**: Extract to a shared utility in `IcecatHttpClient`:

```java
public interface IcecatHttpClient {
    // For authenticated gzipped XML file downloads
    File downloadAndDecompressGzip(String url, String cacheKey);

    // For JSON API calls
    <T> T get(String url, Class<T> responseType);
    <T> T get(String url, Class<T> responseType, Map<String, String> headers);
}
```

**Refactored Flow**:
```
FeatureLoader/CategoryLoader/IcecatService
    └── IcecatHttpClient.downloadAndDecompressGzip()
            ├── Check cache (file exists + not expired)
            ├── Download with Basic Auth
            ├── Decompress GZIP
            └── Return cached file path
```

---

### 1.3 Add Comprehensive Test Coverage

**Test Strategy**:

| Layer | Tool | Coverage Target |
|-------|------|-----------------|
| Unit Tests | JUnit 5 + Mockito | Service logic, DTOs |
| HTTP Mocking | WireMock | API responses, error scenarios |
| Contract Tests | JSON snapshots | Response structure validation |

**Test Files to Create**:
```
services/icecat/src/test/java/org/open4goods/icecat/
├── client/
│   └── IcecatHttpClientTest.java       # HTTP client unit tests
├── services/
│   ├── IcecatServiceTest.java          # Enhance existing
│   ├── IcecatServiceIntegrationTest.java  # WireMock-based
│   └── loader/
│       ├── FeatureLoaderTest.java
│       └── CategoryLoaderTest.java
├── testdata/
│   └── (JSON/XML snapshots)
```

**WireMock Test Example**:
```java
@WireMockTest(httpPort = 8089)
class IcecatServiceIntegrationTest {
    @Test
    void shouldLoadFeaturesFromMockedEndpoint() {
        stubFor(post("/features_list.xml.gz")
            .willReturn(aResponse()
                .withBodyFile("features_sample.xml.gz")));
        // Assert feature loading works
    }

    @Test
    void shouldRetryOnTransientFailure() {
        stubFor(post("/features_list.xml.gz")
            .inScenario("retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("SUCCESS"));
        // ...
    }
}
```

**Dependencies to Add** (pom.xml):
```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.3.1</version>
    <scope>test</scope>
</dependency>
```

---

### 1.4 Improve Error Handling

**Current Issues**:
- `IcecatCompletionService:168-174`: String parsing of exception messages
- Silent failures in loaders (catch + log, continue)

**Improvements**:
- Create typed exceptions hierarchy
- Proper HTTP status code handling
- Structured error responses

```java
public class IcecatApiException extends RuntimeException {
    private final int statusCode;
    private final String errorBody;
}

public class IcecatRateLimitException extends IcecatApiException {
    private final Duration retryAfter;
}
```

---

### 1.5 Configuration Cleanup

**Current State**: `IcecatConfiguration` uses Basic Auth only.

**Enhanced Configuration** (add to existing class):
```java
@ConfigurationProperties(prefix = "icecat-feature-config")
public class IcecatConfiguration {
    // Existing fields...

    // New fields for Retailer API (Phase 2 prep)
    private String retailerApiBaseUrl = "https://retailer-api.icecat.biz";
    private String tokenEndpoint;
    private String clientId;
    private String clientSecret;
    private String oauthUsername;
    private String oauthPassword;
    private String organizationId;

    // HTTP client settings
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 30000;
    private int maxRetries = 3;
    private int rateLimitRequestsPerSecond = 5;
}
```

---

### 1.6 Naming & Code Quality

**Rename for Clarity**:
| Current | Proposed | Reason |
|---------|----------|--------|
| `iceCatConfig` | `icecatConfig` | Consistent casing |
| `remoteCachingFolder` | `cacheDirectory` | Clearer purpose |
| `politnessDelayMs` | `politeDelayMs` | Fix typo |
| `getCachedFile()` | removed | Moved to client |

**Remove Deprecated Code**:
- Unused `@Service` annotations on loaders (they're not Spring-scanned)
- Dead code paths (empty catch blocks)

---

## Phase 2 — Add Categories Integration

### 2.1 OAuth 2.0 Token Management

**New Component**: `IcecatOAuthTokenManager`

```java
public class IcecatOAuthTokenManager {
    private volatile String accessToken;
    private volatile Instant tokenExpiry;

    public String getValidToken() {
        if (isTokenExpired()) {
            synchronized(this) {
                if (isTokenExpired()) {
                    refreshToken();
                }
            }
        }
        return accessToken;
    }

    private void refreshToken() {
        // POST to token endpoint with:
        // - client_id, client_secret, grant_type=password, username, password
        // - Content-Type: application/x-www-form-urlencoded
    }
}
```

**Token Endpoint**: `POST {{Url}}/cdm-cedemo-authenticationservice/connect/token`

**Request Body** (form-urlencoded):
```
client_id=xxx&client_secret=xxx&grant_type=password&username=xxx&password=xxx
```

---

### 2.2 Rate Limiting Implementation

**Strategy**: Token bucket algorithm with 5 req/s limit

```java
public class IcecatRateLimiter {
    private final RateLimiter limiter = RateLimiter.create(5.0); // Guava

    public void acquire() {
        limiter.acquire();
    }
}
```

**HTTP 429 Handling**:
```java
if (response.statusCode() == 429) {
    String retryAfter = response.headers().firstValue("Retry-After").orElse("1");
    throw new IcecatRateLimitException(Duration.ofSeconds(Long.parseLong(retryAfter)));
}
```

---

### 2.3 GetCategories Endpoint Implementation

**New Files**:
```
services/icecat/src/main/java/org/open4goods/icecat/
├── client/
│   └── IcecatRetailerApiClient.java    # OAuth-protected API client
├── model/retailer/
│   ├── RetailerCategory.java           # DTO for API response
│   └── RetailerCategoriesResponse.java
└── services/
    └── RetailerCategoryService.java    # Business logic
```

**API Call**:
```
GET https://retailer-api.icecat.biz/TradeItem/GetCategories
Headers:
  - Authorization: Bearer {token}
  - organizationId: {value}
```

**DTO Model** (based on typical Icecat response):
```java
public record RetailerCategory(
    @JsonProperty("CategoryId") Long categoryId,
    @JsonProperty("CategoryName") String categoryName,
    @JsonProperty("ParentCategoryId") Long parentCategoryId,
    @JsonProperty("Level") Integer level,
    @JsonProperty("Description") String description
) {}

public record RetailerCategoriesResponse(
    @JsonProperty("Categories") List<RetailerCategory> categories,
    @JsonProperty("TotalCount") Integer totalCount
) {}
```

---

### 2.4 Front-API Exposition

**New Controller**: `IcecatCategoriesController`

```
front-api/src/main/java/org/open4goods/nudgerfrontapi/controller/
└── IcecatCategoriesController.java
```

```java
@RestController
@RequestMapping("/icecat")
@Tag(name = "Icecat", description = "Icecat integration endpoints")
public class IcecatCategoriesController {

    @GetMapping("/categories")
    @Operation(summary = "Get Icecat retailer categories")
    public ResponseEntity<List<RetailerCategoryDto>> getCategories(
        @RequestParam DomainLanguage domainLanguage
    ) {
        // Return categories from RetailerCategoryService
    }
}
```

**DTO for Frontend**:
```java
public record RetailerCategoryDto(
    @Schema(description = "Icecat category ID") Long id,
    @Schema(description = "Category name") String name,
    @Schema(description = "Parent category ID") Long parentId,
    @Schema(description = "Hierarchy level") Integer level
) {}
```

---

### 2.5 Test Coverage for Phase 2

**Mock Responses** (based on Postman):

`src/test/resources/__files/categories_response.json`:
```json
{
  "Categories": [
    {
      "CategoryId": 1,
      "CategoryName": "Electronics",
      "ParentCategoryId": null,
      "Level": 0
    },
    {
      "CategoryId": 234,
      "CategoryName": "Televisions",
      "ParentCategoryId": 1,
      "Level": 1
    }
  ],
  "TotalCount": 2
}
```

`src/test/resources/__files/token_response.json`:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

**WireMock Tests**:
```java
@Test
void shouldFetchCategoriesWithOAuth() {
    // Stub token endpoint
    stubFor(post("/cdm-cedemo-authenticationservice/connect/token")
        .willReturn(okJson(tokenResponse)));

    // Stub categories endpoint
    stubFor(get("/TradeItem/GetCategories")
        .withHeader("Authorization", matching("Bearer .*"))
        .withHeader("organizationId", equalTo("test-org"))
        .willReturn(okJson(categoriesResponse)));

    List<RetailerCategory> categories = client.getCategories();
    assertThat(categories).hasSize(2);
}

@Test
void shouldHandleRateLimiting() {
    stubFor(get("/TradeItem/GetCategories")
        .willReturn(aResponse()
            .withStatus(429)
            .withHeader("Retry-After", "2")));

    assertThatThrownBy(() -> client.getCategories())
        .isInstanceOf(IcecatRateLimitException.class)
        .hasFieldOrPropertyWithValue("retryAfter", Duration.ofSeconds(2));
}
```

---

## Implementation Order

### Phase 1 (Cleanup) - Tasks in Order:

1. **Add WireMock dependency** to `pom.xml`
2. **Create exception hierarchy** (`IcecatApiException`, etc.)
3. **Create `IcecatHttpClient`** with Basic Auth support
4. **Write tests for `IcecatHttpClient`** (WireMock)
5. **Refactor `FeatureLoader`** to use new client
6. **Refactor `CategoryLoader`** to use new client
7. **Refactor `IcecatService`** to remove duplicated code
8. **Refactor `IcecatCompletionService`** to use proper HTTP client
9. **Add integration tests** for service layer
10. **Clean up naming and remove dead code**

### Phase 2 (Categories) - Tasks in Order:

1. **Extend `IcecatConfiguration`** with OAuth properties
2. **Create `IcecatOAuthTokenManager`**
3. **Create `IcecatRateLimiter`**
4. **Create `IcecatRetailerApiClient`**
5. **Write tests for OAuth flow** (WireMock)
6. **Create `RetailerCategory` DTOs**
7. **Create `RetailerCategoryService`**
8. **Write tests for category service**
9. **Create front-api controller and DTOs**
10. **Generate OpenAPI client** (`pnpm generate:api`)
11. **End-to-end testing**

---

## File Change Summary

### New Files:
| Path | Purpose |
|------|---------|
| `icecat/client/IcecatHttpClient.java` | Centralized HTTP client |
| `icecat/client/IcecatRetailerApiClient.java` | OAuth API client |
| `icecat/client/IcecatOAuthTokenManager.java` | Token management |
| `icecat/client/IcecatRateLimiter.java` | Rate limiting |
| `icecat/client/exception/*.java` | Exception hierarchy |
| `icecat/model/retailer/*.java` | Retailer API DTOs |
| `icecat/services/RetailerCategoryService.java` | Business logic |
| `front-api/controller/IcecatCategoriesController.java` | API endpoint |
| `front-api/dto/RetailerCategoryDto.java` | Frontend DTO |

### Modified Files:
| Path | Changes |
|------|---------|
| `icecat/pom.xml` | Add WireMock, Guava dependencies |
| `icecat/config/yml/IcecatConfiguration.java` | Add OAuth + HTTP settings |
| `icecat/services/IcecatService.java` | Remove `getCachedFile()`, use client |
| `icecat/services/loader/FeatureLoader.java` | Use `IcecatHttpClient` |
| `icecat/services/loader/CategoryLoader.java` | Use `IcecatHttpClient` |
| `api/services/completion/IcecatCompletionService.java` | Use proper HTTP client |

### Test Files:
| Path | Coverage |
|------|----------|
| `icecat/client/IcecatHttpClientTest.java` | HTTP client unit tests |
| `icecat/client/IcecatRetailerApiClientTest.java` | OAuth + rate limiting |
| `icecat/services/IcecatServiceIntegrationTest.java` | WireMock integration |
| `icecat/services/RetailerCategoryServiceTest.java` | Category service |
| `front-api/controller/IcecatCategoriesControllerTest.java` | API endpoint |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking existing XML downloads | Keep `RemoteFileCachingService` as fallback; test extensively |
| Token expiry during long operations | Implement token refresh with buffer (refresh 5 min before expiry) |
| Rate limit bursts | Use token bucket with smooth rate; queue requests if needed |
| Network instability | Retry with exponential backoff; circuit breaker pattern |

---

## Definition of Done

### Phase 1:
- [ ] All existing tests pass
- [ ] New unit tests achieve >80% coverage on modified code
- [ ] WireMock integration tests for all HTTP scenarios
- [ ] No code duplication in `getCachedFile()` pattern
- [ ] Centralized HTTP client handles all Icecat HTTP traffic

### Phase 2:
- [ ] OAuth token retrieval and refresh working
- [ ] Categories endpoint accessible via front-api
- [ ] Rate limiting enforced (5 req/s)
- [ ] HTTP 429 handled with Retry-After
- [ ] Frontend can consume categories via generated API client
- [ ] Integration tests for full OAuth + categories flow
