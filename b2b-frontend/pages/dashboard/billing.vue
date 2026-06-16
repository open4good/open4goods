<template>
  <div>
    <B2bPageHeader :title="t('dashboard.billing.title')" :subtitle="t('dashboard.billing.subtitle')" />

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <div class="d-flex flex-column ga-6">
        <!-- Balance -->
        <v-card rounded="lg" variant="outlined" class="pa-4">
          <div class="d-flex align-center ga-4 flex-wrap">
            <div>
              <div class="text-overline text-primary">{{ t('dashboard.billing.balance') }}</div>
              <div class="text-h3 font-weight-bold">{{ balance?.creditsRemaining ?? 0 }}</div>
              <div class="text-caption text-medium-emphasis">{{ t('dashboard.billing.creditsUnit') }}</div>
            </div>
            <v-spacer />
            <div class="d-flex flex-column ga-2">
              <v-btn
                v-if="hasActiveSubscription"
                variant="outlined"
                prepend-icon="mdi-open-in-new"
                :loading="redirecting"
                @click="openPortal"
              >
                {{ t('dashboard.billing.manageSubscription') }}
              </v-btn>
            </div>
          </div>

          <!-- Bucket details -->
          <v-expand-transition>
            <div v-if="balance?.buckets?.length" class="mt-4">
              <v-divider class="mb-3" />
              <div class="text-caption text-medium-emphasis mb-2">{{ t('dashboard.billing.buckets') }}</div>
              <v-row dense>
                <v-col v-for="bucket in balance.buckets" :key="bucket.id" cols="12" sm="6" md="4">
                  <v-card variant="tonal" color="surface-variant" rounded="lg" class="pa-3">
                    <div class="d-flex justify-space-between align-center mb-1">
                      <B2bStatusChip :status="bucket.kind" size="x-small" />
                      <span class="text-caption font-weight-bold">{{ bucket.creditsRemaining }} / {{ bucket.creditsTotal }}</span>
                    </div>
                    <v-progress-linear
                      :model-value="bucket.creditsTotal ? (bucket.creditsRemaining / bucket.creditsTotal) * 100 : 0"
                      color="primary"
                      rounded
                      height="4"
                    />
                    <div v-if="bucket.expiresAt" class="text-caption text-medium-emphasis mt-1">
                      {{ t('dashboard.billing.expiresAt', { date: formatDate(bucket.expiresAt) }) }}
                    </div>
                  </v-card>
                </v-col>
              </v-row>
            </div>
          </v-expand-transition>
        </v-card>

        <!-- Active subscription -->
        <div v-if="subscriptions.length">
          <div class="text-subtitle-1 font-weight-bold mb-3">{{ t('dashboard.billing.subscription') }}</div>
          <v-card
            v-for="sub in subscriptions"
            :key="sub.id"
            rounded="lg"
            variant="outlined"
            class="pa-4 mb-3"
          >
            <div class="d-flex align-center ga-3 flex-wrap">
              <div>
                <div class="font-weight-bold">{{ sub.catalogId }}</div>
                <div class="text-caption text-medium-emphasis">
                  {{ t('dashboard.billing.renewsAt', { date: sub.currentPeriodEnd ? formatDate(sub.currentPeriodEnd) : '-' }) }}
                </div>
              </div>
              <v-spacer />
              <B2bStatusChip :status="sub.status" size="small" />
            </div>
          </v-card>
        </div>

        <!-- Billing catalog -->
        <div v-if="catalogItems.length">
          <div class="text-subtitle-1 font-weight-bold mb-3">{{ t('dashboard.billing.topUp') }}</div>
          <B2bBillingCatalog
            :items="catalogItems"
            :cta-label="t('dashboard.billing.buy')"
            @select="handleCatalogSelect"
          />
        </div>
      </div>
    </B2bAsyncState>
  </div>
</template>

<script setup lang="ts">
import type { B2bBalanceResponse, B2bSubscription, BillingCatalog } from '~/domains/b2b/billing'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['authenticated'] })

const { t } = useI18n()
useSeoMeta({ title: t('dashboard.billing.seo.title') })

const billing = useCustomerBillingRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const redirecting = ref(false)
const balance = ref<B2bBalanceResponse | null>(null)
const subscriptions = ref<B2bSubscription[]>([])
const catalog = ref<BillingCatalog | null>(null)

const hasActiveSubscription = computed(() => subscriptions.value.some(s => s.status === 'active'))

const catalogItems = computed(() => [
  ...(catalog.value?.packs ?? []).map(p => ({
    id: `pack:${p.id}`,
    name: `${p.credits} credits`,
    price: `€${p.amountEur}`,
    credits: p.credits
  })),
  ...(catalog.value?.subscriptions ?? []).map(s => ({
    id: `sub:${s.id}`,
    name: `${s.monthlyCredits} credits/month`,
    price: `€${s.amountEur}/mo`,
    credits: s.monthlyCredits,
    badge: 'Monthly'
  }))
])

function handleCatalogSelect(id: string) {
  if (id.startsWith('pack:')) {
    checkoutPack(id.slice(5))
  } else {
    checkoutSubscription(id.slice(4))
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [bal, subs, cat] = await Promise.all([
      billing.getBalance(),
      billing.getSubscriptions(),
      $fetch<BillingCatalog>('/api/v1/customer/billing/catalog', {
        baseURL: useRuntimeConfig().public.backendBaseUrl,
        credentials: 'include'
      })
    ])
    balance.value = bal
    subscriptions.value = subs
    catalog.value = cat
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

async function checkoutPack(catalogId: string) {
  redirecting.value = true
  try {
    const res = await billing.checkoutPack(catalogId)
    await navigateTo(res.url, { external: true })
  } finally {
    redirecting.value = false
  }
}

async function checkoutSubscription(catalogId: string) {
  redirecting.value = true
  try {
    const res = await billing.checkoutSubscription(catalogId)
    await navigateTo(res.url, { external: true })
  } finally {
    redirecting.value = false
  }
}

async function openPortal() {
  redirecting.value = true
  try {
    const res = await billing.openPortal()
    await navigateTo(res.url, { external: true })
  } finally {
    redirecting.value = false
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString()
}

onMounted(load)
</script>
