# Services – Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide documents conventions for all microservices under `/services/`.

---

## 1. Overview

All microservices in this directory follow the root [AGENTS.md](../AGENTS.md) conventions. This guide consolidates common patterns and documents service-specific details.

---

## 2. Common Technology Stack

| Component | Standard |
|-----------|----------|
| **JDK** | Java 21 |
| **Framework** | Spring Boot 3.x |
| **Build tool** | Maven (part of multi-module reactor) |
| **Testing** | JUnit 5, AssertJ |

---

## 3. Standard Directory Structure

All services follow this layout:

```
services/<service-name>/
├─ src/main/java          ← Service implementation
├─ src/main/resources     ← Configuration and assets
├─ src/test/java          ← Unit tests
└─ pom.xml                ← Maven configuration
```

---

## 4. Build and Test Commands

### From a service directory

```bash
# Build and install
mvn --offline clean install

# Run tests only
mvn --offline test
```

### From repository root

```bash
# Build specific service with dependencies
mvn --offline -pl services/<service-name> -am clean install

# Test specific service
mvn --offline -pl services/<service-name> test
```

---

## 5. Service-Specific Guides

Each service has unique responsibilities and may have specific conventions beyond the common patterns above.

### blog

**Purpose**: Handles blog posts fetched from XWiki and generates RSS feeds.

**Key Responsibilities**:
- Fetch blog content from XWiki API
- Transform XWiki markup to web-friendly formats
- Generate RSS/Atom feeds
- Cache blog content appropriately

---

### brand

**Purpose**: Brand resolution and scoring logic reused across applications.

**Key Responsibilities**:
- Resolve brand names from product data
- Calculate brand reputation scores
- Maintain brand database
- Provide brand enrichment APIs

---

### captcha

**Purpose**: Handles captcha verification.

**Key Responsibilities**:
- Integrate with captcha providers (reCAPTCHA, hCaptcha, etc.)
- Verify captcha tokens
- Rate limiting and abuse prevention

**Security Guidelines**:
- Never log captcha tokens or user responses
- Implement token expiration validation
- Use environment variables for API keys

---

### contribution

**Purpose**: Manages user contributions to product data.

**Key Responsibilities**:
- Accept user-submitted product information
- Validate and moderate contributions
- Track contribution history
- Reward/gamification logic

---

### evaluation

**Purpose**: Product evaluation and scoring logic.

**Key Responsibilities**:
- Calculate product scores across multiple dimensions
- Aggregate ratings and reviews
- Apply evaluation algorithms
- Generate comparative assessments

---

### favicon

**Purpose**: Favicon fetching and caching service.

**Key Responsibilities**:
- Fetch favicons from merchant websites
- Cache favicon images
- Provide fallback icons
- Optimize icon formats and sizes

---

### feedservice

**Purpose**: RSS/Atom feed generation and management.

**Key Responsibilities**:
- Generate product feeds
- Support multiple feed formats (RSS, Atom, JSON Feed)
- Filter and customize feeds per vertical
- Handle feed pagination

---

### github-feedback

**Purpose**: GitHub integration for user feedback and issue tracking.

**Key Responsibilities**:
- Create GitHub issues from user feedback
- Sync feedback status with GitHub
- Authenticate with GitHub API
- Map feedback categories to issue labels

**Configuration**:
- Requires GitHub personal access token
- Configure repository and label mappings

---

### googlesearch

**Purpose**: Integrates with Google Custom Search API.

**Key Responsibilities**:
- Execute Google Custom Search queries
- Parse and normalize search results
- Handle API quotas and rate limiting
- Cache search results

**Configuration**:
- Requires Google Custom Search API key
- Configure search engine ID
- Monitor API usage and quotas

---

### google-indexation

**Purpose**: Publishes product URLs to the Google Indexing API after AI review generation.

**Key Responsibilities**:
- Authenticate with Google service accounts
- Publish URL_UPDATED notifications
- Expose actuator metrics and health checks
- Support batch submissions and retries

**Configuration**:
- Requires Google Indexing API service account credentials
- Configure API URL, batch sizes, and request timeouts

---

### gtinservice

**Purpose**: GTIN (Global Trade Item Number) validation and lookup.

**Key Responsibilities**:
- Validate GTIN-8, GTIN-12, GTIN-13, GTIN-14 formats
- Calculate and verify check digits
- Normalize GTIN formats
- Provide GTIN metadata

---

### icecat

**Purpose**: Integration with Icecat product catalog.

**Key Responsibilities**:
- Fetch product data from Icecat API
- Transform Icecat data to internal model
- Handle Icecat authentication
- Cache product specifications

**Configuration**:
- Requires Icecat API credentials
- Configure data synchronization schedules

---

### image-processing

**Purpose**: Image processing and optimization service.

**Key Responsibilities**:
- Resize and crop product images
- Generate thumbnails and responsive variants
- Optimize image formats (WebP, AVIF)
- Apply watermarks if needed
- Extract image metadata

**Performance**:
- Use async processing for large batches
- Implement queue-based architecture
- Monitor memory usage

---

### opendata

**Purpose**: Integration with open data sources.

**Key Responsibilities**:
- Fetch data from public open data APIs
- Transform open data to internal formats
- Schedule periodic data updates
- Handle various open data standards

---

### product-repository

**Purpose**: Stores and retrieves product data.

**Key Responsibilities**:
- Persist product entities
- Provide CRUD operations for products
- Implement search and filtering
- Manage product relationships
- Handle product versioning

**Data Access**:
- Primary repository for product domain
- May use Elasticsearch, MongoDB, or relational DB
- Implement proper indexing strategies

---

### prompt

**Purpose**: Interacts with generative AI using prompt templates.

