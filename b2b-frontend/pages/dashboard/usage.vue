<template>
  <div>
    <B2bPageHeader :title="t('dashboard.usage.title')" :subtitle="t('dashboard.usage.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <div class="d-flex flex-column ga-6">
        <!-- Summary KPIs -->
        <v-row dense>
          <v-col cols="6" sm="3">
            <B2bKpiCard :label="t('dashboard.usage.totalRequests')" :value="totalRequests" icon="mdi-api" />
          </v-col>
          <v-col cols="6" sm="3">
            <B2bKpiCard :label="t('dashboard.usage.billable')" :value="billableRequests" icon="mdi-currency-eur" color="warning" />
          </v-col>
          <v-col cols="6" sm="3">
            <B2bKpiCard :label="t('dashboard.usage.noPay')" :value="noPayRequests" icon="mdi-shield-off-outline" color="success" />
          </v-col>
          <v-col cols="6" sm="3">
            <B2bKpiCard :label="t('dashboard.usage.creditsConsumed')" :value="creditsConsumed" icon="mdi-lightning-bolt" color="primary" />
          </v-col>
        </v-row>

        <!-- Transaction log -->
        <v-card rounded="lg" variant="outlined">
          <v-card-title class="text-subtitle-2 pa-4 pb-2">{{ t('dashboard.usage.log') }}</v-card-title>
          <v-table density="compact">
            <thead>
              <tr>
                <th>{{ t('dashboard.transactions.type') }}</th>
                <th>{{ t('dashboard.transactions.credits') }}</th>
                <th>{{ t('dashboard.transactions.facet') }}</th>
                <th>{{ t('dashboard.transactions.gtin') }}</th>
                <th>{{ t('dashboard.transactions.requestId') }}</th>
                <th>{{ t('dashboard.transactions.date') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!transactions.length">
                <td colspan="6" class="text-center text-medium-emphasis py-6">
                  {{ t('dashboard.transactions.empty') }}
                </td>
              </tr>
              <tr v-for="tx in transactions" :key="tx.id">
                <td><B2bStatusChip :status="tx.type" size="x-small" /></td>
                <td class="text-mono">{{ tx.type === 'DEBIT' ? `-${tx.credits}` : `+${tx.credits}` }}</td>
                <td class="text-caption">{{ tx.facetId ?? '-' }}</td>
                <td class="text-mono text-caption">{{ tx.gtin ?? '-' }}</td>
                <td class="text-mono text-caption" style="max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
                  {{ tx.requestId ?? '-' }}
                </td>
                <td class="text-caption text-medium-emphasis">{{ formatDate(tx.createdAt) }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </div>
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bTransaction } from '~/domains/b2b/billing'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['authenticated'] })

const { t } = useI18n()
useSeoMeta({ title: t('dashboard.usage.seo.title') })

const billing = useCustomerBillingRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const transactions = ref<B2bTransaction[]>([])

const totalRequests = computed(() => transactions.value.filter(t => t.type === 'DEBIT').length)
const billableRequests = computed(() => transactions.value.filter(t => t.type === 'DEBIT' && t.credits > 0).length)
const noPayRequests = computed(() => transactions.value.filter(t => t.type === 'DEBIT' && t.credits === 0).length)
const creditsConsumed = computed(() => transactions.value.filter(t => t.type === 'DEBIT').reduce((s, t) => s + t.credits, 0))

async function load() {
  loading.value = true
  error.value = null
  try {
    transactions.value = await billing.getTransactions(100)
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
