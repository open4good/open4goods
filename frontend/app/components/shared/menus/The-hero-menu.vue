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
      <v-btn
        v-if="isLoggedIn"
        color="secondary"
        rounded="pill"
        class="font-weight-bold"
        data-testid="hero-logout"
        @click="handleLogout"
      >
        <v-icon start icon="mdi-logout" />
        Logout
      </v-btn>
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
import { useI18n } from 'vue-i18n'
import { normalizeLocale, resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const route = useRoute()
const router = useRouter()
const { isLoggedIn, logout } = useAuth()
const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))

const handleLogout = async () => {
  if (!isLoggedIn.value) {
    return
  }

  try {
    await logout()
    await router.replace(route.fullPath || '/')
  } catch (error) {
    console.error('Logout failed', error)
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
  { labelKey: 'siteIdentity.menu.items.products', routeName: 'produits' },
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
  color: black
  font-size: 1rem
  cursor: pointer
  font-weight: bolder
  transition: color 0.3s ease
  &:hover
    color: green
  &.active
    color: green
    font-weight: 900
</style>
