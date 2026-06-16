<template>
  <div>
    <B2bPageHeader :title="t('admin.apiKeys.title')" :subtitle="t('admin.apiKeys.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <v-card rounded="lg" variant="outlined">
        <v-table>
          <thead>
            <tr>
              <th>{{ t('dashboard.keys.name') }}</th>
              <th>{{ t('dashboard.keys.prefix') }}</th>
              <th>{{ t('dashboard.keys.status') }}</th>
              <th>{{ t('dashboard.keys.lastUsed') }}</th>
              <th>{{ t('dashboard.keys.created') }}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-if="!keys.length">
              <td colspan="6" class="text-center text-medium-emphasis py-6">{{ t('dashboard.keys.empty') }}</td>
            </tr>
            <tr v-for="key in keys" :key="key.id">
              <td class="font-weight-medium">{{ key.name }}</td>
              <td><code class="text-caption">{{ key.keyPrefix }}…</code></td>
              <td><B2bStatusChip :status="key.status" size="x-small" /></td>
              <td class="text-caption text-medium-emphasis">
                {{ key.lastUsedAt ? formatDate(key.lastUsedAt) : t('common.not_available') }}
              </td>
              <td class="text-caption text-medium-emphasis">{{ formatDate(key.createdAt) }}</td>
              <td>
                <v-btn
                  v-if="key.status === 'ACTIVE'"
                  icon="mdi-delete-outline"
                  size="x-small"
                  variant="text"
                  color="error"
                  :title="t('dashboard.keys.revoke')"
                  :loading="revokingId === key.id"
                  @click="doRevoke(key.id)"
                />
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bApiKey } from '~/domains/b2b/keys'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['admin'] })

const { t } = useI18n()
useSeoMeta({ title: t('admin.apiKeys.seo.title') })

const admin = useAdminRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const keys = ref<B2bApiKey[]>([])
const revokingId = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    keys.value = await admin.listApiKeys()
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

async function doRevoke(id: string) {
  revokingId.value = id
  try {
    await admin.revokeApiKey(id)
    await load()
  } finally {
    revokingId.value = null
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

onMounted(load)
</script>
