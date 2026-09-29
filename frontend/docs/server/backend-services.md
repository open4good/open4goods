# Backend service integration

## Overview

The frontend calls Java services through the generated OpenAPI client in
[`shared/api-client`](../../shared/api-client). Wrap each backend client in a
server-only service which injects `createBackendApiConfig()`, receives a
`DomainLanguage`, and lazily creates the generated API. This keeps the machine
token out of browser code.

Blog content is deliberately different: it is served from the Nuxt Content
collection by the routes under [`server/api/blog`](../../server/api/blog), not
by a generated front-api client. The routes preserve the existing DTO response
shapes so [`useBlog`](../../app/composables/blog/useBlog.ts) and its components
remain independent of the content source.

## Blog route pattern

Use the local content helpers and apply host-aware cache headers at the route
boundary. The article route validates its slug, resolves the request language,
and maps the content document to the stable response DTO:

```ts
const slug = getRouterParam(event, 'slug')
if (!slug) {
  throw createError({ statusCode: 400, statusMessage: 'Article slug is required' })
}

setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')
const doc = await findBlogDocBySlug(event, slug)
if (!doc) {
  throw createError({ statusCode: 404, statusMessage: 'Article not found' })
}
return await toBlogPostDto(doc)
```

## Consume the route from a composable

Composables hide transport details from pages and components. They call the
Nuxt route with `$fetch`, manage loading and error state, and expose typed data.
See [`useBlog`](../../app/composables/blog/useBlog.ts) for the pagination, tags,
and article-loading implementation.

Pages and components should only render the resolved DTO. This keeps content
storage, cache policy, and presentation independently testable.
