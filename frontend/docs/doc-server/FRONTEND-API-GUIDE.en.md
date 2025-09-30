# Frontend API Integration Guide

Complete guide for frontend developers on integrating API calls into Vue/Nuxt 3 components.

## Table of Contents

1. [General Architecture](#general-architecture)
2. [Using Composables](#using-composables)
3. [Direct API Calls with $fetch](#direct-api-calls-with-fetch)
4. [SSR Management with useAsyncData](#ssr-management-with-useasyncdata)
5. [Error Handling](#error-handling)
6. [Practical Examples by Module](#practical-examples-by-module)
7. [Best Practices](#best-practices)

---

## General Architecture

### Data Flow

```
Vue Component
    ↓
Composable (e.g., useBlog)
    ↓
API Endpoint (/api/blog/articles)
    ↓
Backend Server (Clean Architecture)
    ↓
Java Backend API
```

### Architecture Benefits

- **SSR-friendly**: Data is loaded server-side on first render
- **Type-safe**: TypeScript ensures type safety
- **Automatic caching**: Cache headers defined server-side
- **Centralized error handling**: Errors are normalized
- **Shared state**: Composables use `useState` to share state

---

## Using Composables

Composables are the **recommended** method for interacting with APIs. They encapsulate state logic and API calls.

### Available Composables

#### 1. useBlog() - Blog Module

**Location**: `app/composables/blog/useBlog.ts`

**Exposed State**:
```typescript
const {
  // State
  articles,          // BlogPostDto[] - List of articles
  currentArticle,    // BlogPostDto | null - Current article
  tags,              // BlogTagDto[] - List of tags
  selectedTag,       // string | null - Selected tag
  loading,           // boolean - Loading in progress
  error,             // string | null - Error message
  pagination,        // { page, size, totalElements, totalPages }

  // Actions
  fetchArticles,     // (page?, size?, tag?) => Promise<void>
  changePage,        // (page: number) => Promise<void>
  fetchTags,         // () => Promise<void>
  selectTag,         // (tag: string | null) => Promise<void>
  fetchArticle,      // (slug: string) => Promise<BlogPostDto | null>
  clearCurrentArticle, // () => void
  clearError,        // () => void
} = useBlog()
```

**Usage Example in a Page**:

```vue
<script setup lang="ts">
const { articles, loading, error, pagination, fetchArticles, changePage } = useBlog()

// Load articles on component mount
onMounted(async () => {
  await fetchArticles(1, 12) // page 1, 12 articles per page
})

// Change page
const handlePageChange = async (newPage: number) => {
  await changePage(newPage)
}
</script>

<template>
  <div>
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error">{{ error }}</v-alert>

    <div v-else>
      <article-card
        v-for="article in articles"
        :key="article.id"
        :article="article"
      />

      <v-pagination
        :length="pagination.totalPages"
        :model-value="pagination.page"
        @update:model-value="handlePageChange"
      />
    </div>
  </div>
</template>
```

**Filtering by Tag**:

```vue
<script setup lang="ts">
const { articles, tags, selectedTag, selectTag, fetchTags } = useBlog()

onMounted(async () => {
  await fetchTags()
})

const handleTagClick = async (tag: string) => {
  await selectTag(tag) // Automatically reloads articles
}
</script>

<template>
  <div>
    <v-chip-group>
      <v-chip
        v-for="tag in tags"
        :key="tag.slug"
        :color="selectedTag === tag.slug ? 'primary' : 'default'"
        @click="handleTagClick(tag.slug)"
      >
        {{ tag.name }} ({{ tag.count }})
      </v-chip>
    </v-chip-group>

    <!-- Filtered article list -->
  </div>
</template>
```

#### 2. useContentBloc() - Content Module

**Location**: `app/composables/content/useContentBloc.ts`

**Exposed State**:
```typescript
const {
  // State
  bloc,              // XwikiContentBlocDto | null - Content block
  loading,           // boolean - Loading in progress
  error,             // string | null - Error message

  // Actions
  fetchBloc,         // (blocId: string) => Promise<void>
  clearBloc,         // () => void
} = useContentBloc()
```

**Usage Example**:

```vue
<script setup lang="ts">
const props = defineProps<{
  blocId: string
}>()

const { bloc, loading, error, fetchBloc } = useContentBloc()

// Load block on mount
onMounted(async () => {
  await fetchBloc(props.blocId)
})

// Reload if blocId changes
watch(() => props.blocId, async (newBlocId) => {
  if (newBlocId) {
    await fetchBloc(newBlocId)
  }
})
</script>

<template>
  <div>
    <v-skeleton-loader v-if="loading" type="article" />
    <v-alert v-else-if="error" type="error">{{ error }}</v-alert>
    <text-content v-else-if="bloc" :content="bloc.content" />
  </div>
</template>
```

#### 3. useAuth() - Authentication Module

**Location**: `app/composables/useAuth.ts`

**Exposed State**:
```typescript
const {
  // State (from useAuthStore)
  isAuthenticated,   // boolean - User authenticated
  user,              // User | null - User info
  roles,             // string[] - User roles

  // Actions
  login,             // (username, password) => Promise<void>
  logout,            // () => Promise<void>
  refreshToken,      // () => Promise<void>
  hasRole,           // (role: string) => boolean
  hasAnyRole,        // (roles: string[]) => boolean
} = useAuth()
```

**Usage Example**:

```vue
<script setup lang="ts">
const { isAuthenticated, user, login, logout } = useAuth()
const username = ref('')
const password = ref('')
const error = ref<string | null>(null)

const handleLogin = async () => {
  try {
    await login(username.value, password.value)
    navigateTo('/dashboard')
  } catch (err) {
    error.value = 'Invalid credentials'
  }
}
</script>

<template>
  <div v-if="!isAuthenticated">
    <v-text-field v-model="username" label="Email" />
    <v-text-field v-model="password" type="password" label="Password" />
    <v-btn @click="handleLogin">Login</v-btn>
    <v-alert v-if="error" type="error">{{ error }}</v-alert>
  </div>

  <div v-else>
    <p>Welcome {{ user?.name }}</p>
    <v-btn @click="logout">Logout</v-btn>
  </div>
</template>
```

**Role-Based Access Control**:

```vue
<script setup lang="ts">
const { hasRole, hasAnyRole } = useAuth()

const canEdit = computed(() => hasRole('ROLE_SITEEDITOR'))
const isAdmin = computed(() => hasAnyRole(['XWIKIADMINGROUP', 'ROLE_ADMIN']))
</script>

<template>
  <div>
    <v-btn v-if="canEdit" @click="editContent">Edit</v-btn>
    <v-btn v-if="isAdmin" @click="openAdminPanel">Administration</v-btn>
  </div>
</template>
```

---

## Direct API Calls with $fetch

For simple cases where a composable is not necessary, use `$fetch` directly.

### Basic Syntax

```typescript
const data = await $fetch<ReturnType>('/api/endpoint', {
  method: 'GET', // GET, POST, PUT, DELETE, etc.
  params: { key: 'value' }, // Query parameters
  body: { data: 'value' },  // Request body (POST/PUT)
  headers: { 'Custom-Header': 'value' },
})
```

### Examples

#### Simple GET

```vue
<script setup lang="ts">
import type { BlogTagDto } from '~~/shared/api-client'

const tags = ref<BlogTagDto[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    tags.value = await $fetch<BlogTagDto[]>('/api/blog/tags')
  } catch (err) {
    console.error('Error loading tags', err)
  } finally {
    loading.value = false
  }
})
</script>
```

#### GET with Parameters

```vue
<script setup lang="ts">
import type { PageDto } from '~~/shared/api-client'

const articles = ref<PageDto | null>(null)

const loadArticles = async (page: number, tag?: string) => {
  articles.value = await $fetch<PageDto>('/api/blog/articles', {
    params: {
      pageNumber: page - 1, // 0-based
      pageSize: 12,
      tag: tag || undefined,
    },
  })
}
</script>
```

#### POST with Body

```vue
<script setup lang="ts">
const submitForm = async (formData: FormData) => {
  const response = await $fetch('/api/contact/submit', {
    method: 'POST',
    body: {
      name: formData.name,
      email: formData.email,
      message: formData.message,
    },
  })

  console.log('Form submitted', response)
}
</script>
```

---

## SSR Management with useAsyncData

For **optimal SSR compatibility**, use `useAsyncData` or `useFetch`. These composables automatically handle:
- Server-side loading (SSR)
- Caching
- Loading state
- Reactivity

### useAsyncData

**Ideal for**: API calls requiring transformation or business logic.

```vue
<script setup lang="ts">
import type { BlogPostDto } from '~~/shared/api-client'

const route = useRoute()
const slug = computed(() => route.params.slug as string)

// useAsyncData automatically handles SSR, cache, and reactivity
const { data: article, pending, error, refresh } = await useAsyncData(
  `blog-article-${slug.value}`, // Unique cache key
  () => $fetch<BlogPostDto>(`/api/blog/articles/${slug.value}`),
  {
    watch: [slug], // Reload if slug changes
    server: true,  // Execute server-side (SSR)
    lazy: false,   // Wait for loading before rendering
  }
)
</script>

<template>
  <div>
    <v-skeleton-loader v-if="pending" type="article" />
    <v-alert v-else-if="error" type="error">{{ error.message }}</v-alert>
    <article-detail v-else-if="article" :article="article" />

    <v-btn @click="refresh">Refresh</v-btn>
  </div>
</template>
```

### useFetch

**Ideal for**: Simple API calls without transformation.

```vue
<script setup lang="ts">
import type { BlogTagDto } from '~~/shared/api-client'

// useFetch is a shortcut for useAsyncData + $fetch
const { data: tags, pending, error } = await useFetch<BlogTagDto[]>('/api/blog/tags', {
  key: 'blog-tags',
  server: true,
})
</script>

<template>
  <v-chip-group v-if="tags">
    <v-chip v-for="tag in tags" :key="tag.slug">
      {{ tag.name }}
    </v-chip>
  </v-chip-group>
</template>
```

### Important Options

| Option | Type | Description |
|--------|------|-------------|
| `key` | `string` | Unique cache key (required) |
| `server` | `boolean` | Execute server-side (SSR) - `true` by default |
| `lazy` | `boolean` | Don't block navigation - `false` by default |
| `immediate` | `boolean` | Execute immediately - `true` by default |
| `watch` | `Array` | Refs to watch for reload |
| `transform` | `Function` | Transform data before returning |

---

## Error Handling

### Errors in Composables

Composables automatically handle errors and store them in `error.value`.

```vue
<script setup lang="ts">
const { articles, error, fetchArticles } = useBlog()

onMounted(async () => {
  await fetchArticles()
})
</script>

<template>
  <v-alert v-if="error" type="error" dismissible>
    {{ error }}
  </v-alert>
</template>
```

### Errors with $fetch

Use `try/catch` to handle errors manually.

```vue
<script setup lang="ts">
const loadData = async () => {
  try {
    const data = await $fetch('/api/endpoint')
    // Process data
  } catch (err: any) {
    console.error('API Error', err)

    // Access error details
    if (err.statusCode === 404) {
      console.error('Resource not found')
    } else if (err.statusCode === 401) {
      console.error('Not authenticated')
      navigateTo('/login')
    } else {
      console.error('Server error', err.statusMessage)
    }
  }
}
</script>
```

### Errors with useAsyncData

Errors are available in `error.value`.

```vue
<script setup lang="ts">
const { data, error } = await useAsyncData('key', () => $fetch('/api/endpoint'))

// Display custom message based on error code
const errorMessage = computed(() => {
  if (!error.value) return null

  switch (error.value.statusCode) {
    case 404: return 'Resource not found'
    case 401: return 'You must login'
    case 403: return 'Access denied'
    default: return 'An error occurred'
  }
})
</script>

<template>
  <v-alert v-if="error" type="error">
    {{ errorMessage }}
  </v-alert>
</template>
```

---

## Practical Examples by Module

### Blog Module

#### Article List Page

```vue
<!-- app/pages/blog/index.vue -->
<script setup lang="ts">
const {
  articles,
  loading,
  error,
  pagination,
  tags,
  selectedTag,
  fetchArticles,
  changePage,
  fetchTags,
  selectTag
} = useBlog()

// Load initial data
onMounted(async () => {
  await Promise.all([
    fetchArticles(1, 12),
    fetchTags()
  ])
})

const handlePageChange = async (page: number) => {
  await changePage(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleTagFilter = async (tag: string | null) => {
  await selectTag(tag)
}
</script>

<template>
  <v-container>
    <h1>Blog</h1>

    <!-- Tag filters -->
    <v-chip-group class="mb-4">
      <v-chip
        :color="!selectedTag ? 'primary' : 'default'"
        @click="handleTagFilter(null)"
      >
        All
      </v-chip>
      <v-chip
        v-for="tag in tags"
        :key="tag.slug"
        :color="selectedTag === tag.slug ? 'primary' : 'default'"
        @click="handleTagFilter(tag.slug)"
      >
        {{ tag.name }} ({{ tag.count }})
      </v-chip>
    </v-chip-group>

    <!-- Article list -->
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error">{{ error }}</v-alert>

    <v-row v-else>
      <v-col
        v-for="article in articles"
        :key="article.id"
        cols="12"
        md="6"
        lg="4"
      >
        <article-card :article="article" />
      </v-col>
    </v-row>

    <!-- Pagination -->
    <v-pagination
      v-if="pagination.totalPages > 1"
      :length="pagination.totalPages"
      :model-value="pagination.page"
      @update:model-value="handlePageChange"
      class="mt-4"
    />
  </v-container>
</template>
```

#### Article Detail Page

```vue
<!-- app/pages/blog/[slug].vue -->
<script setup lang="ts">
const route = useRoute()
const router = useRouter()
const { currentArticle, loading, error, fetchArticle } = useBlog()

const slug = computed(() => {
  const rawSlug = route.params.slug
  if (Array.isArray(rawSlug)) return rawSlug[0]
  return rawSlug
})

// Use useAsyncData for SSR
await useAsyncData(
  () => `blog-article-${slug.value}`,
  () => slug.value ? fetchArticle(slug.value) : Promise.resolve(null),
  {
    server: true,
    immediate: true,
    watch: [slug],
  }
)

const article = computed(() => currentArticle.value)
</script>

<template>
  <v-container class="py-10">
    <v-row justify="center">
      <v-col cols="12" md="10" lg="8">
        <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="router.back()">
          Back
        </v-btn>

        <v-skeleton-loader
          v-if="loading"
          type="heading, image, paragraph, paragraph"
          class="mt-4"
        />

        <v-alert v-else-if="error" type="error" variant="tonal" class="mt-4">
          {{ error }}
        </v-alert>

        <the-article v-else-if="article" :article="article" />
      </v-col>
    </v-row>
  </v-container>
</template>
```

### Content Module

#### Editable Content Component

```vue
<!-- app/components/domains/content/EditableContent.vue -->
<script setup lang="ts">
const props = defineProps<{
  blocId: string
  editable?: boolean
}>()

const { bloc, loading, error, fetchBloc } = useContentBloc()
const { hasRole } = useAuth()

const canEdit = computed(() => props.editable && hasRole('ROLE_SITEEDITOR'))

onMounted(async () => {
  await fetchBloc(props.blocId)
})

const handleEdit = () => {
  // Open editor
  console.log('Edit block', props.blocId)
}
</script>

<template>
  <div class="editable-content">
    <v-skeleton-loader v-if="loading" type="article" />
    <v-alert v-else-if="error" type="error">{{ error }}</v-alert>

    <div v-else-if="bloc" class="content-wrapper">
      <text-content :content="bloc.content" />

      <v-btn
        v-if="canEdit"
        icon="mdi-pencil"
        size="small"
        class="edit-btn"
        @click="handleEdit"
      />
    </div>
  </div>
</template>

<style scoped>
.content-wrapper {
  position: relative;
}

.edit-btn {
  position: absolute;
  top: 0;
  right: 0;
}
</style>
```

### Authentication Module

#### Login Component

```vue
<!-- app/components/domains/auth/LoginForm.vue -->
<script setup lang="ts">
const { login } = useAuth()
const router = useRouter()

const form = ref({
  username: '',
  password: '',
})

const loading = ref(false)
const error = ref<string | null>(null)

const handleSubmit = async () => {
  loading.value = true
  error.value = null

  try {
    await login(form.value.username, form.value.password)
    router.push('/dashboard')
  } catch (err: any) {
    error.value = err.statusMessage || 'Invalid credentials'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-card max-width="400" class="mx-auto">
    <v-card-title>Login</v-card-title>

    <v-card-text>
      <v-form @submit.prevent="handleSubmit">
        <v-text-field
          v-model="form.username"
          label="Email"
          type="email"
          required
          :disabled="loading"
        />

        <v-text-field
          v-model="form.password"
          label="Password"
          type="password"
          required
          :disabled="loading"
        />

        <v-alert v-if="error" type="error" class="mt-2">
          {{ error }}
        </v-alert>

        <v-btn
          type="submit"
          color="primary"
          block
          class="mt-4"
          :loading="loading"
        >
          Login
        </v-btn>
      </v-form>
    </v-card-text>
  </v-card>
</template>
```

#### Protected Component

```vue
<!-- app/components/ProtectedComponent.vue -->
<script setup lang="ts">
const { isAuthenticated, hasRole } = useAuth()
const router = useRouter()

const requiredRole = 'ROLE_SITEEDITOR'
const canAccess = computed(() => isAuthenticated.value && hasRole(requiredRole))

onMounted(() => {
  if (!canAccess.value) {
    router.push('/login')
  }
})
</script>

<template>
  <div v-if="canAccess">
    <!-- Protected content -->
    <slot />
  </div>
  <div v-else>
    <v-alert type="warning">
      You must be logged in with the {{ requiredRole }} role to access this section.
    </v-alert>
  </div>
</template>
```

---

## Best Practices

### 1. Always Type API Responses

```typescript
// ✅ GOOD
const articles = await $fetch<BlogPostDto[]>('/api/blog/articles')

// ❌ BAD
const articles = await $fetch('/api/blog/articles')
```

### 2. Use Composables for Reusable Logic

```typescript
// ✅ GOOD - Create a composable
export const useBlog = () => {
  const articles = useState<BlogPostDto[]>('blog-articles', () => [])
  const loading = useState('blog-loading', () => false)

  const fetchArticles = async () => {
    loading.value = true
    articles.value = await $fetch('/api/blog/articles')
    loading.value = false
  }

  return { articles, loading, fetchArticles }
}

// ❌ BAD - Duplicate logic in each component
const articles = ref([])
const loading = ref(false)
const fetchArticles = async () => { /* ... */ }
```

### 3. Handle Loading States

```vue
<!-- ✅ GOOD -->
<template>
  <div>
    <v-skeleton-loader v-if="loading" />
    <v-alert v-else-if="error" type="error">{{ error }}</v-alert>
    <div v-else>{{ data }}</div>
  </div>
</template>

<!-- ❌ BAD - No loading indication -->
<template>
  <div>{{ data }}</div>
</template>
```

### 4. Use useAsyncData for SSR

```typescript
// ✅ GOOD - SSR compatible
const { data } = await useAsyncData('key', () => $fetch('/api/endpoint'))

// ❌ BAD - No SSR
onMounted(async () => {
  data.value = await $fetch('/api/endpoint')
})
```

### 5. Clean Up State on Navigation

```vue
<script setup lang="ts">
const { clearCurrentArticle } = useBlog()

// Clean up when component is unmounted
onUnmounted(() => {
  clearCurrentArticle()
})
</script>
```

### 6. Use watch for Dynamic Parameters

```vue
<script setup lang="ts">
const route = useRoute()
const { fetchArticle } = useBlog()

// Reload when slug changes
watch(() => route.params.slug, async (newSlug) => {
  if (newSlug) {
    await fetchArticle(newSlug as string)
  }
}, { immediate: true })
</script>
```

### 7. Optimize Performance with Cache

```typescript
// useAsyncData automatically caches with the provided key
const { data } = await useAsyncData(
  'unique-cache-key',
  () => $fetch('/api/endpoint'),
  {
    // Data will be reused on next visit
    getCachedData: (key) => useNuxtApp().payload.data[key]
  }
)
```

### 8. Handle Errors Globally

```typescript
// plugins/error-handler.ts
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook('vue:error', (error) => {
    console.error('Vue Error', error)
    // Send to monitoring service
  })
})
```

### 9. Available Types

All types are generated from OpenAPI and available in `shared/api-client`:

```typescript
import type {
  BlogPostDto,
  BlogTagDto,
  PageDto,
  XwikiContentBlocDto,
  // ... other types
} from '~~/shared/api-client'
```

### 10. Automatic Token Refresh

The JWT token is automatically refreshed by the `app/plugins/auth-refresh.client.ts` plugin. You don't need to do anything.

---

## Available Endpoints Summary

| Endpoint | Method | Description | Return Type |
|----------|--------|-------------|-------------|
| `/api/blog/articles` | GET | Paginated article list | `PageDto` |
| `/api/blog/articles/:slug` | GET | Article detail | `BlogPostDto` |
| `/api/blog/tags` | GET | Tag list | `BlogTagDto[]` |
| `/api/blocs/:blocId` | GET | Block content | `XwikiContentBlocDto` |
| `/auth/login` | POST | Login | `{ accessToken, refreshToken }` |
| `/auth/logout` | POST | Logout | `{ success: true }` |
| `/auth/refresh` | POST | Refresh token | `{ accessToken, refreshToken }` |

---

## Support and Questions

For any questions about API integration:

1. Check the server documentation: `server/DOC.md`
2. Check examples in `app/pages/blog/`
3. Check existing composables in `app/composables/`

**Golden Rule**: Always prefer using composables over direct `$fetch` calls for better reusability and code maintainability.