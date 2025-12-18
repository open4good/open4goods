<template>
  <nav class="mobile-menu" :aria-label="t('siteIdentity.menu.mobileAriaLabel')">
    <v-list class="pa-0">
      <v-list-item
        class="px-6 py-4 border-bottom d-flex justify-space-between align-center"
      >
        <v-list-item-title class="text-h6 font-weight-bold">
          {{ t('siteIdentity.menu.title') }}
        </v-list-item-title>
        <template #append>
          <v-btn
            icon
            variant="text"
            size="small"
            :aria-label="t('siteIdentity.menu.closeLabel')"
            @click="$emit('close')"
          >
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </template>
      </v-list-item>

      <div v-if="showMenuSearch" class="px-6 pt-3 pb-2 mobile-menu__search">
        <SearchSuggestField
          v-model="searchQuery"
          class="mobile-menu__search-field"
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
            <div class="mobile-menu__search-actions">
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
                @click.prevent="handleSearchClear"
              />
            </div>
          </template>
        </SearchSuggestField>
      </div>

      <v-divider v-if="showMenuSearch" class="mx-6" />

      <v-list-item
        v-for="(item, index) in menuItems"
        :key="index"
        :to="item.to"
        class="px-6 py-4"
        :class="{ 'mobile-menu__item--active': isActiveRoute(item.to) }"
        @click="emit('close')"
      >
        <template #prepend>
          <v-icon :icon="item.icon" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ item.title }}
        </v-list-item-title>
      </v-list-item>

      <v-divider v-if="showInstallMenuItem" class="mx-6" />

      <v-list-item
        v-if="showInstallMenuItem"
        class="px-6 py-4 mobile-menu__install"
      >
        <template #prepend>
          <v-icon icon="mdi-cellphone-arrow-down" class="me-4" />
        </template>

        <v-list-item-title class="mobile-menu__install-title">
          {{ t('pwa.install.cta') }}
        </v-list-item-title>
        <v-list-item-subtitle class="mobile-menu__install-subtitle">
          {{ t('pwa.install.description') }}
        </v-list-item-subtitle>

        <template #append>
          <v-btn
            color="primary"
            variant="flat"
            size="small"
            :loading="installInProgress"
            data-testid="mobile-install-cta"
            @click.stop.prevent="handleInstallFromMenu"
          >
            {{ t('pwa.install.title') }}
          </v-btn>
        </template>
      </v-list-item>

      <v-list-group
        v-model="isCommunityExpanded"
        class="mobile-menu__community"
      >
        <template #activator="{ props, isOpen }">
          <v-list-item
            v-bind="props"
            class="px-6 py-4 mobile-menu__community-activator"
            :class="{
              'mobile-menu__community-activator--active': isCommunityActive,
            }"
            :append-icon="isOpen ? 'mdi-menu-up' : 'mdi-menu-down'"
          >
            <template #prepend>
              <v-icon icon="mdi-account-group" class="me-4" />
            </template>
            <v-list-item-title class="text-body-1">
              {{ communityLabel }}
            </v-list-item-title>
          </v-list-item>
        </template>

        <div class="mobile-menu__community-content px-6 pb-4">
          <div
            v-for="section in communitySections"
            :key="section.id"
            class="mobile-menu__community-section"
          >
            <p class="mobile-menu__community-section-title">
              {{ section.title }}
            </p>
            <p class="mobile-menu__community-section-description">
              {{ section.description }}
            </p>

            <v-list
              class="mobile-menu__community-links"
              bg-color="transparent"
              density="comfortable"
              nav
              lines="one"
            >
              <v-list-item
                v-for="link in section.links"
                :key="link.id"
                class="mobile-menu__community-link"
                :href="link.href"
                :target="link.external ? '_blank' : undefined"
                :rel="link.external ? 'noopener noreferrer' : undefined"
                :value="link.id"
                @click="handleCommunityNavigation(link)"
              >
                <template #prepend>
                  <v-avatar size="32" class="mobile-menu__community-link-icon">
                    <v-icon
                      :icon="link.icon"
                      size="18"
                      color="accent-primary-highlight"
                    />
                  </v-avatar>
                </template>

                <v-list-item-title class="mobile-menu__community-link-label">
                  {{ link.label }}
                </v-list-item-title>
                <v-list-item-subtitle
                  v-if="link.description"
                  class="mobile-menu__community-link-description"
                >
                  {{ link.description }}
                </v-list-item-subtitle>

                <template v-if="link.external" #append>
                  <v-icon icon="mdi-open-in-new" size="16" />
                </template>
              </v-list-item>
            </v-list>
          </div>
        </div>
      </v-list-group>

      <v-list-item class="px-6 py-4">
        <template #prepend>
          <v-icon icon="mdi-theme-light-dark" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ themeToggleLabel }}
        </v-list-item-title>
        <template #append>
          <ThemeToggle test-id="mobile-theme-toggle" density="compact" />
        </template>
      </v-list-item>

      <v-list-item class="px-6 py-4">
        <template #prepend>
          <v-icon icon="mdi-face-woman-outline" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ t('siteIdentity.menu.zoom.label') }}
        </v-list-item-title>
        <template #append>
          <ZoomToggle density="compact" />
        </template>
      </v-list-item>

      <v-divider v-if="isLoggedIn" class="mx-6" />

      <v-list-item v-if="isLoggedIn" class="px-6 py-4 account-summary">
        <template #prepend>
          <v-icon icon="mdi-account-circle" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1 font-weight-medium">
          {{ displayName }}
        </v-list-item-title>
        <v-list-item-subtitle v-if="hasRoles" class="mt-2">
          <div class="d-flex flex-wrap ga-2">
            <v-chip
              v-for="role in accountRoles"
              :key="role"
              size="small"
              variant="flat"
              color="surface-primary-100"
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

      <v-list-item
        v-if="isLoggedIn"
        class="px-6 py-4"
        data-testid="mobile-clear-cache"
        :disabled="isClearingCache"
        @click="handleClearCache"
      >
        <template #prepend>
          <v-icon icon="mdi-refresh" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ isClearingCache ? 'Clearing cache…' : 'Clear cache' }}
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        v-if="isLoggedIn"
        class="px-6 py-4"
        data-testid="mobile-logout"
        @click="handleLogout"
      >
        <template #prepend>
          <v-icon icon="mdi-logout" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1"> Logout </v-list-item-title>
      </v-list-item>
    </v-list>
  </nav>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import ThemeToggle from './ThemeToggle.vue'
