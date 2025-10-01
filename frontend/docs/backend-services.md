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

The pattern keeps the generated code untouched while providing a stable surface
for server routes, composables, and tests.

## Creating a service wrapper
Suppose you need to expose operations from the generated
[`CategoriesApi`](../shared/api-client/apis/CategoriesApi.ts). The service lives
next to the other wrappers in `shared/api-client/services/`:

```ts
// shared/api-client/services/categories.services.ts
import { CategoriesApi } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useCategoriesService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: CategoriesApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useCategoriesService() is only available on the server runtime.')
    }

    if (!api) {
      api = new CategoriesApi(createBackendApiConfig())
    }

    return api
  }

  const list = async (onlyEnabled?: boolean) => {
    return await resolveApi().categories1({ domainLanguage, onlyEnabled })
  }

  const findById = async (categoryId: string) => {
    return await resolveApi().category({ categoryId, domainLanguage })
  }

  return { list, findById }
}
```

Key points:

- **Configuration** – `createBackendApiConfig()` builds a `Configuration` seeded
  with `config.apiUrl` and `config.machineToken`. Never instantiate the generated
  API with `new Configuration()` directly; the helper ensures authentication is
  consistent and only available on the server.
- **Domain language** – services receive the resolved domain language and pass it
  to each OpenAPI call. This keeps backend responses aligned with the current
  hostname and mirrors how [`useContentService`](../shared/api-client/services/content.services.ts)
  behaves.
- **Lazy instantiation** – `resolveApi()` creates the generated client only once,
  caching it inside the closure so subsequent calls reuse the same instance.
- **Server-only guards** – the `import.meta.server`/`process.env.VITEST` checks
  prevent browser bundles from importing backend clients and allow Vitest suites
  to use them during SSR-style tests.

## Wiring the service into a Nuxt server route
Nuxt server routes act as the integration layer between browser requests and the
backend. [`server/api/blocs/[blocId].ts`](../server/api/blocs/%5BblocId%5D.ts)
shows the full lifecycle:

1. **Validate parameters** – read dynamic params with `getRouterParam()` and
   return a 400 error early when required values are missing.
2. **Set caching headers** – use `setResponseHeader()` to define shared cache
   behaviour (`public, max-age=3600` in the bloc example).
3. **Resolve the domain language** – feed the incoming `host`/`x-forwarded-host`
   header to `resolveDomainLanguage()` so backend calls receive the correct
   locale context.
4. **Call the service** – instantiate your wrapper (`useCategoriesService`,
   `useContentService`, etc.) with the domain language and execute the desired
   method.
5. **Translate backend errors** – wrap the call in a `try/catch`, feed failures
   to [`extractBackendErrorDetails()`](../server/utils/log-backend-error.ts), log
   the `logMessage`, and rethrow with `createError({ statusCode, statusMessage })`
   so Nuxt responds with meaningful HTTP codes.

```ts
export default defineEventHandler(async (event) => {
  const categoryId = getRouterParam(event, 'categoryId')
  if (!categoryId) {
    throw createError({ statusCode: 400, statusMessage: 'Category id is required' })
  }

  setResponseHeader(event, 'Cache-Control', 'public, max-age=300, s-maxage=300')
  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const categoriesService = useCategoriesService(domainLanguage)

  try {
    return await categoriesService.findById(categoryId)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching category', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
```

Reusing the same pattern across handlers keeps error logging consistent and
ensures downstream services always receive authenticated, locale-aware requests.
When adding query parameters or headers, extend the service wrapper so the
handler remains focused on request validation and response shaping.
