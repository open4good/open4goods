# Frontend API Integration Guide

Guide complet pour les développeurs frontend sur l'intégration des appels API dans les composants Vue/Nuxt 3.

## Table des matières

1. [Architecture générale](#architecture-générale)
2. [Utilisation des composables](#utilisation-des-composables)
3. [Appels API directs avec $fetch](#appels-api-directs-avec-fetch)
4. [Gestion SSR avec useAsyncData](#gestion-ssr-avec-useasyncdata)
5. [Gestion des erreurs](#gestion-des-erreurs)
6. [Exemples pratiques par module](#exemples-pratiques-par-module)
7. [Bonnes pratiques](#bonnes-pratiques)

---

## Architecture générale

### Flux de données

```
Composant Vue
    ↓
Composable (ex: useBlog)
    ↓
API Endpoint (/api/blog/articles)
    ↓
Backend Server (Clean Architecture)
    ↓
Backend Java API
```

### Avantages de cette architecture

- **SSR-friendly** : Les données sont chargées côté serveur lors du premier rendu
- **Type-safe** : TypeScript assure la sécurité des types
- **Cache automatique** : Headers de cache définis côté serveur
- **Gestion d'erreurs centralisée** : Les erreurs sont normalisées
- **État partagé** : Les composables utilisent `useState` pour partager l'état

---

## Utilisation des composables

Les composables sont la méthode **recommandée** pour interagir avec les API. Ils encapsulent la logique d'état et les appels API.

### Composables disponibles

#### 1. useBlog() - Module Blog

**Localisation** : `app/composables/blog/useBlog.ts`

**État exposé** :
```typescript
const {
  // État
  articles,          // BlogPostDto[] - Liste des articles
  currentArticle,    // BlogPostDto | null - Article courant
  tags,              // BlogTagDto[] - Liste des tags
  selectedTag,       // string | null - Tag sélectionné
  loading,           // boolean - Chargement en cours
  error,             // string | null - Message d'erreur
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

**Exemple d'utilisation dans une page** :

```vue
<script setup lang="ts">
const { articles, loading, error, pagination, fetchArticles, changePage } = useBlog()

// Charger les articles au montage du composant
onMounted(async () => {
  await fetchArticles(1, 12) // page 1, 12 articles par page
})

// Changer de page
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

**Filtrage par tag** :

```vue
<script setup lang="ts">
const { articles, tags, selectedTag, selectTag, fetchTags } = useBlog()

onMounted(async () => {
  await fetchTags()
})

const handleTagClick = async (tag: string) => {
  await selectTag(tag) // Recharge automatiquement les articles
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

    <!-- Liste des articles filtrés -->
  </div>
</template>
```

#### 2. useContentBloc() - Module Content

**Localisation** : `app/composables/content/useContentBloc.ts`

**État exposé** :
```typescript
const {
  // État
  bloc,              // XwikiContentBlocDto | null - Bloc de contenu
  loading,           // boolean - Chargement en cours
  error,             // string | null - Message d'erreur

  // Actions
  fetchBloc,         // (blocId: string) => Promise<void>
  clearBloc,         // () => void
} = useContentBloc()
```

**Exemple d'utilisation** :

```vue
<script setup lang="ts">
const props = defineProps<{
  blocId: string
}>()

const { bloc, loading, error, fetchBloc } = useContentBloc()

// Charger le bloc au montage
onMounted(async () => {
  await fetchBloc(props.blocId)
})

// Recharger si le blocId change
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

#### 3. useAuth() - Module Authentification

**Localisation** : `app/composables/useAuth.ts`

**État exposé** :
```typescript
const {
  // État (depuis useAuthStore)
  isAuthenticated,   // boolean - Utilisateur connecté
  user,              // User | null - Infos utilisateur
  roles,             // string[] - Rôles de l'utilisateur

  // Actions
  login,             // (username, password) => Promise<void>
  logout,            // () => Promise<void>
  refreshToken,      // () => Promise<void>
  hasRole,           // (role: string) => boolean
  hasAnyRole,        // (roles: string[]) => boolean
} = useAuth()
```

**Exemple d'utilisation** :

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
    error.value = 'Identifiants invalides'
  }
}
</script>

<template>
  <div v-if="!isAuthenticated">
    <v-text-field v-model="username" label="Email" />
    <v-text-field v-model="password" type="password" label="Mot de passe" />
    <v-btn @click="handleLogin">Se connecter</v-btn>
    <v-alert v-if="error" type="error">{{ error }}</v-alert>
  </div>

  <div v-else>
    <p>Bienvenue {{ user?.name }}</p>
    <v-btn @click="logout">Se déconnecter</v-btn>
  </div>
</template>
```

**Contrôle d'accès basé sur les rôles** :

```vue
<script setup lang="ts">
const { hasRole, hasAnyRole } = useAuth()

const canEdit = computed(() => hasRole('ROLE_SITEEDITOR'))
const isAdmin = computed(() => hasAnyRole(['XWIKIADMINGROUP', 'ROLE_ADMIN']))
</script>

<template>
  <div>
    <v-btn v-if="canEdit" @click="editContent">Éditer</v-btn>
    <v-btn v-if="isAdmin" @click="openAdminPanel">Administration</v-btn>
  </div>
</template>
```

---

## Appels API directs avec $fetch

Pour des cas simples où un composable n'est pas nécessaire, utilisez `$fetch` directement.

### Syntaxe de base

```typescript
const data = await $fetch<TypeDeRetour>('/api/endpoint', {
  method: 'GET', // GET, POST, PUT, DELETE, etc.
  params: { key: 'value' }, // Query parameters
  body: { data: 'value' },  // Request body (POST/PUT)
  headers: { 'Custom-Header': 'value' },
})
```

### Exemples

#### GET simple

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
    console.error('Erreur lors du chargement des tags', err)
  } finally {
    loading.value = false
  }
})
</script>
```

#### GET avec paramètres

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

#### POST avec body

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

  console.log('Formulaire envoyé', response)
}
</script>
```

---

## Gestion SSR avec useAsyncData

Pour une **compatibilité SSR optimale**, utilisez `useAsyncData` ou `useFetch`. Ces composables gèrent automatiquement :
- Le chargement côté serveur (SSR)
- Le cache
- L'état de chargement
- La réactivité

### useAsyncData

**Idéal pour** : Appels API qui nécessitent une transformation ou une logique métier.

```vue
<script setup lang="ts">
import type { BlogPostDto } from '~~/shared/api-client'

const route = useRoute()
const slug = computed(() => route.params.slug as string)

// useAsyncData gère automatiquement SSR, cache, et réactivité
const { data: article, pending, error, refresh } = await useAsyncData(
  `blog-article-${slug.value}`, // Clé unique pour le cache
  () => $fetch<BlogPostDto>(`/api/blog/articles/${slug.value}`),
  {
    watch: [slug], // Recharger si slug change
    server: true,  // Exécuter côté serveur (SSR)
    lazy: false,   // Attendre le chargement avant de rendre
  }
)
</script>

<template>
  <div>
    <v-skeleton-loader v-if="pending" type="article" />
    <v-alert v-else-if="error" type="error">{{ error.message }}</v-alert>
    <article-detail v-else-if="article" :article="article" />

    <v-btn @click="refresh">Rafraîchir</v-btn>
  </div>
</template>
```

### useFetch

**Idéal pour** : Appels API simples sans transformation.

```vue
<script setup lang="ts">
import type { BlogTagDto } from '~~/shared/api-client'

// useFetch est un raccourci pour useAsyncData + $fetch
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

### Options importantes

| Option | Type | Description |
|--------|------|-------------|
| `key` | `string` | Clé unique pour le cache (obligatoire) |
| `server` | `boolean` | Exécuter côté serveur (SSR) - `true` par défaut |
| `lazy` | `boolean` | Ne pas bloquer la navigation - `false` par défaut |
| `immediate` | `boolean` | Exécuter immédiatement - `true` par défaut |
| `watch` | `Array` | Refs à surveiller pour recharger |
| `transform` | `Function` | Transformer les données avant de les retourner |

---

## Gestion des erreurs

### Erreurs dans les composables

Les composables gèrent automatiquement les erreurs et les stockent dans `error.value`.

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

### Erreurs avec $fetch

Utilisez `try/catch` pour gérer les erreurs manuellement.

```vue
<script setup lang="ts">
const loadData = async () => {
  try {
    const data = await $fetch('/api/endpoint')
    // Traiter les données
  } catch (err: any) {
    console.error('Erreur API', err)

    // Accéder aux détails de l'erreur
    if (err.statusCode === 404) {
      console.error('Ressource non trouvée')
    } else if (err.statusCode === 401) {
      console.error('Non authentifié')
      navigateTo('/login')
    } else {
      console.error('Erreur serveur', err.statusMessage)
    }
  }
}
</script>
```

### Erreurs avec useAsyncData

Les erreurs sont disponibles dans `error.value`.

```vue
<script setup lang="ts">
const { data, error } = await useAsyncData('key', () => $fetch('/api/endpoint'))

// Afficher un message personnalisé selon le code d'erreur
const errorMessage = computed(() => {
  if (!error.value) return null

  switch (error.value.statusCode) {
    case 404: return 'Ressource non trouvée'
    case 401: return 'Vous devez vous connecter'
    case 403: return 'Accès refusé'
    default: return 'Une erreur est survenue'
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

## Exemples pratiques par module

### Module Blog

#### Page de liste d'articles

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

// Charger les données initiales
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

    <!-- Filtres par tag -->
    <v-chip-group class="mb-4">
      <v-chip
        :color="!selectedTag ? 'primary' : 'default'"
        @click="handleTagFilter(null)"
      >
        Tous
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

    <!-- Liste des articles -->
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

#### Page de détail d'un article

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

// Utiliser useAsyncData pour SSR
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
          Retour
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

### Module Content

#### Composant de contenu éditable

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
  // Ouvrir l'éditeur
  console.log('Éditer le bloc', props.blocId)
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

### Module Authentification

#### Composant de connexion

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
    error.value = err.statusMessage || 'Identifiants invalides'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-card max-width="400" class="mx-auto">
    <v-card-title>Connexion</v-card-title>

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
          label="Mot de passe"
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
          Se connecter
        </v-btn>
      </v-form>
    </v-card-text>
  </v-card>
</template>
```

#### Composant protégé par authentification

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
    <!-- Contenu protégé -->
    <slot />
  </div>
  <div v-else>
    <v-alert type="warning">
      Vous devez être connecté avec le rôle {{ requiredRole }} pour accéder à cette section.
    </v-alert>
  </div>
</template>
```

---

## Bonnes pratiques

### 1. Toujours typer les réponses API

```typescript
// ✅ BON
const articles = await $fetch<BlogPostDto[]>('/api/blog/articles')

// ❌ MAUVAIS
const articles = await $fetch('/api/blog/articles')
```

### 2. Utiliser des composables pour la logique réutilisable

```typescript
// ✅ BON - Créer un composable
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

// ❌ MAUVAIS - Dupliquer la logique dans chaque composant
const articles = ref([])
const loading = ref(false)
const fetchArticles = async () => { /* ... */ }
```

### 3. Gérer les états de chargement

```vue
<!-- ✅ BON -->
<template>
  <div>
    <v-skeleton-loader v-if="loading" />
    <v-alert v-else-if="error" type="error">{{ error }}</v-alert>
    <div v-else>{{ data }}</div>
  </div>
</template>

<!-- ❌ MAUVAIS - Pas d'indication de chargement -->
<template>
  <div>{{ data }}</div>
</template>
```

### 4. Utiliser useAsyncData pour SSR

```typescript
// ✅ BON - Compatible SSR
const { data } = await useAsyncData('key', () => $fetch('/api/endpoint'))

// ❌ MAUVAIS - Pas de SSR
onMounted(async () => {
  data.value = await $fetch('/api/endpoint')
})
```

### 5. Nettoyer les états lors de la navigation

```vue
<script setup lang="ts">
const { clearCurrentArticle } = useBlog()

// Nettoyer quand le composant est démonté
onUnmounted(() => {
  clearCurrentArticle()
})
</script>
```

### 6. Utiliser watch pour les paramètres dynamiques

```vue
<script setup lang="ts">
const route = useRoute()
const { fetchArticle } = useBlog()

// Recharger quand le slug change
watch(() => route.params.slug, async (newSlug) => {
  if (newSlug) {
    await fetchArticle(newSlug as string)
  }
}, { immediate: true })
</script>
```

### 7. Optimiser les performances avec le cache

```typescript
// useAsyncData cache automatiquement avec la clé fournie
const { data } = await useAsyncData(
  'unique-cache-key',
  () => $fetch('/api/endpoint'),
  {
    // Les données seront réutilisées lors de la prochaine visite
    getCachedData: (key) => useNuxtApp().payload.data[key]
  }
)
```

### 8. Gérer les erreurs globalement

```typescript
// plugins/error-handler.ts
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook('vue:error', (error) => {
    console.error('Erreur Vue', error)
    // Envoyer à un service de monitoring
  })
})
```

### 9. Types disponibles

Tous les types sont générés depuis l'OpenAPI et disponibles dans `shared/api-client` :

```typescript
import type {
  BlogPostDto,
  BlogTagDto,
  PageDto,
  XwikiContentBlocDto,
  // ... autres types
} from '~~/shared/api-client'
```

### 10. Refresh du token automatique

Le token JWT est automatiquement rafraîchi par le plugin `app/plugins/auth-refresh.client.ts`. Vous n'avez rien à faire.

---

## Résumé des endpoints disponibles

| Endpoint | Méthode | Description | Type de retour |
|----------|---------|-------------|----------------|
| `/api/blog/articles` | GET | Liste paginée d'articles | `PageDto` |
| `/api/blog/articles/:slug` | GET | Détail d'un article | `BlogPostDto` |
| `/api/blog/tags` | GET | Liste des tags | `BlogTagDto[]` |
| `/api/blocs/:blocId` | GET | Contenu d'un bloc | `XwikiContentBlocDto` |
| `/auth/login` | POST | Connexion | `{ accessToken, refreshToken }` |
| `/auth/logout` | POST | Déconnexion | `{ success: true }` |
| `/auth/refresh` | POST | Rafraîchir le token | `{ accessToken, refreshToken }` |

---

## Support et questions

Pour toute question sur l'intégration des API :

1. Consulter la documentation du serveur : `server/DOC.md`
2. Consulter les exemples dans `app/pages/blog/`
3. Consulter les composables existants dans `app/composables/`

**Règle d'or** : Privilégiez toujours l'utilisation des composables plutôt que les appels `$fetch` directs pour une meilleure réutilisabilité et maintenabilité du code.