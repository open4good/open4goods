<template>
  <div>
    <B2bPageHeader :title="t('dashboard.overview.title')" :subtitle="t('dashboard.overview.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <div class="d-flex flex-column ga-6">
        <!-- KPI strip -->
        <v-row dense>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('dashboard.overview.creditsRemaining')"
              :value="balance?.creditsRemaining ?? 0"
              icon="mdi-lightning-bolt-circle"
              color="primary"
            />
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('dashboard.overview.activeKeys')"
              :value="activeKeys"
              icon="mdi-key-variant"
              color="success"
            />
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('dashboard.overview.recentRequests')"
              :value="recentDebits"
              icon="mdi-api"
              color="info"
            />
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <B2bKpiCard
              :label="t('dashboard.overview.billableRequests')"
              :value="recentBillable"
              icon="mdi-currency-eur"
              color="warning"
            />
          </v-col>
        </v-row>

        <!-- Setup checklist (shown until first API key created) -->
        <v-card v-if="showChecklist" variant="tonal" color="primary" rounded="lg">
          <v-card-title class="text-subtitle-1 font-weight-bold">
            {{ t('dashboard.overview.checklist.title') }}
          </v-card-title>
          <v-card-text>
            <v-list dense>
              <v-list-item prepend-icon="mdi-check-circle" :title="t('dashboard.overview.checklist.account')" />
              <v-list-item
                :prepend-icon="hasApiKey ? 'mdi-check-circle' : 'mdi-circle-outline'"
                :title="t('dashboard.overview.checklist.createKey')"
                :to="hasApiKey ? undefined : '/dashboard/api-keys'"
              />
              <v-list-item
                prepend-icon="mdi-circle-outline"
                :title="t('dashboard.overview.checklist.firstCall')"
                to="/docs/products/price/playground"
              />
            </v-list>
          </v-card-text>
        </v-card>

        <!-- Recent transactions -->
        <div>
          <div class="text-subtitle-1 font-weight-bold mb-3">{{ t('dashboard.overview.recentActivity') }}</div>
          <v-card rounded="lg" variant="outlined">
            <v-table density="compact">
              <thead>
                <tr>
                  <th>{{ t('dashboard.transactions.type') }}</th>
                  <th>{{ t('dashboard.transactions.credits') }}</th>
                  <th>{{ t('dashboard.transactions.gtin') }}</th>
                  <th>{{ t('dashboard.transactions.date') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!transactions.length">
                  <td colspan="4" class="text-center text-medium-emphasis py-4">
                    {{ t('dashboard.transactions.empty') }}
                  </td>
                </tr>
                <tr v-for="tx in transactions.slice(0, 10)" :key="tx.id">
                  <td>
                    <B2bStatusChip :status="tx.type" size="x-small" />
                  </td>
                  <td class="text-mono">{{ tx.type === 'DEBIT' ? `-${tx.credits}` : `+${tx.credits}` }}</td>
                  <td class="text-mono text-caption">{{ tx.gtin ?? '-' }}</td>
                  <td class="text-caption text-medium-emphasis">{{ formatDate(tx.createdAt) }}</td>
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
import type { B2bBalanceResponse, B2bTransaction } from '~/domains/b2b/billing'
import type { B2bApiKey } from '~/domains/b2b/keys'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['authenticated'] })

const { t } = useI18n()
useSeoMeta({ title: t('dashboard.overview.seo.title') })

const billing = useCustomerBillingRepository()
const keysRepo = useB2bApiKeysRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const balance = ref<B2bBalanceResponse | null>(null)
const transactions = ref<B2bTransaction[]>([])
const keys = ref<B2bApiKey[]>([])

const activeKeys = computed(() => keys.value.filter(k => k.status === 'ACTIVE').length)
const hasApiKey = computed(() => keys.value.length > 0)
const showChecklist = computed(() => !hasApiKey.value)
const recentDebits = computed(() => transactions.value.filter(t => t.type === 'DEBIT').length)
const recentBillable = computed(() => recentDebits.value)

async function load() {
  loading.value = true
  error.value = null
  try {
    const [bal, txs, ks] = await Promise.all([
      billing.getBalance(),
      billing.getTransactions(20),
      keysRepo.list()
    ])
    balance.value = bal
    transactions.value = txs
    keys.value = ks
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
