<template>
  <v-app>
    <LandingHeader />

    <v-navigation-drawer
      v-model="orgDrawer"
      :rail="orgRail"
      permanent
      elevation="1"
    >
      <div class="px-3 py-4 d-flex align-center ga-2">
        <v-avatar color="primary" variant="tonal" size="30">
          <v-icon icon="mdi-domain" />
        </v-avatar>
        <div v-if="!orgRail" class="d-flex flex-column overflow-hidden">
          <span class="text-subtitle-2 font-weight-medium text-truncate">
            {{ currentOrg?.name || 'Organization' }}
          </span>
          <v-chip v-if="currentOrg?.status === 'PENDING_VALIDATION'" size="x-small" color="warning" class="mt-1 align-self-start">
            {{ t('org.status.pending') }}
          </v-chip>
        </div>
      </div>

      <v-divider />

      <v-list density="comfortable" nav class="pa-2">
        <v-list-item
          v-for="item in orgLinks"
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
            :prepend-icon="orgRail ? 'mdi-arrow-expand-horizontal' : 'mdi-arrow-collapse-horizontal'"
            @click="orgRail = !orgRail"
          >
            <template v-if="!orgRail">
              {{ t('org.sidebar.collapse') }}
            </template>
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <v-main>
      <v-container :class="containerClass" :fluid="isFluidWidth">
        <slot />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import LandingHeader from '~/components/landing/LandingHeader.vue'
import type { OrganizationResponse } from '~/composables/useCustomerOrganizationRepository'

const { t } = useI18n()
const route = useRoute()
const organizationRepository = useCustomerOrganizationRepository()

const orgDrawer = ref(true)
const orgRail = ref(false)

const currentOrgSlug = computed(() => route.params.slug as string)
const currentOrg = ref<OrganizationResponse | null>(null)

const isFluidWidth = computed(() => route.meta.width === 'fluid')
const containerClass = computed(() => 'py-6 px-lg-8')

const orgLinks = computed(() => [
  { to: `/org/${currentOrgSlug.value}`, icon: 'mdi-view-dashboard-outline', title: t('nav.org.dashboard') },
  { to: `/org/${currentOrgSlug.value}/keys`, icon: 'mdi-key-outline', title: t('nav.org.keys') },
  { to: `/org/${currentOrgSlug.value}/billing`, icon: 'mdi-credit-card-outline', title: t('nav.org.billing') },
  { to: `/org/${currentOrgSlug.value}/usage`, icon: 'mdi-chart-line', title: t('nav.org.usage') },
  { to: `/org/${currentOrgSlug.value}/settings`, icon: 'mdi-cog-outline', title: t('nav.org.settings') }
])

onMounted(async () => {
  if (currentOrgSlug.value) {
    try {
      currentOrg.value = await organizationRepository.getBySlug(currentOrgSlug.value)
    } catch {
      // Handle error if org is not accessible
    }
  }
})
</script>

