<template>
  <v-app>
    <!-- Main Header (Handled by LandingHeader component) -->
    <LandingHeader />

    <!-- Admin Sidebar (Only for /admin routes) -->
    <v-navigation-drawer
      v-if="isAdminRoute"
      v-model="adminDrawer"
      :rail="adminRail"
      permanent
      elevation="1"
      class="admin-sidebar"
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
            :prepend-icon="adminRail ? 'mdi-arrow-expand-horizontal' : 'mdi-arrow-collapse-horizontal'"
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
      <!-- Full Bleed Pages (Landing, Download, etc.) -->
      <slot v-if="isFullBleedPage" />

      <!-- Standard Pages -->
      <v-container
        v-else
        :class="containerClass"
        :fluid="isFluidWidth"
      >
        <slot />
      </v-container>
    </v-main>

    <!-- Global Footer -->
    <LandingFooter />
  </v-app>
</template>

<script setup lang="ts">
import LandingHeader from '~/components/landing/LandingHeader.vue'
import LandingFooter from '~/components/landing/LandingFooter.vue'

const { t } = useI18n()
const route = useRoute()

const adminDrawer = ref(true)
const adminRail = ref(false)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

// Full bleed pages (Landing, etc.) already include their own container logic if needed
const isFullBleedPage = computed(() =>
  route.path === '/' ||
  route.path.startsWith('/index-') ||
  route.path.startsWith('/offres') ||
  route.path.startsWith('/download') ||
  route.path.startsWith('/docs')
)

const isFluidWidth = computed(() => route.meta.width === 'fluid')

const containerClass = computed(() => [
  'py-6 px-lg-8',
  route.meta.width === 'semi-fluid' ? 'app-shell__container--semi-fluid' : ''
])

const adminLinks = computed(() => [
  { to: '/admin', icon: 'mdi-view-dashboard-outline', title: t('nav.admin_overview') },
  { to: '/admin/nodes', icon: 'mdi-lan-connect', title: t('admin.overview.actions.inspect_nodes') },
  { to: '/admin/keys', icon: 'mdi-key-outline', title: t('admin.overview.actions.create_key') },
  { to: '/admin/organisations', icon: 'mdi-domain', title: t('admin.overview.actions.manage_organizations') },
  { to: '/admin/users', icon: 'mdi-account-group-outline', title: t('admin.overview.actions.manage_users') },
  { to: '/admin/models', icon: 'mdi-cube-outline', title: t('nav.admin_models') },
  { to: '/admin/energy', icon: 'mdi-lightning-bolt-outline', title: t('nav.admin_energy') },
  { to: '/admin/benchmarks', icon: 'mdi-leaf-circle-outline', title: t('admin.overview.actions.benchmarks') },
  { to: '/admin/pentest', icon: 'mdi-shield-bug-outline', title: t('nav.admin_pentest') },
  { to: '/admin/docs', icon: 'mdi-book-open-variant', title: t('admin.overview.actions.admin_docs') }
])
</script>

<style scoped lang="scss">
.app-shell__container--semi-fluid {
  max-width: min(90vw, 1600px);
  width: 100%;
}

.admin-sidebar {
  border-right: 1px solid var(--inf-token-color-line-subtle) !important;
}

@media (max-width: 960px) {
  .app-shell__container--semi-fluid {
    max-width: 100%;
  }
}
</style>
