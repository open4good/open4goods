<template>
  <v-app>
    <LandingHeader />

    <!-- Dashboard sidebar -->
    <v-navigation-drawer
      v-if="isDashboardRoute"
      v-model="dashboardDrawer"
      :rail="dashboardRail"
      permanent
      elevation="1"
      class="app-sidebar"
    >
      <div class="px-3 py-4 d-flex align-center ga-2">
        <v-avatar color="primary" size="30">
          <v-icon icon="mdi-view-dashboard-outline" />
        </v-avatar>
        <span v-if="!dashboardRail" class="text-subtitle-2 font-weight-medium text-uppercase text-truncate">
          {{ t('nav.dashboard') }}
        </span>
      </div>

      <v-divider />

      <v-list density="comfortable" nav class="pa-2">
        <v-list-item
          v-for="item in dashboardLinks"
          :key="item.to"
          :to="item.to"
          :prepend-icon="item.icon"
          :title="item.title"
          class="mb-1 rounded-lg"
        />
      </v-list>

      <template #append>
        <div class="pa-3">
          <v-btn
            block
            variant="text"
            :icon="dashboardRail ? 'mdi-arrow-expand-horizontal' : undefined"
            :prepend-icon="dashboardRail ? undefined : 'mdi-arrow-collapse-horizontal'"
            @click="dashboardRail = !dashboardRail"
          >
            <template v-if="!dashboardRail">
              {{ t('admin.sidebar.collapse') }}
            </template>
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <!-- Admin sidebar -->
    <v-navigation-drawer
      v-if="isAdminRoute"
      v-model="adminDrawer"
      :rail="adminRail"
      permanent
      elevation="1"
      class="app-sidebar"
    >
      <div class="px-3 py-4 d-flex align-center ga-2">
        <v-avatar color="primary" size="30">
          <v-icon icon="mdi-shield-crown-outline" />
        </v-avatar>
        <span v-if="!adminRail" class="text-subtitle-2 font-weight-medium text-uppercase text-truncate">
          {{ t('admin.sidebar.title') }}
        </span>
      </div>

      <v-divider />

      <v-list density="comfortable" nav class="pa-2">
        <v-list-item
          v-for="item in adminLinks"
          :key="item.to"
          :to="item.to"
          :prepend-icon="item.icon"
          :title="item.title"
          class="mb-1 rounded-lg"
        />
      </v-list>

      <template #append>
        <div class="pa-3">
          <v-btn
            block
            variant="text"
            :icon="adminRail ? 'mdi-arrow-expand-horizontal' : undefined"
            :prepend-icon="adminRail ? undefined : 'mdi-arrow-collapse-horizontal'"
            @click="adminRail = !adminRail"
          >
            <template v-if="!adminRail">
              {{ t('admin.sidebar.collapse') }}
            </template>
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <!-- Content Area -->
    <v-main>
      <slot v-if="isFullBleedPage" />
      <v-container
        v-else
        :class="containerClass"
        :fluid="isFluidWidth"
      >
        <slot />
      </v-container>
    </v-main>

    <LandingFooter />
  </v-app>
</template>

<script setup lang="ts">
import LandingHeader from '~/components/landing/LandingHeader.vue'
import LandingFooter from '~/components/landing/LandingFooter.vue'

const { t } = useI18n()
const route = useRoute()

const dashboardDrawer = ref(true)
const dashboardRail = ref(false)
const adminDrawer = ref(true)
const adminRail = ref(false)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isDashboardRoute = computed(() => route.path.startsWith('/dashboard'))

const isFullBleedPage = computed(() =>
  route.path === '/' ||
  route.path.startsWith('/docs')
)

const isFluidWidth = computed(() => route.meta.width === 'fluid')

const containerClass = computed(() => [
  'py-6 px-lg-8',
  route.meta.width === 'semi-fluid' ? 'app-shell__container--semi-fluid' : ''
])

const dashboardLinks = computed(() => [
  { to: '/dashboard', icon: 'mdi-view-dashboard-outline', title: t('nav.org.dashboard') },
  { to: '/dashboard/api-keys', icon: 'mdi-key-variant', title: t('nav.org.keys') },
  { to: '/dashboard/usage', icon: 'mdi-chart-line', title: t('nav.org.usage') },
  { to: '/dashboard/billing', icon: 'mdi-credit-card-outline', title: t('nav.org.billing') },
  { to: '/dashboard/invoices', icon: 'mdi-receipt', title: t('dashboard.invoices.title') },
  { to: '/dashboard/settings', icon: 'mdi-cog-outline', title: t('nav.org.settings') },
])

const adminLinks = computed(() => [
  { to: '/admin', icon: 'mdi-view-dashboard-outline', title: t('admin.overview.title') },
  { to: '/admin/organizations', icon: 'mdi-domain', title: t('admin.organizations.title') },
  { to: '/admin/usage', icon: 'mdi-chart-line', title: t('admin.usage.title') },
  { to: '/admin/api-keys', icon: 'mdi-key-outline', title: t('admin.apiKeys.title') },
  { to: '/admin/audit', icon: 'mdi-history', title: t('admin.audit.title') },
])
</script>

<style scoped lang="scss">
.app-shell__container--semi-fluid {
  max-width: min(90vw, 1600px);
  width: 100%;
}

.app-sidebar {
  border-right: 1px solid var(--inf-token-color-line-subtle) !important;
}

@media (max-width: 960px) {
  .app-shell__container--semi-fluid {
    max-width: 100%;
  }
}
</style>