import ZoomToggle from './ZoomToggle.vue'
import { useI18n } from 'vue-i18n'

import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'
import { useCommunityMenu, type CommunityMenuLink } from './useCommunityMenu'
import { usePwaInstallPromptBridge } from '~/composables/pwa/usePwaInstallPromptBridge'
import {
  MIN_SEARCH_QUERY_LENGTH,
  useMenuSearchControls,
} from '~/composables/menus/useMenuSearchControls'

const SearchSuggestField = defineAsyncComponent({
  loader: () => import('~/components/search/SearchSuggestField.vue'),
  suspensible: false,
})

const { t, locale } = useI18n()
const {
  installPromptVisible,
  isInstallSupported,
  installInProgress,
  requestInstall,
} = usePwaInstallPromptBridge()
const currentLocale = computed(() => normalizeLocale(locale.value))

const themeToggleLabel = computed(() => {
  const translation = t('siteIdentity.menu.themeToggle')
  return translation === 'siteIdentity.menu.themeToggle'
    ? 'Toggle theme'
    : translation
})

const emit = defineEmits<{
  close: []
}>()

const nuxtApp = useNuxtApp()
const { isLoggedIn, logout, username, roles } = useAuth()
const router = useRouter()
const route = useRoute()
const isClearingCache = ref(false)
const showInstallMenuItem = computed(
  () => installPromptVisible.value && isInstallSupported.value
)

const {
  searchQuery,
  showMenuSearch,
  handleSearchClear,
  handleSearchSubmit,
  handleCategorySuggestion,
  handleProductSuggestion,
} = useMenuSearchControls({
  onNavigate: () => emit('close'),
})

const { sections: communitySections, activePaths: communityActivePaths } =
  useCommunityMenu(t, currentLocale)
const communityLabel = computed(() =>
  String(t('siteIdentity.menu.items.contact'))
)
const isCommunityExpanded = ref(false)

const isActiveRoute = (path: string): boolean => {
  if (!path) {
    return false
  }

  if (path === '/') {
    return route.path === path
  }

  return route.path.startsWith(path)
}

const isCommunityActive = computed(() =>
  communityActivePaths.value.some(path => isActiveRoute(path))
)

watch(
  isCommunityActive,
  active => {
    if (active) {
      isCommunityExpanded.value = true
    }
  },
  { immediate: true }
)

type FetchLike = (
  input: string,
  init?: Record<string, unknown>
) => Promise<unknown>

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

const displayName = computed(() => {
  const label = username.value?.trim()
  return label && label.length > 0 ? label : 'Account'
})

const accountRoles = computed(() =>
  roles.value.map(role => role.trim()).filter(role => role.length > 0)
)
const hasRoles = computed(() => accountRoles.value.length > 0)

interface MenuLinkDefinition {
  titleKey: string
  routeName: string
  icon: string
}

interface MenuLink extends MenuLinkDefinition {
  title: string
  to: string
}

const updateSearchQuery = (value: string) => {
  searchQuery.value = value
}

const handleInstallFromMenu = async () => {
  if (!showInstallMenuItem.value || installInProgress.value) {
    return
  }

  await requestInstall()
  emit('close')
}

const handleCommunityNavigation = (link: CommunityMenuLink) => {
  emit('close')

  if (link.to) {
    router.push(link.to)
  }
}

const handleLogout = async () => {
  if (!isLoggedIn.value) {
    return
  }

  try {
    await logout()
    await router.replace(route.fullPath || '/')
  } catch (error) {
    console.error('Logout failed', error)
  } finally {
    emit('close')
  }
}