**Key Responsibilities**:
- Manage AI prompt templates
- Execute prompts against LLM APIs (OpenAI, Anthropic, etc.)
- Handle prompt versioning
- Track token usage and costs
- Implement retry logic and fallbacks

**AI Guidelines**:
- All prompts must be versioned in `src/main/resources/prompts/`
- Use structured logging for token usage and costs
- Implement rate limiting per API key
- Never log full prompt responses (may contain PII)
- Cache responses when appropriate
- Monitor for prompt injection attempts

**Configuration**:
- Requires LLM API keys (environment variables)
- Configure model selection and parameters
- Set token limits and timeouts

---

### exposed-docs

**Purpose**: Exposes embedded documentation and prompt resources for browsing.

**Key Responsibilities**:
- Index embedded `.md`, `.prompt`, and documentation assets from the classpath
- Provide navigation trees, content retrieval, and search endpoints
- Support configurable categories and optional authentication

**Configuration**:
- Configure resource categories and extensions via `exposed-docs.*`
- Toggle public access and security with `exposed-docs.security.enabled`

---

### remotefilecaching

**Purpose**: Remote file fetching and caching service.

**Key Responsibilities**:
- Fetch remote files (images, documents, etc.)
- Implement multi-level caching (memory, disk, CDN)
- Handle cache invalidation
- Provide cache statistics
- Manage storage quotas

**Performance**:
- Use async downloads
- Implement circuit breakers for unreliable sources
- Monitor cache hit rates

---

### review-generation

**Purpose**: Generates product reviews using AI.

**Key Responsibilities**:
- Generate synthetic product reviews
- Ensure review quality and diversity
- Apply content moderation
- Track generated content metadata

**AI/Content Guidelines**:
- All generated reviews must be marked as AI-generated
- Implement quality scoring for generated content
- Use diverse prompt templates to avoid repetition
- Apply content filters for inappropriate language
- Store generation metadata (model, prompt version, timestamp)

**Ethical Considerations**:
- Clearly distinguish AI-generated from human reviews
- Implement disclosure mechanisms
- Follow platform policies on synthetic content

---

### serialisation

**Purpose**: Data serialization and format conversion service.

**Key Responsibilities**:
- Convert between data formats (JSON, XML, CSV, etc.)
- Handle schema validation
- Provide serialization utilities
- Support versioned data formats

---

### embedding-djl

**Purpose**: Shared DJL-based embedding starter.

**Key Responsibilities**:
- Provide reusable auto-configuration for text embeddings.
- Centralise DJL model loading and health checks for downstream services.
- Expose configuration properties for local vs remote models so applications fail fast when assets are missing.

---

### urlfetching

**Purpose**: URL fetching with retry logic and error handling.

**Key Responsibilities**:
- Fetch content from URLs with robust error handling
- Implement exponential backoff retry logic
- Handle redirects and authentication
- Respect robots.txt and rate limits
- Provide user-agent configuration

**Best Practices**:
- Use connection pooling
- Implement circuit breakers
- Log failed requests for debugging
- Monitor response times and error rates

---

### xwiki-spring-boot-starter

**Purpose**: Spring Boot starter for XWiki integration.

**Key Responsibilities**:
- Provide auto-configuration for XWiki clients
- Simplify XWiki API integration
- Handle XWiki authentication
- Provide common XWiki utilities

**Usage**:
- Add as dependency to services needing XWiki integration
- Configure via `application.yml` or `application.properties`
- Follow Spring Boot starter conventions

---

### geocode

**Purpose**: Offline city geocoding and distance lookup service backed by GeoNames cities5000.txt.

**Key Responsibilities**:
- Download and cache GeoNames cities5000.zip via `RemoteFileCachingService`
- Parse and index cities into in-memory lookup maps for fast geocoding
- Expose REST APIs to resolve city + country and compute distances
- Provide health status based on successful dataset load

**Configuration**:
- Requires cache directory for `RemoteFileCachingService`
- Uses GeoNames download URL `https://download.geonames.org/export/dump/cities5000.zip`

---

## 6. Common Service Patterns

### 6.1 Configuration

All services should:
- Use `@ConfigurationProperties` for external configuration
- Provide sensible defaults
- Document all configuration properties in `additional-spring-configuration-metadata.json`
- Support environment variable overrides

### 6.2 Error Handling

- Use custom exceptions extending from common base exceptions
- Return RFC 9457 Problem Detail responses for REST APIs
- Log errors with appropriate severity levels
- Implement circuit breakers for external dependencies

### 6.3 Testing

- Write unit tests for business logic
- Use Testcontainers for integration tests requiring external services
- Mock external APIs in unit tests
- Maintain test coverage above 70%

### 6.4 Logging

- Use SLF4J with parameterized messages
- Include correlation IDs for distributed tracing
- Log at appropriate levels (DEBUG, INFO, WARN, ERROR)
- Never log sensitive data (passwords, tokens, PII)

### 6.5 Security

- Never hardcode credentials
- Use Spring Security where appropriate
- Validate all inputs
- Implement rate limiting for public APIs
- Follow OWASP security guidelines

---

## 7. Adding a New Service

When creating a new microservice:

1. **Create directory structure** following the standard layout
2. **Add to parent POM** in `/services/pom.xml`
3. **Create service-specific section** in this guide
4. **Document**:
   - Purpose (one-line description)
   - Key responsibilities
   - Specific conventions or guidelines
   - Configuration requirements
   - Security considerations (if applicable)
5. **Follow root conventions** from [AGENTS.md](../AGENTS.md)

---

## 8. Questions?

For service-specific questions, consult this guide first. For project-wide conventions, see the [root AGENTS.md](../AGENTS.md). For architectural decisions, open a GitHub Discussion.
