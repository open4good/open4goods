<template>
  <v-app-bar scroll-behavior="elevate" color="surface" elevation="0" border="b">
    <v-container class="d-flex align-center ga-2" style="min-height: 64px;">
      <NuxtLink to="/" class="d-flex align-center text-decoration-none">
        <v-avatar color="primary" variant="tonal" rounded="lg" size="34" class="mr-2">
          <v-icon icon="mdi-barcode-scan" size="20" />
        </v-avatar>
        <span class="font-weight-bold text-body-1">{{ t('app.name') }}</span>
      </NuxtLink>

      <template v-if="display.mdAndUp.value">
        <v-spacer />

        <nav aria-label="Primary" class="d-flex align-center ga-1">
          <template v-for="item in desktopLinks" :key="item.key">
            <v-menu
              v-if="item.children && item.children.length > 0"
              open-on-hover
              open-on-focus
              :open-on-click="isMenuParentClickable(item)"
              close-delay="100"
              offset="8"
            >
              <template #activator="{ props }">
                <v-btn
                  v-bind="props"
                  :to="isMenuParentClickable(item) ? item.to : undefined"
                  variant="text"
                  :exact="true"
                  size="small"
                  rounded="pill"
                  append-icon="mdi-chevron-down"
                >
                  {{ t(item.key) }}
                </v-btn>
              </template>
              <v-list density="compact" nav class="mt-1 rounded-xl border elevation-lg">
                <v-list-item
                  v-for="child in item.children"
                  :key="child.key"
                  :to="child.to"
                  :title="t(child.key)"
                  rounded="lg"
                  color="primary"
                />
              </v-list>
            </v-menu>
            <v-btn
              v-else
              :to="item.to"
              variant="text"
              :exact="true"
              size="small"
              rounded="pill"
            >
              {{ t(item.key) }}
            </v-btn>
          </template>
        </nav>

        <v-spacer />

        <div class="d-flex align-center ga-2">
          <template v-for="item in desktopActions" :key="item.key">
            <v-btn
              :to="item.to"
              :variant="item.variant"
              :color="item.color"
              :prepend-icon="item.icon"
              :exact="true"
              size="small"
              rounded="pill"
            >
              {{ t(item.key) }}
            </v-btn>
          </template>

          <InfLanguageToggle />
          <InfThemeToggle />

          <v-btn
            v-if="!isAuthenticated"
            :to="{ path: '/auth/login', query: { next: route.fullPath } }"
            size="small"
            color="primary"
            variant="flat"
            rounded="pill"
          >
            {{ t('nav.sign_in') }}
          </v-btn>
          <OrgContextSwitcher v-else />
        </div>
      </template>

      <template v-else>
        <v-spacer />
        <v-app-bar-nav-icon class="ml-1" @click="drawer = !drawer" />
      </template>
    </v-container>
  </v-app-bar>

  <v-navigation-drawer
    v-if="display.smAndDown.value"
    v-model="drawer"
    location="right"
    temporary
  >
    <v-list nav>
      <v-list-subheader>{{ t('nav.navigation') }}</v-list-subheader>
      <template v-for="item in mobileItems" :key="item.key">
        <v-list-group v-if="item.children && item.children.length > 0" :value="item.key">
          <template #activator="{ props }">
            <v-list-item
              v-bind="props"
              :title="t(item.key)"
              :prepend-icon="item.icon || 'mdi-cube-outline'"
              :active="isNavActive(item.to, item.activePrefix)"
            />
          </template>
          <v-list-item
            v-for="child in item.children"
            :key="child.key"
            :to="child.to"
            :title="t(child.key)"
            rounded="lg"
            @click="drawer = false"
          />
        </v-list-group>
        <v-list-item
          v-else
          :to="item.to"
          :title="t(item.key)"
          :prepend-icon="item.icon"
          :active="isNavActive(item.to, item.activePrefix)"
          @click="drawer = false"
        />
      </template>

      <v-divider class="my-2" />
      <v-list-subheader>{{ t('theme.title') }}</v-list-subheader>
      <v-list-item :title="t('nav.language')" prepend-icon="mdi-translate" />
      <v-list-item :title="t('theme.toggle')" prepend-icon="mdi-theme-light-dark" @click="toggleThemeFromMobile" />

      <v-divider class="my-2" />
      <v-list-subheader>{{ t('nav.account') }}</v-list-subheader>
      <v-list-item
        v-if="!isAuthenticated"
        :to="{ path: '/auth/login', query: { next: route.fullPath } }"
        :title="t('nav.sign_in')"
        @click="drawer = false"
      />
      <template v-else>
        <v-list-item
          v-for="item in desktopAccountActions"
          :key="item.key"
          :to="item.to"
          :title="t(item.key)"
          :prepend-icon="item.icon"
          :active="isNavActive(item.to)"
          @click="drawer = false"
        />
        <v-list-item :to="'/dashboard/settings'" :title="t('nav.manage_profile')" prepend-icon="mdi-account-cog-outline" @click="drawer = false" />
        <v-list-item
          :title="t('nav.logout')"
          prepend-icon="mdi-logout"
          @click="onMobileLogout"
        />
      </template>
    </v-list>
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import { useDisplay, useTheme } from 'vuetify'
import InfLanguageToggle from '~/components/infra/InfLanguageToggle.vue'
import InfThemeToggle from '~/components/infra/InfThemeToggle.vue'
import OrgContextSwitcher from '~/components/org/OrgContextSwitcher.vue'

