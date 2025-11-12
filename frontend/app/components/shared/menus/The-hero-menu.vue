<template>
  <menu id="container-main-menu" class="d-none d-md-block">
    <!-- Desktop menu -->
    <div class="d-flex justify-end align-center ga-4">
      <div
        v-if="showMenuSearch"
        ref="menuSearchRef"
        class="main-menu-search"
        :class="{ 'main-menu-search--open': isSearchOpen }"
      >
        <v-btn
          v-if="!isSearchOpen"
          class="main-menu-search__activator"
          icon="mdi-magnify"
          variant="text"
          :aria-label="t('siteIdentity.menu.search.openLabel')"
          @click="openSearch"
        />

        <v-expand-x-transition>
          <div v-if="isSearchOpen" class="main-menu-search__field-wrapper">
            <SearchSuggestField
              v-model="searchQuery"
              class="main-menu-search__field"
              :label="t('siteIdentity.menu.search.label')"
              :placeholder="t('siteIdentity.menu.search.placeholder')"
              :aria-label="t('siteIdentity.menu.search.ariaLabel')"
              :min-chars="MIN_SEARCH_QUERY_LENGTH"
              @submit="handleSearchSubmit"
              @select-category="handleCategorySuggestion"
              @select-product="handleProductSuggestion"
              @update:model-value="updateSearchQuery"
              @clear="handleSearchClear"
            >
              <template #append-inner>
                <div class="main-menu-search__actions">
                  <v-btn
                    icon="mdi-arrow-right"
                    variant="flat"
                    color="primary"
                    size="small"
                    :aria-label="t('siteIdentity.menu.search.submitLabel')"
                    @click.prevent="handleSearchSubmit"
                  />
                  <v-btn
                    icon="mdi-close"
                    variant="text"
                    color="text-neutral-secondary"
                    size="small"
                    :aria-label="t('siteIdentity.menu.search.closeLabel')"
                    @click.prevent="closeSearch"
                  />
                </div>
              </template>
            </SearchSuggestField>
          </div>
        </v-expand-x-transition>
      </div>

      <v-list class="d-flex justify-end font-weight-bold" role="menubar">
        <template v-for="item in menuItems" :key="item.id">
          <v-list-item
            v-if="item.type === 'link'"
            class="main-menu-items"
            :class="{ active: isMenuItemActive(item) }"
            role="menuitem"
            @click="navigateToPage(item.path)"
          >
            <v-list-item-title>{{ item.label }}</v-list-item-title>
          </v-list-item>

          <div v-else class="main-menu-item main-menu-item--community">
            <v-menu
              open-on-hover
              location="bottom"
              transition="fade-transition"
              :offset="[0, 12]"
            >
              <template #activator="{ props, isActive }">
                <v-list-item
                  v-bind="props"
                  class="main-menu-items main-menu-items--community"
                  :class="{ active: isMenuItemActive(item) || isActive }"
                  role="menuitem"
                  :append-icon="isActive ? 'mdi-menu-up' : 'mdi-menu-down'"
                >

                  <v-list-item-title>{{ item.label }}</v-list-item-title>
                </v-list-item>
              </template>

              <v-card class="community-menu" color="surface-default" elevation="8">
                <div class="community-menu__header">
                  <v-avatar class="community-menu__avatar" size="44" color="surface-primary-080">
                    <v-icon icon="mdi-account-group" color="primary" />
                  </v-avatar>
                  <div>
                    <p class="community-menu__title">{{ item.label }}</p>
                    <p class="community-menu__subtitle">
                      {{ t('siteIdentity.menu.community.tagline') }}
                    </p>
                  </div>
                </div>

                <v-divider class="community-menu__divider" />

                <v-row class="community-menu__sections" justify="space-between" align="stretch">
                  <v-col
                    v-for="section in item.sections"
                    :key="section.id"
                    cols="12"
                    sm="4"
                    class="community-menu__section"
                  >
                    <p class="community-menu__section-title">{{ section.title }}</p>
                    <p class="community-menu__section-description">
                      {{ section.description }}
                    </p>

                    <v-list
                      class="community-menu__section-list"
                      bg-color="transparent"
                      density="comfortable"
                      nav
                      lines="one"
                    >
                      <v-list-item
                        v-for="link in section.links"
                        :key="link.id"
                        class="community-menu__link"
                        :class="{ 'community-menu__link--external': link.external }"
                        :to="link.to"
                        :href="link.href"
                        :target="link.external ? '_blank' : undefined"
                        :rel="link.external ? 'noopener noreferrer' : undefined"
                        role="menuitem"
                      >
                        <template #prepend>
                          <v-avatar size="34" class="community-menu__link-icon">
                            <v-icon :icon="link.icon" size="20" color="accent-primary-highlight" />
                          </v-avatar>
                        </template>

                        <v-list-item-title class="community-menu__link-label">
                          {{ link.label }}
                        </v-list-item-title>
                        <v-list-item-subtitle
                          v-if="link.description"
                          class="community-menu__link-description"
                        >
                          {{ link.description }}
                        </v-list-item-subtitle>

                        <template v-if="link.external" #append>
                          <v-icon icon="mdi-open-in-new" size="18" class="community-menu__external-icon" />
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-col>
                </v-row>
              </v-card>
            </v-menu>
          </div>
        </template>
      </v-list>

      <ThemeToggle test-id="hero-theme-toggle" />
      <v-menu
        v-if="isLoggedIn"
        v-model="isAccountMenuOpen"
        location="bottom"
        transition="fade-transition"
        min-width="260"
        offset="8"
      >
        <template #activator="{ props }">
          <v-btn
            v-bind="props"
            color="surface-primary-080"
            class="font-weight-bold account-menu-activator"
            rounded="pill"
            variant="flat"
            data-testid="hero-account-menu-activator"
          >
            <v-icon icon="mdi-account-circle" start />
            <span class="account-username text-truncate">{{ displayName }}</span>
            <v-icon icon="mdi-menu-down" end />
          </v-btn>
        </template>

        <v-card class="account-menu" color="surface-default" elevation="4">
          <v-list density="comfortable">
            <v-list-item>
              <v-list-item-title class="font-weight-medium text-truncate">
                {{ displayName }}
              </v-list-item-title>
              <v-list-item-subtitle v-if="hasRoles" class="mt-2">
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="role in accountRoles"
                    :key="role"
                    color="surface-primary-100"
                    size="small"
                    variant="flat"
                    class="role-chip"
                  >
                    {{ role }}
                  </v-chip>
                </div>
              </v-list-item-subtitle>
              <v-list-item-subtitle v-else class="text-neutral-soft">
                No assigned roles
              </v-list-item-subtitle>
            </v-list-item>

            <v-divider class="my-2" />

            <v-list-item
              density="comfortable"
              data-testid="hero-clear-cache"
              :disabled="isClearingCache"
              @click="handleClearCache"
            >
              <template #prepend>
                <v-icon icon="mdi-refresh" />
              </template>
              <v-list-item-title>
                {{ isClearingCache ? 'Clearing cache…' : 'Clear cache' }}
              </v-list-item-title>
            </v-list-item>

            <v-list-item
              density="comfortable"
              data-testid="hero-account-logout"
              @click="handleLogout"
            >
              <template #prepend>
                <v-icon icon="mdi-logout" />
              </template>
              <v-list-item-title>Logout</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card>
      </v-menu>
    </div>
  </menu>

  <!-- Mobile menu command -->
  <div class="d-flex justify-end d-md-none">
    <v-btn icon aria-label="Ouvrir le menu" @click="$emit('toggle-drawer')">
      <v-icon>mdi-menu</v-icon>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import type {
  CategorySuggestionItem,
  ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import ThemeToggle from './ThemeToggle.vue'
import { useI18n } from 'vue-i18n'
import { useCommunityMenu } from './useCommunityMenu'
import type { CommunitySection } from './useCommunityMenu'
import { normalizeLocale, resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const SearchSuggestField = defineAsyncComponent({
  loader: () => import('~/components/search/SearchSuggestField.vue'),
  suspensible: false,
})

const route = useRoute()
const router = useRouter()
const nuxtApp = useNuxtApp()
const { isLoggedIn, logout, username, roles } = useAuth()
const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))

