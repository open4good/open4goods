# Backend service integration

## Overview

The frontend talks to the backend through the generated OpenAPI client under
[`shared/api-client`](../shared/api-client). Every downstream call should be
wrapped in a thin service that:

1. Injects the runtime configuration with `createBackendApiConfig()` so the
   `X-Shared-Token` header is always present.
2. Accepts the caller's domain language (`'en' | 'fr'`) and forwards it to the
   backend.
3. Lazily instantiates the generated API so the client only exists on the
   server (or in Vitest) and can be reused across calls.
4. Guards against accidental client-side usage to keep secrets such as
   `MACHINE_TOKEN` out of the browser bundle.
5. Treats every response as domain-sensitive and reuses the shared helper for
   cache headers so CDNs keep hostname-specific variants.

The blog feature provides a good end-to-end example of the pattern: the
[`useBlogService`](../shared/api-client/services/blog.services.ts) wrapper feeds
Nuxt server routes, those routes are consumed by the
[`useBlog`](../app/composables/blog/useBlog.ts) composable, and components such
as [`TheArticle.vue`](../app/components/domains/blog/TheArticle.vue) display the
resulting data. The sections below break that flow down.

## 1. Create a service wrapper (server only)

Service wrappers live next to the generated APIs. They enforce authentication,
domain language propagation, and lazy client instantiation. The blog service is
representative:

```ts
// shared/api-client/services/blog.services.ts
import { BlogApi } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useBlogService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: BlogApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useBlogService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new BlogApi(createBackendApiConfig())
    }

    return api
  }

  const getArticleBySlug = async (slug: string) => {
    return await resolveApi().post({ slug, domainLanguage })
  }

  const getArticles = async (params?: {
    tag?: string
    pageNumber?: number
    pageSize?: number
  }) => {
    return await resolveApi().posts({ ...params, domainLanguage })
  }

  const getTags = async () => {
    return await resolveApi().tags({ domainLanguage })
  }

  return { getArticleBySlug, getArticles, getTags }
}
```

Key points to preserve in every service wrapper:

- **Configuration** - always use `createBackendApiConfig()` so authentication and
  base URLs stay aligned with runtime configuration.
- **Domain language** - accept the language from callers and pass it to every
  OpenAPI call so backend responses match the current hostname.
- **Lazy instantiation** - keep the generated client in a closure and reuse it.
- **Server-only guards** - block browser execution paths; only SSR and Vitest
  may talk to the backend directly.

## 2. Expose the service through a Nuxt server route

Nuxt server routes translate incoming HTTP requests into service calls. The blog
article endpoint mirrors the recommended structure:

```ts
// server/api/blog/articles/[slug].ts
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(async event => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const slug = getRouterParam(event, 'slug')
  if (!slug) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Article slug is required',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const blogService = useBlogService(domainLanguage)

  try {
    return await blogService.getArticleBySlug(slug)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching blog article:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
```

This keeps request validation, caching, localisation, and error translation close
to the network boundary while delegating the backend call to the wrapper. Use
`setDomainLanguageCacheHeaders()` in every server route that surfaces backend
data so the response carries both `Cache-Control` and the host-aware `Vary`
header-mixing manual header calls risks dropping one of them and breaking
multi-domain caching.

## 3. Consume the server route from a composable

Composables hide the transport details from components and pages. They call the
Nuxt server routes with `$fetch`, manage loading/error state, and expose a typed
API. The blog composable demonstrates the pattern:

```ts
// app/composables/blog/useBlog.ts
export const useBlog = () => {
  const currentArticle = useState('blog-current-article', () => null)
  const loading = useState('blog-loading', () => false)
  const error = useState('blog-error', () => null)

  const fetchArticle = async (slug: string) => {
    loading.value = true
    error.value = null

    try {
      const article = await $fetch(`/api/blog/articles/${slug}`)
      currentArticle.value = article
      return article
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch article'
      console.error('Error in fetchArticle:', err)
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    currentArticle: readonly(currentArticle),
    loading: readonly(loading),
    error: readonly(error),
    fetchArticle,
  }
}
```

When a feature needs pagination, filters, or reset helpers, expose them from the
composable so the UI remains declarative.

## 4. Render data in a page or component

With the composable in place, pages and components can focus on presentation.
The blog article page fetches data in `setup()` and passes the resolved article
to [`TheArticle.vue`](../app/components/domains/blog/TheArticle.vue):

```vue
<!-- app/pages/blog/[slug].vue -->
<script setup lang="ts">
const { currentArticle, loading, error, fetchArticle } = useBlog()
const slug = computed(() => /* derive slug from the route */)

await useAsyncData(
  () => (slug.value ? `blog-article-${slug.value}` : 'blog-article'),
  () => (slug.value ? fetchArticle(slug.value) : Promise.resolve(null)),
  { server: true, immediate: true, watch: [slug] },
)

const article = computed(() => currentArticle.value)
</script>

<template>
  <TheArticle v-if="article" :article="article" />
  <v-skeleton-loader
    v-else-if="loading"
    type="heading, image, paragraph, paragraph"
  />
  <v-alert v-else-if="error" type="error" variant="tonal">{{ error }}</v-alert>
</template>
```

`TheArticle.vue` itself focuses purely on rendering: it receives the DTO, builds
computed properties (title, dates, SEO metadata), sanitises HTML, and exposes the
final markup without concerning itself with backend access.

Following this workflow-service wrapper → Nuxt route → composable → component-
keeps backend integrations testable, SSR-safe, and easy to reason about. When
adding a new feature, mirror the blog example so the team can recognise the
control flow instantly.
