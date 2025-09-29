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

const { isLoggedIn, logout } = useAuth()
const router = useRouter()
const route = useRoute()

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

const baseMenuItems: MenuItemDefinition[] = [
  {
    titleKey: 'siteIdentity.menu.items.impactScore',
    routeName: 'impact-score',
    icon: 'mdi-chart-line',
  },
  {
    titleKey: 'siteIdentity.menu.items.products',
    routeName: 'produits',
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
</style>