const MIN_SEARCH_QUERY_LENGTH = 2

const menuSearchRef = ref<HTMLElement | null>(null)
const isSearchOpen = ref(false)
const searchQuery = ref('')

const homeRoutePath = computed(() => resolveLocalizedRoutePath('index', currentLocale.value))
const searchRoutePath = computed(() => resolveLocalizedRoutePath('search', currentLocale.value))
const showMenuSearch = computed(() => route.path !== homeRoutePath.value)

const focusSearchInput = () => {
  const root = menuSearchRef.value

  if (!root) {
    return
  }

  const input = root.querySelector('input')

  if (input instanceof HTMLInputElement) {
    input.focus()
    input.select()
  }
}

const openSearch = async () => {
  if (isSearchOpen.value) {
    focusSearchInput()
    return
  }

  isSearchOpen.value = true
  await nextTick()
  focusSearchInput()
}

const closeSearch = () => {
  isSearchOpen.value = false
}

const updateSearchQuery = (value: string) => {
  searchQuery.value = value
}

const handleSearchClear = () => {
  searchQuery.value = ''
}

watch(
  () => route.path,
  (path) => {
    if (path === homeRoutePath.value) {
      closeSearch()
      handleSearchClear()
    }
  },
)

watch(homeRoutePath, (path) => {
  if (route.path === path) {
    closeSearch()
    handleSearchClear()
  }
})

