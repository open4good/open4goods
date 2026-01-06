# Backend API Integration Template

This document provides a template for integrating a new backend API following the project's recommended architecture.

## 4-Layer Architecture

The integration follows the pattern: **Service → Server Route → Composable → Component**

## Example: Categories API Integration

### 1. Create the service wrapper

**File:** `shared/api-client/services/categories.services.ts`

```ts
import { CategoriesApi } from '..'
import type { VerticalConfigDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Categories service for handling category-related API calls
 */
export const useCategoriesService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: CategoriesApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useCategoriesService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new CategoriesApi(createBackendApiConfig())
    }

    return api
  }

  /**
   * Fetch categories optionally filtered by enabled status
   * @param onlyEnabled - Filter only enabled categories
   * @returns Promise<VerticalConfigDto[]>
   */
  const getCategories = async (
    onlyEnabled?: boolean
  ): Promise<VerticalConfigDto[]> => {
    try {
      return await resolveApi().categories1({ domainLanguage, onlyEnabled })
    } catch (error) {
      console.error('Error fetching categories:', error)
      throw error
    }
  }

  /**
   * Fetch a single category by ID
   * @param categoryId - Category identifier
   * @returns Promise<VerticalConfigFullDto>
   */
  const getCategoryById = async (
    categoryId: string
  ): Promise<VerticalConfigFullDto> => {
    try {
      return await resolveApi().category({ categoryId, domainLanguage })
    } catch (error) {
      console.error(`Error fetching category ${categoryId}:`, error)
      throw error
    }
  }

  return { getCategories, getCategoryById }
}
```

**Key points:**

- Inject `createBackendApiConfig()` for authentication
- Required `domainLanguage` parameter
- Server-side guard (throws error if called client-side)
- Lazy API instantiation
- Error handling with rethrow for upstream handlers

### 2. Create the Nuxt server route

**File:** `server/api/categories/index.ts`

```ts
import { getQuery } from 'h3'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { VerticalConfigDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

/**
 * Categories API endpoint
 * Handles GET requests for categories with caching
 */
export default defineEventHandler(
  async (event): Promise<VerticalConfigDto[]> => {
    // Set cache headers for 1 hour
    setResponseHeader(
      event,
      'Cache-Control',
      'public, max-age=3600, s-maxage=3600'
    )

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const categoriesService = useCategoriesService(domainLanguage)
    const query = getQuery(event)
    const onlyEnabledParam = Array.isArray(query.onlyEnabled)
      ? query.onlyEnabled[0]
      : query.onlyEnabled

    const onlyEnabled = onlyEnabledParam === 'true' || onlyEnabledParam === true

    try {
      const response = await categoriesService.getCategories(onlyEnabled)
      return response
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching categories:',
        backendError.logMessage,
        backendError
      )

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
```

**File:** `server/api/categories/[id].ts` (optional for parameterized routes)

```ts
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { VerticalConfigFullDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

export default defineEventHandler(
  async (event): Promise<VerticalConfigFullDto> => {
    setResponseHeader(
      event,
      'Cache-Control',
      'public, max-age=3600, s-maxage=3600'
    )

    const categoryId = getRouterParam(event, 'id')
    if (!categoryId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Category ID is required',
      })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const categoriesService = useCategoriesService(domainLanguage)

    try {
      return await categoriesService.getCategoryById(categoryId)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching category:',
        backendError.logMessage,
        backendError
      )

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
```

**Key points:**

- Appropriate cache headers
- Resolve `domainLanguage` from headers
- Parse and validate query params
- Standardized error handling with `extractBackendErrorDetails()`

### 3. Create the composable

**File:** `app/composables/categories/useCategories.ts`

```ts
import type {
  VerticalConfigDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'

/**
 * Composable for categories-related functionality
 */
export const useCategories = () => {
  // Reactive state
  const categories = useState<VerticalConfigDto[]>('categories-list', () => [])
  const currentCategory = useState<VerticalConfigFullDto | null>(
    'categories-current',
    () => null
  )
  const loading = useState('categories-loading', () => false)
  const error = useState<string | null>('categories-error', () => null)

  /**
   * Fetch categories from the backend proxy
   * @param onlyEnabled - Filter only enabled categories
   */
  const fetchCategories = async (onlyEnabled: boolean = true) => {
    loading.value = true
    error.value = null

    try {
      const response = await $fetch<VerticalConfigDto[]>('/api/categories', {
        params: { onlyEnabled },
      })

      categories.value = response ?? []
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch categories'
      console.error('Error in fetchCategories:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single category by ID
   * @param categoryId - Category identifier
   * @returns The fetched category or null if not found
   */
  const fetchCategory = async (categoryId: string) => {
    loading.value = true
    error.value = null

    try {
      const category = await $fetch<VerticalConfigFullDto>(
        `/api/categories/${categoryId}`
      )
      currentCategory.value = category
      return category
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch category'
      console.error('Error in fetchCategory:', err)
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Clear current category
   */
  const clearCurrentCategory = () => {
    currentCategory.value = null
  }

  /**
   * Clear error state
   */
  const clearError = () => {
    error.value = null
  }

  return {
    // State (readonly)
    categories: readonly(categories),
    currentCategory: readonly(currentCategory),
    loading: readonly(loading),
    error: readonly(error),

    // Actions
    fetchCategories,
    fetchCategory,
    clearCurrentCategory,
    clearError,
  }
}
```

**Key points:**

- Reactive state with `useState` for SSR
- Readonly state exposure
- Fetch methods using `$fetch` to server routes
- Loading and error handling
- Cleanup methods (clear)

### 4. Use in a component

**File:** `app/components/domains/home/sections/The-section-items-slide.vue`

```vue
<template>
  <The-slide :items="categoryItems" />
</template>

<script setup lang="ts">
const { categories, loading, error, fetchCategories } = useCategories()

// Fetch categories on component mount
await useAsyncData('home-categories-slide', () => fetchCategories(), {
  server: true,
  immediate: true,
})

// Transform categories to items format expected by The-slide
const categoryItems = computed(() => {
  return categories.value.map(category => ({
    // Adapt based on what The-slide expects
    // Example if it expects image URLs:
    image: category.icon || category.image,
    title: category.name,
    id: category.id,
    // ... other props
  }))
})
</script>
```

**Key points:**

- Use `useAsyncData` for SSR
- Immediate fetch on mount
- Data transformation to adapt to presentation component
- Separation of business logic and presentation

## Integration Checklist

- [ ] Service wrapper created in `shared/api-client/services/`
  - [ ] Import generated API from `..`
  - [ ] Import `createBackendApiConfig`
  - [ ] `domainLanguage` parameter
  - [ ] Server-side runtime guard
  - [ ] Lazy instantiation
  - [ ] Error handling with rethrow
- [ ] Server route created in `server/api/`
  - [ ] Cache headers
  - [ ] `domainLanguage` resolution
  - [ ] Parameter validation
  - [ ] Service call
  - [ ] Error handling with `extractBackendErrorDetails`
- [ ] Composable created in `app/composables/`
  - [ ] Reactive state with `useState`
  - [ ] Readonly exposure
  - [ ] Fetch methods using `$fetch`
  - [ ] Loading/error handling
- [ ] Component updated
  - [ ] Import and use composable
  - [ ] `useAsyncData` for SSR
  - [ ] Data transformation if needed
  - [ ] Loading/error states handling

## Reference

See [docs/backend-services.md](../backend-services.md) for complete pattern documentation.
