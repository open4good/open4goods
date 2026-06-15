<template>
  <div>
    <div class="d-flex align-center justify-space-between flex-wrap ga-3 mb-6">
      <div>
        <div class="text-overline text-primary font-weight-bold">{{ t('nav.org.dashboard') }}</div>
        <h1 class="text-h4 font-weight-bold">{{ orgName }}</h1>
      </div>
    </div>

    <v-row>
      <v-col cols="12" md="4">
        <v-card border rounded="lg" class="pa-4">
          <div class="d-flex align-center ga-3">
            <v-avatar color="primary" variant="tonal" rounded="lg">
              <v-icon>mdi-key-outline</v-icon>
            </v-avatar>
            <div>
              <div class="text-subtitle-2 text-medium-emphasis">{{ t('nav.org.keys') }}</div>
              <div class="text-h5 font-weight-bold">-</div>
            </div>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card border rounded="lg" class="pa-4">
          <div class="d-flex align-center ga-3">
            <v-avatar color="success" variant="tonal" rounded="lg">
              <v-icon>mdi-credit-card-outline</v-icon>
            </v-avatar>
            <div>
              <div class="text-subtitle-2 text-medium-emphasis">{{ t('nav.org.billing') }}</div>
              <div class="text-h5 font-weight-bold">-</div>
            </div>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card border rounded="lg" class="pa-4">
          <div class="d-flex align-center ga-3">
            <v-avatar color="info" variant="tonal" rounded="lg">
              <v-icon>mdi-chart-line</v-icon>
            </v-avatar>
            <div>
              <div class="text-subtitle-2 text-medium-emphasis">{{ t('nav.org.usage') }}</div>
              <div class="text-h5 font-weight-bold">-</div>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import type { OrganizationResponse } from '~/composables/useCustomerOrganizationRepository'

definePageMeta({ layout: 'org', middleware: ['authenticated'], width: 'semi-fluid' })

const { t } = useI18n()
const route = useRoute()
const organizationRepository = useCustomerOrganizationRepository()

const slug = computed(() => route.params.slug as string)

const { data: currentOrg } = await useAsyncData<OrganizationResponse | null>(
  `org-${slug.value}`,
  () => organizationRepository.getBySlug(slug.value)
)

const orgName = computed(() => currentOrg.value?.name || slug.value)

useSeoMeta({
  title: () => `${orgName.value} - ${t('nav.org.dashboard')}`
})
</script>