const { t } = useI18n()
const { items, primaryActions } = useLandingNav()
const display = useDisplay()
const vuetifyTheme = useTheme()
const drawer = ref(false)
const route = useRoute()
const router = useRouter()
const { session, fetchMe, logout } = useAuthSession()

const isAuthenticated = computed(() => Boolean(session.value))
const canAccessAdmin = computed(() => session.value?.user?.platformAdmin === true)

interface LandingNavItem {
  key: string
  to: string
  variant: 'text' | 'tonal' | 'flat'
  color?: string
  icon?: string
  emphasis?: 'strong' | 'light'
  activePrefix?: string
  parentClickable?: boolean
  children?: { key: string; to: string }[]
}

const desktopLinks = computed<LandingNavItem[]>(() =>
  items.map(item => ({
    key: item.key,
    to: item.to,
    activePrefix: item.activePrefix,
    parentClickable: item.parentClickable,
    variant: 'text',
    children: item.children
  }))
)

const desktopActions = computed<LandingNavItem[]>(() =>
  primaryActions.map(item => ({
    key: item.key,
    to: item.to,
    variant: item.variant,
    color: item.color,
    icon: item.icon,
    emphasis: item.emphasis as 'strong' | 'light'
  }))
)

const desktopAccountActions = computed<LandingNavItem[]>(() => {
  const accountItems: LandingNavItem[] = []

  if (isAuthenticated.value) {
    accountItems.push({
      key: 'nav.dashboard',
      to: '/dashboard',
      variant: 'text',
      icon: 'mdi-view-dashboard-outline'
    })
  }

  if (canAccessAdmin.value) {
    accountItems.push({
      key: 'nav.admin_overview',
      to: '/admin',
      variant: 'text',
      icon: 'mdi-shield-crown-outline'
    })
  }

  return accountItems
})

const mobileItems = computed(() =>
  [
    ...desktopLinks.value,
    ...desktopActions.value
  ]
)

function isNavActive(to: string, activePrefix?: string) {
  if (activePrefix) {
    return route.path.startsWith(activePrefix)
  }

  if (to === '/') {
    return route.path === '/'
  }

  return route.path === to || route.path.startsWith(`${to}/`)
}

function isMenuParentClickable(item: LandingNavItem) {
  return item.parentClickable !== false
}

onMounted(async () => {
  if (!session.value) {
    await fetchMe()
  }
})

async function onLogout() {
  await logout()
  if (route.path.startsWith('/admin')) {
    await router.push({ path: '/auth/login', query: { next: route.fullPath } })
  }
}

async function onMobileLogout() {
  drawer.value = false
  await onLogout()
}

function toggleThemeFromMobile() {
  vuetifyTheme.global.name.value = vuetifyTheme.global.current.value.dark ? 'light' : 'dark'
}
</script>
