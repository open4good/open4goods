<template>
  <menu id="container-main-menu" class="d-none d-md-block">
  <!-- Desktop menu -->
    <div class="d-flex justify-end align-center ga-4">
      <v-list class="d-flex justify-end font-weight-bold">
        <v-list-item
          v-for="item in menuItems"
          :key="item.path"
          class="main-menu-items"
          :class="{ 'active': isActiveRoute(item.path) }"
          @click="navigateToPage(item.path)"
        >
          <v-list-item-title>{{ item.label }}</v-list-item-title>
        </v-list-item>
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
import ThemeToggle from './ThemeToggle.vue'
import { useI18n } from 'vue-i18n'
import { normalizeLocale, resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const route = useRoute()
const router = useRouter()
const nuxtApp = useNuxtApp()
const { isLoggedIn, logout, username, roles } = useAuth()
const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))

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

interface MenuItemDefinition {
  labelKey: string
  routeName: string
}

interface MenuItem extends MenuItemDefinition {
  label: string
  path: string
}

const baseMenuItems: MenuItemDefinition[] = [
  { labelKey: 'siteIdentity.menu.items.impactScore', routeName: 'impact-score' },
  { labelKey: 'siteIdentity.menu.items.products', routeName: 'categories' },
  { labelKey: 'siteIdentity.menu.items.blog', routeName: 'blog' },
  { labelKey: 'siteIdentity.menu.items.contact', routeName: 'contact' },
]

const menuItems = computed<MenuItem[]>(() =>
  baseMenuItems.map((item) => ({
    ...item,
    label: t(item.labelKey),
    path: resolveLocalizedRoutePath(item.routeName, currentLocale.value),
  })),
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
</script>

<style scoped lang="sass">
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
  .account-menu-activator
    .account-username
      max-width: 96px
</style>