watch(showMenuSearch, (visible) => {
  if (!visible) {
    closeSearch()
  }
})

const navigateToSearch = (query?: string) => {
  const normalizedQuery = query?.trim() ?? ''

  router.push({
    path: searchRoutePath.value,
    query: normalizedQuery ? { q: normalizedQuery } : undefined,
  })

  closeSearch()
}

const normalizeVerticalHomeUrl = (raw: string | null | undefined): string | null => {
  if (!raw) {
    return null
  }

  const trimmed = raw.trim()

  if (!trimmed) {
    return null
  }

  if (/^https?:\/\//iu.test(trimmed)) {
    return trimmed
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

const handleSearchSubmit = () => {
  const trimmedQuery = searchQuery.value.trim()

  if (trimmedQuery.length > 0 && trimmedQuery.length < MIN_SEARCH_QUERY_LENGTH) {
    return
  }

  navigateToSearch(trimmedQuery)
}

const handleCategorySuggestion = (suggestion: CategorySuggestionItem) => {
  searchQuery.value = suggestion.title

  const normalizedUrl = normalizeVerticalHomeUrl(suggestion.url)

  if (normalizedUrl) {
    closeSearch()
    router.push(normalizedUrl)
    return
  }

  navigateToSearch(suggestion.title)
}

const handleProductSuggestion = (suggestion: ProductSuggestionItem) => {
  searchQuery.value = suggestion.title

  const gtin = suggestion.gtin?.trim()

  if (gtin) {
    closeSearch()
    router.push({
      name: 'gtin',
      params: { gtin },
    })
    return
  }

  navigateToSearch(suggestion.title)
}

type FetchLike = (input: string, init?: Record<string, unknown>) => Promise<unknown>

const resolveFetch = (): FetchLike | undefined => {
  if (typeof nuxtApp.$fetch === 'function') {
    return nuxtApp.$fetch as FetchLike
  }

  const globalFetch = (globalThis as { $fetch?: unknown }).$fetch

  if (typeof globalFetch === 'function') {
    return globalFetch as FetchLike
  }

  return undefined
}

const isAccountMenuOpen = ref(false)
const isClearingCache = ref(false)

const displayName = computed(() => {
  const label = username.value?.trim()
  return label && label.length > 0 ? label : 'Account'
})

const accountRoles = computed(() => roles.value.map((role) => role.trim()).filter((role) => role.length > 0))
const hasRoles = computed(() => accountRoles.value.length > 0)

const handleLogout = async () => {
  if (!isLoggedIn.value) {
    return
  }

  try {
    await logout()
    await router.replace(route.fullPath || '/')
    isAccountMenuOpen.value = false
  } catch (error) {
    console.error('Logout failed', error)
  }
}

const isSuccessfulCacheResetResponse = (payload: unknown): payload is { success: true } =>
  Boolean(
    payload &&
      typeof payload === 'object' &&
      'success' in payload &&
      (payload as { success?: boolean }).success === true,
  )

const handleClearCache = async () => {
  if (isClearingCache.value) {
    return
  }

  isClearingCache.value = true

  try {
    const fetcher = resolveFetch()

    if (!fetcher) {
      console.error('Failed to clear caches', new Error('No fetch helper available'))
      return
    }

    const response = await fetcher('/api/admin/cache/reset', { method: 'POST' })

    if (!isSuccessfulCacheResetResponse(response)) {
      console.error('Failed to clear caches', new Error('Unexpected response payload'))
      return
    }

    isAccountMenuOpen.value = false

    if (typeof window !== 'undefined') {
      window.location.reload()
    }
  } catch (error) {
    console.error('Failed to clear caches', error)
  } finally {
    isClearingCache.value = false
  }
}

defineEmits<{
  'toggle-drawer': []
}>()

interface LinkMenuItemDefinition {
  id: string
  labelKey: string
  routeName: string
  type: 'link'
}

interface CommunityMenuItemDefinition {
  id: string
  labelKey: string
  type: 'community'
}

type MenuItemDefinition = LinkMenuItemDefinition | CommunityMenuItemDefinition

interface LinkMenuItem {
  id: string
  type: 'link'
  label: string
  path: string
}

interface CommunityMenuItem {
  id: string
  type: 'community'
  label: string
  sections: CommunitySection[]
  activePaths: string[]
}

type MenuItem = LinkMenuItem | CommunityMenuItem
const { sections: communitySections, activePaths: communityActivePaths } = useCommunityMenu(t, currentLocale)

const baseMenuItems: MenuItemDefinition[] = [
  {
    id: 'impact-score',
    type: 'link',
    labelKey: 'siteIdentity.menu.items.impactScore',
    routeName: 'impact-score',
  },
  { id: 'products', type: 'link', labelKey: 'siteIdentity.menu.items.products', routeName: 'categories' },
  { id: 'blog', type: 'link', labelKey: 'siteIdentity.menu.items.blog', routeName: 'blog' },
  { id: 'community', type: 'community', labelKey: 'siteIdentity.menu.items.contact' },
]

const menuItems = computed<MenuItem[]>(() =>
  baseMenuItems.map((item) => {
    const label = String(t(item.labelKey))

    if (item.type === 'link') {
      return {
        id: item.id,
        type: 'link' as const,
        label,
        path: resolveLocalizedRoutePath(item.routeName, currentLocale.value),
      }
    }

    return {
      id: item.id,
      type: 'community' as const,
      label,
      sections: communitySections.value,
      activePaths: communityActivePaths.value,
    }
  }),
)

const isActiveRoute = (path: string): boolean => {
  if (!path) {
    return false
  }

  if (path === '/') {
    return route.path === path
  }

  return route.path.startsWith(path)
}

const navigateToPage = (path: string): void => {
  router.push(path)
}

const isMenuItemActive = (item: MenuItem): boolean => {
  if (item.type === 'link') {
    return isActiveRoute(item.path)
  }

  return item.activePaths.some((path) => isActiveRoute(path))
}
</script>

<style scoped lang="sass">
.main-menu-search
  display: flex
  align-items: center
  gap: 0.5rem

  &__activator
    color: rgb(var(--v-theme-text-neutral-strong))
    transition: color 0.3s ease, transform 0.3s ease
    &:hover
      color: rgb(var(--v-theme-accent-primary-highlight))
      transform: scale(1.05)

  &__field-wrapper
    width: clamp(220px, 28vw, 320px)
    max-width: 320px

  &__actions
    display: flex
    align-items: center
    gap: 0.25rem

.main-menu-search__field
  :deep(.v-field)
    border-radius: 999px
    background-color: rgba(var(--v-theme-surface-primary-080), 0.95)
    box-shadow: 0 12px 30px rgba(var(--v-theme-shadow-primary-600), 0.12)

  :deep(.v-field__outline)
    display: none

  :deep(.v-field__input)
    padding-inline-start: 1rem
    min-height: 44px

  :deep(.v-field__append-inner)
    padding-inline-end: 0.25rem

  :deep(.v-field__clearable)
    margin-inline-end: 0.25rem

.main-menu-search--open .main-menu-search__activator
  opacity: 0
  pointer-events: none

.main-menu-items
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 1rem
  cursor: pointer
  font-weight: bolder
  transition: color 0.3s ease
  &:hover
    color: rgb(var(--v-theme-accent-supporting))
  &.active
    color: rgb(var(--v-theme-accent-supporting))
    font-weight: 900

.main-menu-items--community
  min-height: 48px
  padding-inline: 0.75rem
  gap: 0.25rem

.community-menu
  width: min(720px, 80vw)
  padding: 1.5rem
  border-radius: 1.25rem
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.95), rgba(var(--v-theme-surface-glass), 0.95))
  box-shadow: 0 24px 48px rgba(var(--v-theme-shadow-primary-600), 0.16)

  &__header
    display: flex
    align-items: center
    gap: 1rem
    margin-block-end: 0.75rem

  &__avatar
    background-color: rgba(var(--v-theme-surface-primary-120), 0.85)
    box-shadow: 0 10px 30px rgba(var(--v-theme-shadow-primary-600), 0.2)

  &__title
    margin: 0
    font-size: 1.1rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    margin: 0
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__divider
    margin-block: 0.5rem 1rem
    opacity: 0.4

  &__sections
    gap: 1rem
    margin: 0

  &__section
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__section-title
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__section-description
    margin: 0
    font-size: 0.85rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    min-height: 2.5rem

  &__section-list
    padding: 0
    background: transparent

  &__link
    border-radius: 0.75rem
    transition: background-color 0.2s ease, transform 0.2s ease
    margin-block: 0.125rem

    &:hover
      background-color: rgba(var(--v-theme-surface-primary-080), 0.8)
      transform: translateY(-1px)

  &__link-icon
    background-color: rgba(var(--v-theme-surface-primary-120), 0.9)

  &__link-label
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__link-description
    color: rgb(var(--v-theme-text-neutral-secondary))
    white-space: normal

  &__external-icon
    color: rgb(var(--v-theme-accent-primary-highlight))

.account-menu-activator
  text-transform: none
  color: rgb(var(--v-theme-text-neutral-strong))
  background-color: rgb(var(--v-theme-surface-primary-080))

  &:hover
    background-color: rgb(var(--v-theme-surface-primary-100))

  .account-username
    max-width: 140px
    display: inline-block

.account-menu
  background-color: rgb(var(--v-theme-surface-default))
  color: rgb(var(--v-theme-text-neutral-strong))

.role-chip
  background-color: rgb(var(--v-theme-surface-primary-120))
  border: 1px solid rgb(var(--v-theme-border-primary-strong))
  color: rgb(var(--v-theme-text-neutral-strong))

.text-neutral-soft
  color: rgb(var(--v-theme-text-neutral-soft))

@media (max-width: 1263px)
  .community-menu
    width: min(640px, 90vw)

  .account-menu-activator
    .account-username
      max-width: 96px
</style>
