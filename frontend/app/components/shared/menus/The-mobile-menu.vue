<template>
  <div class="mobile-menu">
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

      <v-list-item
        v-for="(item, index) in menuItems"
        :key="index"
        :to="item.to"
        class="px-6 py-4"
        @click="emit('close')"
      >
        <template #prepend>
          <v-icon :icon="item.icon" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ item.title }}
        </v-list-item-title>
      </v-list-item>

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

      <v-divider v-if="isLoggedIn" class="mx-6" />

      <v-list-item
        v-if="isLoggedIn"
        class="px-6 py-4 account-summary"
      >
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
        <v-list-item-title class="text-body-1">
          Logout
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </div>
</template>

<script setup lang="ts">
import ThemeToggle from './ThemeToggle.vue'
import { useI18n } from 'vue-i18n'

import { normalizeLocale, resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))

const themeToggleLabel = computed(() => {
  const translation = t('siteIdentity.menu.themeToggle')
  return translation === 'siteIdentity.menu.themeToggle' ? 'Toggle theme' : translation
})

const emit = defineEmits<{
  close: []
}>()

const nuxtApp = useNuxtApp()
const { isLoggedIn, logout, username, roles } = useAuth()
const router = useRouter()
const route = useRoute()
const isClearingCache = ref(false)

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

const displayName = computed(() => {
  const label = username.value?.trim()
  return label && label.length > 0 ? label : 'Account'
})

const accountRoles = computed(() => roles.value.map((role) => role.trim()).filter((role) => role.length > 0))
const hasRoles = computed(() => accountRoles.value.length > 0)

interface MenuItemDefinition {
  titleKey: string
  routeName: string
  icon: string
}

interface MenuItem extends MenuItemDefinition {
  title: string
  to: string
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

const isSuccessfulCacheResetResponse = (payload: unknown): payload is { success: true } =>
  Boolean(
    payload &&
      typeof payload === 'object' &&
      'success' in payload &&
      (payload as { success?: boolean }).success === true,
  )

const handleClearCache = async () => {
  if (!isLoggedIn.value || isClearingCache.value) {
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

const baseMenuItems: MenuItemDefinition[] = [
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
  {
    titleKey: 'siteIdentity.menu.items.contact',
    routeName: 'contact',
    icon: 'mdi-email',
  },
]

const menuItems = computed<MenuItem[]>(() =>
  baseMenuItems.map((item) => ({
    ...item,
    title: t(item.titleKey),
    to: resolveLocalizedRoutePath(item.routeName, currentLocale.value),
  })),
)
</script>

<style scoped lang="sass">
.mobile-menu
  height: 100%

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
</style>