const isSuccessfulCacheResetResponse = (
  payload: unknown
): payload is { success: true } =>
  Boolean(
    payload &&
    typeof payload === 'object' &&
    'success' in payload &&
    (payload as { success?: boolean }).success === true
  )

const handleClearCache = async () => {
  if (!isLoggedIn.value || isClearingCache.value) {
    return
  }

  isClearingCache.value = true

  try {
    const fetcher = resolveFetch()

    if (!fetcher) {
      console.error(
        'Failed to clear caches',
        new Error('No fetch helper available')
      )
      return
    }

    const response = await fetcher('/api/admin/cache/reset', { method: 'POST' })

    if (!isSuccessfulCacheResetResponse(response)) {
      console.error(
        'Failed to clear caches',
        new Error('Unexpected response payload')
      )
      return
    }

    emit('close')

    if (typeof window !== 'undefined') {
      window.location.reload()
    }
  } catch (error) {
    console.error('Failed to clear caches', error)
  } finally {
    isClearingCache.value = false
  }
}

const baseMenuItems: MenuLinkDefinition[] = [
  {
    titleKey: 'siteIdentity.menu.items.impactScore',
    routeName: 'impact-score',
    icon: 'mdi-chart-line',
  },
  {
    titleKey: 'siteIdentity.menu.items.products',
    routeName: 'categories',
    icon: 'mdi-package-variant',
  },
  {
    titleKey: 'siteIdentity.menu.items.blog',
    routeName: 'blog',
    icon: 'mdi-post',
  },
]

const menuItems = computed<MenuLink[]>(() =>
  baseMenuItems.map(item => ({
    ...item,
    title: t(item.titleKey),
    to: resolveLocalizedRoutePath(item.routeName, currentLocale.value),
  }))
)
</script>

<style scoped lang="sass">
.mobile-menu
  height: 100%

.mobile-menu__search
  background-color: rgba(var(--v-theme-surface-primary-080), 0.35)

.mobile-menu__search-field
  :deep(.v-field)
    border-radius: 999px
    background-color: rgb(var(--v-theme-surface-default))
    box-shadow: 0 10px 24px rgba(var(--v-theme-shadow-primary-600), 0.12)

  :deep(.v-field__outline)
    display: none

  :deep(.v-field__input)
    min-height: 48px
    padding-inline-start: 1rem

  :deep(.v-field__append-inner)
    padding-inline-end: 0.25rem

.mobile-menu__search-actions
  display: flex
  align-items: center
  gap: 0.25rem

.mobile-menu__item--active
  color: rgb(var(--v-theme-accent-supporting))
  font-weight: 700

.mobile-menu__community
  --v-list-group-prepend-width: 36px

.mobile-menu__community-activator
  transition: color 0.2s ease

.mobile-menu__community-activator--active
  color: rgb(var(--v-theme-accent-supporting))
  font-weight: 700

.mobile-menu__community-content
  background-color: rgba(var(--v-theme-surface-primary-080), 0.25)
  border-block-start: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

.mobile-menu__community-section
  padding-block: 1rem 0

.mobile-menu__community-section-title
  margin: 0
  font-weight: 700
  font-size: 1rem
  color: rgb(var(--v-theme-text-neutral-strong))

.mobile-menu__community-section-description
  margin: 0.35rem 0 0.75rem
  font-size: 0.85rem
  color: rgb(var(--v-theme-text-neutral-secondary))

.mobile-menu__community-links
  :deep(.v-list-item)
    border-radius: 0.75rem
    transition: background-color 0.2s ease

  :deep(.v-list-item:hover)
    background-color: rgba(var(--v-theme-surface-primary-120), 0.35)

.mobile-menu__community-link-icon
  background-color: rgba(var(--v-theme-surface-primary-120), 0.85)

.mobile-menu__community-link-label
  font-weight: 600

.mobile-menu__community-link-description
  font-size: 0.8rem
  color: rgb(var(--v-theme-text-neutral-secondary))

.border-bottom
  border-bottom: 1px solid rgba(0, 0, 0, 0.12)

.account-summary
  background-color: rgb(var(--v-theme-surface-default))

.role-chip
  background-color: rgb(var(--v-theme-surface-primary-120))
  border: 1px solid rgb(var(--v-theme-border-primary-strong))
  color: rgb(var(--v-theme-text-neutral-strong))

.text-neutral-soft
  color: rgb(var(--v-theme-text-neutral-soft))

.mobile-menu__install
  background-color: rgba(var(--v-theme-surface-primary-080), 0.35)
  border-radius: 1rem

.mobile-menu__install-title
  font-weight: 700
  margin-bottom: 0.15rem

.mobile-menu__install-subtitle
  font-size: 0.85rem
  color: rgb(var(--v-theme-text-neutral-secondary))

.mobile-menu__install :deep(.v-btn)
  text-transform: none
  font-weight: 600
</style>
