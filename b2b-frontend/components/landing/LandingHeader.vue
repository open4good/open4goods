<template>
  <v-app-bar class="landing-header" scroll-behavior="elevate" flat>
    <v-container class="landing-header__container d-flex align-center ga-2">
      <NuxtLink to="/" class="landing-header__brand d-flex align-center">
        <v-avatar color="primary" variant="tonal" rounded="lg" size="34" class="mr-2">
          <v-icon icon="mdi-barcode-scan" size="20" />
        </v-avatar>
        <span class="font-weight-bold text-body-1">{{ t('app.name') }}</span>
      </NuxtLink>

      <template v-if="display.mdAndUp.value">
        <v-spacer />

        <nav aria-label="Primary" class="landing-header__desktop-nav">
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
                  :variant="item.variant"
                  :exact="true"
                  size="small"
                  rounded="pill"
                  class="landing-header__nav-btn landing-header__nav-btn--link"
                  :class="{ 'landing-header__nav-btn--active': isNavActive(item.to, item.activePrefix) }"
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
              :variant="item.variant"
              :exact="true"
              size="small"
              rounded="pill"
              class="landing-header__nav-btn landing-header__nav-btn--link"
              :class="{ 'landing-header__nav-btn--active': isNavActive(item.to, item.activePrefix) }"
            >
              {{ t(item.key) }}
            </v-btn>
          </template>
        </nav>

        <div class="landing-header__desktop-actions">
          <div class="landing-header__desktop-cta-group">
            <v-btn
              v-for="item in desktopActions"
              :key="item.key"
              :to="item.to"
              :variant="item.variant"
              :color="item.color"
              :prepend-icon="item.icon"
              :exact="true"
              size="small"
              rounded="pill"
              class="landing-header__nav-btn"
              :class="[
                item.emphasis === 'strong'
                  ? 'landing-header__nav-btn--download'
                  : 'landing-header__nav-btn--playground',
                { 'landing-header__nav-btn--active': isNavActive(item.to, item.activePrefix) }
              ]"
            >
              {{ t(item.key) }}
            </v-btn>
          </div>

          <div class="landing-header__desktop-group landing-header__desktop-group--preferences">
            <InfLanguageToggle />
            <InfThemeToggle />
          </div>

          <div class="landing-header__desktop-group landing-header__desktop-group--identity">
            <v-btn
              v-if="!isAuthenticated"
              class="identity-cta"
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
          :class="item.listClass"
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
          :class="item.listClass"
          :active="isNavActive(item.to)"
          :prepend-icon="item.icon"
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
  listClass?: string
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
    ...desktopLinks.value.map(item => ({
      ...item,
      icon: item.key === 'nav.models' ? 'mdi-cube-outline' : undefined,
      listClass: 'landing-header__mobile-list-item'
    })),
    ...desktopActions.value.map(item => ({
      ...item,
      listClass:
        item.emphasis === 'strong'
          ? 'landing-header__mobile-list-item landing-header__mobile-list-item--download'
          : 'landing-header__mobile-list-item landing-header__mobile-list-item--playground'
    }))
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

<style scoped lang="scss">
.landing-header {
  background:
    radial-gradient(circle at 16% -20%, color-mix(in oklab, var(--inf-token-color-accent-glow) 66%, transparent) 0%, transparent 48%),
    color-mix(in oklab, var(--inf-token-color-bg-base) 76%, transparent);
  backdrop-filter: blur(14px) saturate(130%);
  border-bottom: 1px solid color-mix(in oklab, var(--inf-token-color-line-subtle) 82%, transparent);
}

.landing-header__container {
  min-height: 64px;
  gap: 16px;
}

.landing-header__brand {
  text-decoration: none;
  flex: 0 0 auto;
  color: var(--inf-token-color-text-primary);
}

.landing-header__desktop-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--inf-token-color-line-subtle) 70%, transparent);
  background: color-mix(in oklab, var(--inf-token-color-bg-elevated) 55%, transparent);
}

.landing-header__desktop-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}

.landing-header__desktop-cta-group {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 14px;
  margin-right: 2px;
  border-right: 1px solid color-mix(in oklab, var(--inf-token-color-line-subtle) 70%, transparent);
}

.landing-header__nav-btn {
  text-transform: none;
  font-weight: 600;
  letter-spacing: 0;
  border-radius: 999px;
  transition:
    transform 0.22s ease,
    background-color 0.22s ease,
    box-shadow 0.22s ease,
    color 0.22s ease;
}

.landing-header__nav-btn--link {
  color: var(--inf-token-color-text-secondary);
}

.landing-header__nav-btn--download {
  font-weight: 700;
  box-shadow: 0 10px 28px color-mix(in oklab, var(--inf-token-color-accent-glow) 46%, transparent);
}

.landing-header__nav-btn--playground {
  min-width: 0;
}

.landing-header__nav-btn--active {
  transform: translateY(-1px);
  color: var(--inf-token-color-text-primary);
}

.landing-header__nav-btn--link.landing-header__nav-btn--active {
  background: color-mix(in oklab, var(--inf-token-color-accent-glow) 22%, transparent);
}

.landing-header__nav-btn--playground.landing-header__nav-btn--active {
  background: color-mix(in oklab, var(--inf-token-color-accent-glow) 30%, transparent);
}

.landing-header__nav-btn--download.landing-header__nav-btn--active {
  box-shadow: 0 12px 30px color-mix(in oklab, var(--inf-token-color-accent-glow) 54%, transparent);
}

.landing-header__desktop-group {
  display: flex;
  align-items: center;
}

.landing-header__desktop-group--actions {
  gap: 16px;
}

.landing-header__desktop-group--preferences {
  gap: 8px;
}

.landing-header__desktop-group--identity {
  gap: 4px;
}

.landing-header__mobile-list-item--download {
  font-weight: 700;
}

.landing-header__mobile-list-item--playground {
  font-weight: 600;
}

.landing-header__mobile-list-item--account {
  opacity: 0.92;
}

.identity-cta {
  text-transform: none;
  font-weight: 700;
  border-radius: 999px;
  box-shadow: 0 10px 28px color-mix(in oklab, var(--inf-token-color-accent-glow) 60%, transparent);
}

.identity-account {
  text-transform: none;
  font-weight: 600;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--inf-token-color-line-subtle) 70%, transparent);
  background: color-mix(in oklab, var(--inf-token-color-bg-elevated) 35%, transparent);
}
</style>
