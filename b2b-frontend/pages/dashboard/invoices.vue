<template>
  <div>
    <B2bPageHeader :title="t('dashboard.invoices.title')" :subtitle="t('dashboard.invoices.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <v-card rounded="lg" variant="outlined">
        <v-table>
          <thead>
            <tr>
              <th>{{ t('dashboard.invoices.id') }}</th>
              <th>{{ t('dashboard.invoices.amount') }}</th>
              <th>{{ t('dashboard.invoices.credits') }}</th>
              <th>{{ t('dashboard.invoices.status') }}</th>
              <th>{{ t('dashboard.invoices.date') }}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-if="!invoices.length">
              <td colspan="6" class="text-center text-medium-emphasis py-6">
                {{ t('dashboard.invoices.empty') }}
              </td>
            </tr>
            <tr v-for="inv in invoices" :key="inv.id">
              <td class="text-caption text-mono">{{ inv.stripeInvoiceId }}</td>
              <td>{{ formatAmount(inv.amountCents, inv.currency) }}</td>
              <td>{{ inv.creditsGranted ?? '-' }}</td>
              <td><B2bStatusChip :status="inv.status" size="x-small" /></td>
              <td class="text-caption text-medium-emphasis">{{ formatDate(inv.createdAt) }}</td>
              <td>
                <v-btn
                  v-if="inv.hostedInvoiceUrl"
                  size="x-small"
                  variant="text"
                  icon="mdi-open-in-new"
                  :href="inv.hostedInvoiceUrl"
                  target="_blank"
                  rel="noopener"
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
import type { B2bInvoice } from '~/domains/b2b/billing'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['authenticated'] })

const { t } = useI18n()
useSeoMeta({ title: t('dashboard.invoices.seo.title') })

const billing = useCustomerBillingRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const invoices = ref<B2bInvoice[]>([])

async function load() {
  loading.value = true
  error.value = null
  try {
    invoices.value = await billing.getInvoices()
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

function formatAmount(cents: number, currency: string) {
  return new Intl.NumberFormat('en', { style: 'currency', currency: currency.toUpperCase() }).format(cents / 100)
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString()
}

onMounted(load)
</script>
