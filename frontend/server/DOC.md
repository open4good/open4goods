# Server Architecture Documentation

## Overview

This server follows **Clean Architecture** (also known as **Hexagonal Architecture** or **Ports & Adapters**) with **Domain-Driven Design (DDD)** principles and **CQRS** pattern for read operations.

## Architecture Principles

### Key Benefits

1. **Separation of Concerns**: Each layer has a single responsibility
2. **Testability**: Business logic is independent of infrastructure
3. **Maintainability**: Easy to understand and modify
4. **Flexibility**: Can swap implementations without changing business logic
5. **Type Safety**: Domain entities are independent of DTOs

### Dependency Rule

Dependencies flow **inward**:

```
Presentation → Application → Domain
     ↓              ↓           ↑
Infrastructure ─────┘           │
     └──────────────────────────┘
```

- **Domain** has no dependencies (pure business logic)
- **Application** depends only on Domain
- **Infrastructure** depends on Domain (implements interfaces)
- **Presentation** orchestrates everything

## Folder Structure

```
server/
├── domain/                      # Business Logic (Core)
│   ├── blog/
│   │   ├── entities/           # Domain entities with business logic
│   │   │   ├── Article.ts      # Blog article entity
│   │   │   ├── Tag.ts          # Blog tag entity
│   │   │   └── Page.ts         # Pagination entity
│   │   ├── repositories/       # Repository interfaces (Ports)
│   │   │   └── IBlogRepository.ts
│   │   └── services/           # Domain services (optional)
│   └── content/
│       ├── entities/
│       │   └── ContentBloc.ts
│       └── repositories/
│           └── IContentRepository.ts
│
├── application/                 # Use Cases (CQRS)
│   ├── blog/
│   │   ├── queries/            # Query objects
│   │   │   ├── GetArticlesQuery.ts
│   │   │   ├── GetArticleBySlugQuery.ts
│   │   │   └── GetTagsQuery.ts
│   │   └── handlers/           # Query handlers (use cases)
│   │       ├── GetArticlesHandler.ts
│   │       ├── GetArticleBySlugHandler.ts
│   │       └── GetTagsHandler.ts
│   └── content/
│       ├── queries/
│       │   └── GetBlocQuery.ts
│       └── handlers/
│           └── GetBlocHandler.ts
│
├── infrastructure/              # External Concerns (Adapters)
│   ├── repositories/           # Repository implementations
│   │   ├── HttpBlogRepository.ts
│   │   └── HttpContentRepository.ts
│   ├── http/                   # HTTP client configuration
│   ├── cache/                  # Cache strategies
│   └── i18n/                   # Internationalization
│
├── presentation/                # HTTP Layer
│   ├── api/                    # API handlers (placed in server/api/)
│   │   ├── blog/
│   │   │   ├── articles.ts     # GET /api/blog/articles
│   │   │   ├── [slug].ts       # GET /api/blog/articles/:slug
│   │   │   └── tags.ts         # GET /api/blog/tags
│   │   └── blocs/
│   │       └── [blocId].ts     # GET /api/blocs/:blocId
│   ├── middleware/             # Reusable middleware
│   │   ├── errorHandler.ts     # Domain error to HTTP error converter
│   │   ├── cacheHeaders.ts     # Cache strategy helpers
│   │   └── languageDetector.ts # Language detection from headers
│   └── validators/             # Input validation
│
├── shared/                      # Shared Utilities
│   ├── errors/                 # Custom error classes
│   │   ├── DomainError.ts      # Base error class
│   │   ├── ValidationError.ts  # 400 errors
│   │   ├── NotFoundError.ts    # 404 errors
│   │   └── InfrastructureError.ts # 500 errors
│   ├── types/                  # Common types
│   │   └── Result.ts           # Result pattern (success/failure)
│   └── di/                     # Dependency Injection
│       ├── container.ts        # DI container
│       └── providers.ts        # Service registration
│
└── routes/                      # Legacy auth routes (not refactored yet)
    └── auth/
```

## Layer Responsibilities

### 1. Domain Layer (Business Logic)

**Purpose**: Contains pure business logic, no framework dependencies.

**Components**:

- **Entities**: Core business objects with behavior
  - Example: `Article`, `Tag`, `ContentBloc`
  - Factory functions for creation
  - Business rules methods

- **Repository Interfaces** (Ports): Define data access contracts
  - Example: `IBlogRepository`, `IContentRepository`
  - Return `Result<T, DomainError>` for functional error handling

**Example - Article Entity**:

```typescript
export interface Article {
  id: string
  slug: string
  title: string
  // ... other fields
}

export const createArticle = (data: RawData): Article => {
  // Validation and transformation
}

export const isRecentArticle = (article: Article): boolean => {
  // Business logic
}
```

### 2. Application Layer (Use Cases)

**Purpose**: Orchestrates business logic, implements use cases.

