<template>
  <div>
    <B2bPageHeader :title="t('admin.usage.title')" :subtitle="t('admin.usage.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <v-card rounded="lg" variant="outlined">
        <v-table density="compact">
          <thead>
            <tr>
              <th>{{ t('admin.usage.org') }}</th>
              <th>{{ t('admin.usage.apiKey') }}</th>
              <th>{{ t('admin.usage.facet') }}</th>
              <th>{{ t('admin.usage.gtin') }}</th>
              <th>{{ t('admin.usage.status') }}</th>
              <th>{{ t('admin.usage.billable') }}</th>
              <th>{{ t('admin.usage.credits') }}</th>
              <th>{{ t('admin.usage.date') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!events.length">
              <td colspan="8" class="text-center text-medium-emphasis py-6">{{ t('admin.usage.empty') }}</td>
            </tr>
            <tr v-for="ev in events" :key="ev.id">
              <td class="text-caption font-weight-medium">{{ ev.organizationName }}</td>
              <td class="text-mono text-caption">{{ ev.apiKeyPrefix ? `${ev.apiKeyPrefix}…` : '-' }}</td>
              <td class="text-caption">{{ ev.facetId }}</td>
              <td class="text-mono text-caption">{{ ev.gtin ?? '-' }}</td>
              <td>
                <v-chip size="x-small" :color="ev.httpStatus < 400 ? 'success' : 'error'" variant="tonal">
                  {{ ev.httpStatus }}
                </v-chip>
              </td>
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
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bAdminUsageEvent } from '~/domains/b2b/admin'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['admin'] })

const { t } = useI18n()
useSeoMeta({ title: t('admin.usage.seo.title') })

const admin = useAdminRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const events = ref<B2bAdminUsageEvent[]>([])

async function load() {
  loading.value = true
  error.value = null
  try {
    events.value = await admin.listUsage(100)
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
