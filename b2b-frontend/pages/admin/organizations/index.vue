<template>
  <div>
    <B2bPageHeader :title="t('admin.organizations.title')" :subtitle="t('admin.organizations.subtitle')" />

    <div class="mb-4 d-flex ga-3 align-center">
      <v-text-field
        v-model="search"
        :label="t('admin.organizations.search')"
        prepend-inner-icon="mdi-magnify"
        variant="outlined"
        density="compact"
        clearable
        hide-details
        style="max-width: 320px"
      />
    </div>

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <v-card rounded="lg" variant="outlined">
        <v-table>
          <thead>
            <tr>
              <th>{{ t('admin.organizations.name') }}</th>
              <th>{{ t('admin.organizations.slug') }}</th>
              <th>{{ t('admin.organizations.status') }}</th>
              <th>{{ t('admin.organizations.balance') }}</th>
              <th>{{ t('admin.organizations.freeGrant') }}</th>
              <th>{{ t('admin.organizations.created') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!filtered.length">
              <td colspan="6" class="text-center text-medium-emphasis py-6">{{ t('admin.organizations.empty') }}</td>
            </tr>
            <tr
              v-for="org in filtered"
              :key="org.id"
              style="cursor: pointer"
              @click="navigateTo(`/admin/organizations/${org.id}`)"
            >
              <td class="font-weight-medium">{{ org.name }}</td>
              <td class="text-mono text-caption">{{ org.slug }}</td>
              <td><B2bStatusChip :status="org.status" size="x-small" /></td>
              <td>{{ org.creditBalance }}</td>
              <td>
                <v-icon
                  :icon="org.freeGrantApplied ? 'mdi-check-circle' : 'mdi-clock-outline'"
                  :color="org.freeGrantApplied ? 'success' : 'warning'"
                  size="16"
                />
              </td>
              <td class="text-caption text-medium-emphasis">{{ formatDate(org.createdAt) }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bAdminOrganization } from '~/domains/b2b/admin'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['admin'] })

const { t } = useI18n()
useSeoMeta({ title: t('admin.organizations.seo.title') })

const admin = useAdminRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const organizations = ref<B2bAdminOrganization[]>([])
const search = ref('')

const filtered = computed(() => {
  if (!search.value) return organizations.value
  const q = search.value.toLowerCase()
  return organizations.value.filter(o =>
    o.name.toLowerCase().includes(q) ||
    o.slug.toLowerCase().includes(q) ||
    (o.billingEmail?.toLowerCase().includes(q))
  )
})

async function load() {
  loading.value = true
  error.value = null
  try {
    organizations.value = await admin.listOrganizations()
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString()
}

onMounted(load)
</script>
