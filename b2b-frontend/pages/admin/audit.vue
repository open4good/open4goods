<template>
  <div>
    <B2bPageHeader :title="t('admin.audit.title')" :subtitle="t('admin.audit.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <v-card rounded="lg" variant="outlined">
        <v-table density="compact">
          <thead>
            <tr>
              <th>{{ t('admin.audit.action') }}</th>
              <th>{{ t('admin.audit.actor') }}</th>
              <th>{{ t('admin.audit.targetOrg') }}</th>
              <th>{{ t('admin.audit.ref') }}</th>
              <th>{{ t('admin.audit.date') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!events.length">
              <td colspan="5" class="text-center text-medium-emphasis py-6">{{ t('admin.audit.empty') }}</td>
            </tr>
            <tr v-for="ev in events" :key="ev.id">
              <td>
                <v-chip size="x-small" color="primary" variant="tonal">{{ ev.action }}</v-chip>
              </td>
              <td class="text-caption">{{ ev.actorUserEmail }}</td>
              <td class="text-caption">{{ ev.targetOrganizationName ?? '-' }}</td>
              <td class="text-mono text-caption">{{ ev.targetRef ?? '-' }}</td>
              <td class="text-caption text-medium-emphasis">{{ formatDate(ev.createdAt) }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bAdminAuditEvent } from '~/domains/b2b/admin'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['admin'] })

const { t } = useI18n()
useSeoMeta({ title: t('admin.audit.seo.title') })

const admin = useAdminRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const events = ref<B2bAdminAuditEvent[]>([])

async function load() {
  loading.value = true
  error.value = null
  try {
    events.value = await admin.listAuditEvents(100)
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