**Components**:

- **Queries**: Read operation requests (CQRS pattern)
  - Example: `GetArticlesQuery`, `GetBlocQuery`
  - Simple data structures

- **Handlers**: Execute use cases
  - Validate input
  - Call domain services/repositories
  - Return `Result<T, DomainError>`

**Example - Handler**:

```typescript
export class GetArticlesHandler {
  constructor(private readonly repository: IBlogRepository) {}

  async handle(
    query: GetArticlesQuery
  ): Promise<Result<Page<Article>, DomainError>> {
    // 1. Validate/sanitize input
    const pageSize = Math.min(Math.max(1, query.pageSize ?? 10), 100)

    // 2. Delegate to repository
    return await this.repository.getArticles({ pageSize })
  }
}
```

### 3. Infrastructure Layer (Technical Details)

**Purpose**: Implements interfaces, handles external systems.

**Components**:

- **Repository Implementations**: Connect to external APIs
  - Example: `HttpBlogRepository` implements `IBlogRepository`
  - Transforms DTOs to domain entities
  - Handles HTTP errors → domain errors

**Example - Repository**:

```typescript
export class HttpBlogRepository implements IBlogRepository {
  private readonly blogService: ReturnType<typeof useBlogService>

  constructor(domainLanguage: DomainLanguage) {
    this.blogService = useBlogService(domainLanguage)
  }

  async getArticles(params): Promise<Result<Page<Article>, DomainError>> {
    try {
      const dto = await this.blogService.getArticles(params)
      const articles = dto.data.map(createArticle) // Transform DTO → Entity
      return success(createPage(articles, dto.page))
    } catch (error) {
      return failure(this.handleError(error)) // HTTP error → Domain error
    }
  }
}
```

### 4. Presentation Layer (HTTP Interface)

**Purpose**: Handle HTTP requests/responses, orchestrate use cases.

**Components**:

- **API Handlers**: Nuxt API routes
  - Detect language
  - Register dependencies (DI)
  - Apply cache headers
  - Parse query/route params
  - Execute handler
  - Convert Result → HTTP response

- **Middleware**: Reusable HTTP utilities
  - `errorHandler`: Domain errors → H3 errors
  - `cacheHeaders`: Apply cache strategies
  - `languageDetector`: Extract language from host header

**Example - API Handler**:

```typescript
export default defineEventHandler(async (event): Promise<Page<Article>> => {
  try {
    // 1. Setup
    const domainLanguage = detectLanguage(event)
    registerProviders(domainLanguage)
    applyCacheHeaders(event, CacheStrategies.ONE_HOUR)

    // 2. Parse input
    const query = getQuery(event)
    const pageNumber = query.pageNumber ? parseInt(query.pageNumber) : undefined

    // 3. Execute use case
    const handler = getHandler<GetArticlesHandler>(
      SERVICE_KEYS.GET_ARTICLES_HANDLER
    )
    const result = await handler.handle({ pageNumber })

    // 4. Handle result
    if (isSuccess(result)) {
      return result.value
    }
    handleDomainError(result.error, event)
  } catch (error) {
    handleUnknownError(error, event)
  }
})
```

## Key Patterns

### Result Pattern

Functional error handling without throwing exceptions:

```typescript
type Result<T, E> = Success<T> | Failure<E>

// Usage
const result = await repository.getArticles(params)
if (isSuccess(result)) {
  return result.value // Type: Page<Article>
} else {
  handleError(result.error) // Type: DomainError
}
```

### Dependency Injection

Simple container-based DI:

```typescript
// Registration (providers.ts)
container.register(
  SERVICE_KEYS.BLOG_REPOSITORY,
  () => new HttpBlogRepository(language)
)

container.register(
  SERVICE_KEYS.GET_ARTICLES_HANDLER,
  () => new GetArticlesHandler(container.get(SERVICE_KEYS.BLOG_REPOSITORY))
)

// Usage (API handler)
const handler = getHandler<GetArticlesHandler>(
  SERVICE_KEYS.GET_ARTICLES_HANDLER
)
```

### Error Hierarchy

```
DomainError (abstract)
├── ValidationError (400)
├── NotFoundError (404)
└── InfrastructureError (500)
```

Each error has:

- `code`: Machine-readable error code
- `statusCode`: HTTP status
- `message`: Human-readable message
- `toJSON()`: Serialization

## Data Flow Example

**Request**: `GET /api/blog/articles?pageNumber=1&pageSize=10`

1. **Presentation Layer** (`server/api/blog/articles.ts`)
   - Detect language from Host header
   - Register DI providers
   - Apply cache headers
   - Parse query params

2. **DI Container** (`shared/di/providers.ts`)
   - Resolve `GetArticlesHandler`
   - Inject `HttpBlogRepository`

3. **Application Layer** (`GetArticlesHandler`)
   - Validate pageNumber/pageSize
   - Call repository

