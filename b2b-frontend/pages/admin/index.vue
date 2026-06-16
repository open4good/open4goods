<template>
  <div>
    <B2bPageHeader :title="t('admin.overview.title')" :subtitle="t('admin.overview.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <div class="d-flex flex-column ga-6">
        <v-row dense>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('admin.overview.totalOrgs')"
              :value="organizations.length"
              icon="mdi-domain"
              color="primary"
            />
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('admin.overview.activeOrgs')"
              :value="activeOrgs"
              icon="mdi-domain-plus"
              color="success"
            />
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('admin.overview.totalKeys')"
              :value="apiKeys.length"
              icon="mdi-key-outline"
              color="info"
            />
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('admin.overview.recentRequests')"
              :value="usageEvents.length"
              icon="mdi-api"
              color="warning"
            />
          </v-col>
        </v-row>

        <div>
          <div class="d-flex align-center justify-space-between mb-3">
            <div class="text-subtitle-1 font-weight-bold">{{ t('admin.organizations.title') }}</div>
            <v-btn variant="text" to="/admin/organizations" append-icon="mdi-arrow-right" size="small">
              {{ t('admin.overview.viewAll') }}
            </v-btn>
          </div>
          <v-card rounded="lg" variant="outlined">
            <v-table density="compact">
              <thead>
                <tr>
                  <th>{{ t('admin.organizations.name') }}</th>
                  <th>{{ t('admin.organizations.status') }}</th>
                  <th>{{ t('admin.organizations.balance') }}</th>
                  <th>{{ t('admin.organizations.created') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!organizations.length">
                  <td colspan="4" class="text-center text-medium-emphasis py-4">{{ t('admin.organizations.empty') }}</td>
                </tr>
                <tr
                  v-for="org in organizations.slice(0, 5)"
                  :key="org.id"
                  class="cursor-pointer"
                  style="cursor: pointer"
                  @click="navigateTo(`/admin/organizations/${org.id}`)"
                >
                  <td class="font-weight-medium">{{ org.name }}</td>
                  <td><B2bStatusChip :status="org.status" size="x-small" /></td>
                  <td>{{ org.creditBalance }}</td>
                  <td class="text-caption text-medium-emphasis">{{ formatDate(org.createdAt) }}</td>
                </tr>
              </tbody>
            </v-table>
          </v-card>
        </div>

        <div>
          <div class="d-flex align-center justify-space-between mb-3">
            <div class="text-subtitle-1 font-weight-bold">{{ t('admin.usage.recent') }}</div>
            <v-btn variant="text" to="/admin/usage" append-icon="mdi-arrow-right" size="small">
              {{ t('admin.overview.viewAll') }}
            </v-btn>
          </div>
          <v-card rounded="lg" variant="outlined">
            <v-table density="compact">
              <thead>
                <tr>
                  <th>{{ t('admin.usage.org') }}</th>
                  <th>{{ t('admin.usage.gtin') }}</th>
                  <th>{{ t('admin.usage.billable') }}</th>
                  <th>{{ t('admin.usage.credits') }}</th>
                  <th>{{ t('admin.usage.date') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!usageEvents.length">
                  <td colspan="5" class="text-center text-medium-emphasis py-4">{{ t('admin.usage.empty') }}</td>
                </tr>
                <tr v-for="ev in usageEvents.slice(0, 10)" :key="ev.id">
                  <td class="text-caption">{{ ev.organizationName }}</td>
                  <td class="text-mono text-caption">{{ ev.gtin ?? '-' }}</td>
                  <td>
                    <v-icon
                      :icon="ev.billable ? 'mdi-check-circle' : 'mdi-minus-circle'"
                      :color="ev.billable ? 'success' : undefined"
                      size="16"
                    />
                  </td>
                  <td>{{ ev.creditsConsumed }}</td>
                  <td class="text-caption text-medium-emphasis">{{ formatDate(ev.createdAt) }}</td>
                </tr>
              </tbody>
            </v-table>
          </v-card>
        </div>
      </div>
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bAdminOrganization, B2bAdminUsageEvent } from '~/domains/b2b/admin'
import type { B2bApiKey } from '~/domains/b2b/keys'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['admin'] })

const { t } = useI18n()
useSeoMeta({ title: t('admin.overview.seo.title') })

const admin = useAdminRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const organizations = ref<B2bAdminOrganization[]>([])
const apiKeys = ref<B2bApiKey[]>([])
const usageEvents = ref<B2bAdminUsageEvent[]>([])

const activeOrgs = computed(() => organizations.value.filter(o => o.status === 'ACTIVE').length)

async function load() {
  loading.value = true
  error.value = null
  try {
    const [orgs, keys, usage] = await Promise.all([
      admin.listOrganizations(),
      admin.listApiKeys(),
      admin.listUsage(20)
    ])
    organizations.value = orgs
    apiKeys.value = keys
    usageEvents.value = usage
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

onMounted(load)
</script>