4. **Infrastructure Layer** (`HttpBlogRepository`)
   - Call OpenAPI client (`useBlogService`)
   - Transform DTOs → Domain entities
   - Handle errors

5. **Domain Layer** (`Article`, `Page` entities)
   - Factory functions create valid entities
   - Business logic methods available

6. **Response Flow** (back up)
   - Repository returns `Result<Page<Article>, DomainError>`
   - Handler returns result to API handler
   - API handler unwraps Result → HTTP response or error

## Frontend Integration

The frontend continues to use composables that call `/api/*` endpoints:

```typescript
// app/composables/blog/useBlog.ts
const fetchArticles = async (page: number) => {
  const response = await $fetch<Page<Article>>('/api/blog/articles', {
    params: { pageNumber: page },
  })
  return response
}
```

**Benefits**:

- Frontend code unchanged
- SSR-friendly
- Type-safe (shares domain entities)
- Cache headers applied automatically

## Testing Strategy

### Unit Tests

**Domain Layer**:

```typescript
// Test pure business logic
test('isRecentArticle returns true for articles within 7 days', () => {
  const article = createArticle({ publishedAt: new Date() })
  expect(isRecentArticle(article)).toBe(true)
})
```

**Application Layer**:

```typescript
// Test handlers with mocked repositories
test('GetArticlesHandler validates pageSize', async () => {
  const mockRepo: IBlogRepository = {
    /* mock */
  }
  const handler = new GetArticlesHandler(mockRepo)

  const result = await handler.handle({ pageSize: 1000 })
  // Verify pageSize was capped at 100
})
```

### Integration Tests

**Infrastructure Layer**:

```typescript
// Test repository with real or mocked HTTP client
test('HttpBlogRepository transforms DTOs correctly', async () => {
  const repo = new HttpBlogRepository('en-US')
  const result = await repo.getArticles({})

  if (isSuccess(result)) {
    expect(result.value.data[0]).toHaveProperty('slug')
  }
})
```

## Migration Status

- ✅ **Blog module**: Fully migrated to Clean Architecture
- ✅ **Content module**: Fully migrated
- ⏸️ **Auth module**: Not migrated (uses legacy routes/auth/)
  - Reason: Cookie handling is simple enough, DDD would add overhead

## Adding a New Feature

### Example: Add "Featured Articles" endpoint

1. **Domain** (`server/domain/blog/entities/Article.ts`):

```typescript
export const isFeatured = (article: Article): boolean => {
  return article.tags.includes('featured')
}
```

2. **Application** (`server/application/blog/queries/GetFeaturedArticlesQuery.ts`):

```typescript
export interface GetFeaturedArticlesQuery {
  limit?: number
}
```

3. **Application** (`server/application/blog/handlers/GetFeaturedArticlesHandler.ts`):

```typescript
export class GetFeaturedArticlesHandler {
  constructor(private readonly repository: IBlogRepository) {}

  async handle(query: GetFeaturedArticlesQuery) {
    const result = await this.repository.getArticles({ tag: 'featured' })
    // ... filter and limit
    return result
  }
}
```

4. **Infrastructure** (optional, if new repository method needed)
   - Update `IBlogRepository` interface
   - Implement in `HttpBlogRepository`

5. **DI** (`server/shared/di/providers.ts`):

```typescript
const SERVICE_KEYS = {
  // ...
  GET_FEATURED_ARTICLES_HANDLER: 'blog.handlers.getFeaturedArticles',
}

// In registerProviders()
container.register(
  SERVICE_KEYS.GET_FEATURED_ARTICLES_HANDLER,
  () =>
    new GetFeaturedArticlesHandler(container.get(SERVICE_KEYS.BLOG_REPOSITORY))
)
```

6. **Presentation** (`server/api/blog/featured.ts`):

```typescript
export default defineEventHandler(async event => {
  const domainLanguage = detectLanguage(event)
  registerProviders(domainLanguage)
  applyCacheHeaders(event, CacheStrategies.FIVE_MINUTES)

  const handler = getHandler<GetFeaturedArticlesHandler>(
    SERVICE_KEYS.GET_FEATURED_ARTICLES_HANDLER
  )
  const result = await handler.handle({ limit: 5 })

  if (isSuccess(result)) return result.value
  handleDomainError(result.error, event)
})
```

## Best Practices

1. **Keep domain pure**: No framework imports in `domain/`
2. **Use Result pattern**: Avoid throwing in business logic
3. **Validate at boundaries**: Application handlers validate input
4. **Transform at boundaries**: Infrastructure transforms DTOs → Entities
5. **Single responsibility**: Each handler does one thing
6. **Immutable entities**: Entities are readonly data structures
7. **Factory functions**: Use `createArticle()` instead of constructors
8. **Type safety**: Use TypeScript strictly throughout

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
