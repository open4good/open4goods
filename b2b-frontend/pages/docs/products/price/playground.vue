<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="11">
        <B2bPageHeader
          :title="t('playground.title')"
          :subtitle="t('playground.subtitle')"
        />

        <!-- Mode tabs -->
        <v-tabs v-model="mode" class="mb-6" color="primary">
          <v-tab value="sample">
            <v-icon start icon="mdi-flask-outline" />
            {{ t('playground.sampleMode') }}
          </v-tab>
          <v-tab value="live" :disabled="!isAuthenticated">
            <v-icon start icon="mdi-lightning-bolt" />
            {{ t('playground.liveMode') }}
            <v-chip v-if="!isAuthenticated" size="x-small" class="ml-2" color="warning" variant="tonal">
              {{ t('playground.loginRequired') }}
            </v-chip>
          </v-tab>
        </v-tabs>

        <!-- Sign-in prompt for live mode -->
        <v-alert
          v-if="!isAuthenticated"
          type="info"
          variant="tonal"
          class="mb-6"
          :text="t('playground.liveSignInHint')"
        >
          <template #append>
            <v-btn :to="localePath('/auth/login')" size="small" variant="flat" color="primary">
              {{ t('playground.signIn') }}
            </v-btn>
          </template>
        </v-alert>

        <B2bPlaygroundShell>
          <!-- Request panel -->
          <template #request>
            <v-card variant="outlined" height="100%">
              <v-card-title class="text-body-2 font-weight-medium pa-4 pb-0">
                <v-icon icon="mdi-send-outline" size="16" class="mr-1" />
                {{ t('playground.request') }}
              </v-card-title>
              <v-card-text class="pa-4">
                <v-text-field
                  v-model="gtin"
                  :label="t('playground.gtinLabel')"
                  :placeholder="t('playground.gtinPlaceholder')"
                  variant="outlined"
                  density="comfortable"
                  prepend-inner-icon="mdi-barcode"
                  class="mb-3"
                  clearable
                />

                <v-select
                  v-model="language"
                  :label="t('playground.languageLabel')"
                  :items="languageOptions"
                  item-title="label"
                  item-value="value"
                  variant="outlined"
                  density="comfortable"
                  class="mb-3"
                />

                <template v-if="mode === 'live' && isAuthenticated">
                  <v-select
                    v-model="selectedKeyId"
                    :label="t('playground.apiKeyLabel')"
                    :items="keyOptions"
                    item-title="label"
                    item-value="value"
                    variant="outlined"
                    density="comfortable"
                    :loading="keysLoading"
                    class="mb-3"
                  />
                </template>

                <v-btn
                  block
                  color="primary"
                  size="large"
                  :loading="running"
                  :disabled="!canRun"
                  prepend-icon="mdi-play"
                  @click="run"
                >
                  {{ t('playground.run') }}
                </v-btn>

                <!-- Request preview -->
                <div class="mt-4">
                  <div class="text-caption text-medium-emphasis mb-1">{{ t('playground.requestPreview') }}</div>
                  <B2bCodeBlock :code="requestPreview" language="http" />
                </div>
              </v-card-text>
            </v-card>
          </template>

          <!-- Response panel -->
          <template #response>
            <v-card variant="outlined" height="100%">
              <v-card-title class="text-body-2 font-weight-medium pa-4 pb-0 d-flex align-center">
                <v-icon icon="mdi-code-json" size="16" class="mr-1" />
                {{ t('playground.response') }}
                <v-spacer />
                <template v-if="result">
                  <v-chip
                    :color="result.meta?.billable ? 'success' : 'default'"
                    size="x-small"
                    variant="tonal"
                    class="mr-2"
                  >
                    {{ result.meta?.billable ? t('playground.billable') : t('playground.notBillable') }}
                  </v-chip>
                  <v-chip size="x-small" variant="tonal">
                    {{ result.meta?.creditsConsumed ?? 0 }} {{ t('playground.credits') }}
                  </v-chip>
                </template>
              </v-card-title>
              <v-card-text class="pa-4">
                <!-- Empty state -->
                <div v-if="!result && !runError && !running" class="text-center py-10 text-medium-emphasis">
                  <v-icon icon="mdi-flask-outline" size="48" class="mb-3 d-block mx-auto" />
                  <div>{{ t('playground.emptyHint') }}</div>
                </div>

                <!-- Loading -->
                <v-progress-linear v-else-if="running" indeterminate color="primary" class="my-8" />

                <!-- Error -->
                <v-alert v-else-if="runError" type="error" variant="tonal" class="mb-4">
                  <div class="font-weight-medium mb-1">{{ runError.title ?? t('playground.error') }}</div>
                  <div class="text-body-2">{{ runError.detail }}</div>
                </v-alert>

                <!-- Result -->
                <template v-else-if="result">
                  <!-- Metering strip -->
                  <v-row dense class="mb-4">
                    <v-col cols="6" md="3">
                      <div class="text-caption text-medium-emphasis">{{ t('playground.meta.reason') }}</div>
                      <div class="font-weight-medium text-body-2">{{ result.meta?.reason ?? '—' }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-medium-emphasis">{{ t('playground.meta.creditsConsumed') }}</div>
                      <div class="font-weight-medium text-body-2">{{ result.meta?.creditsConsumed ?? 0 }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-medium-emphasis">{{ t('playground.meta.creditsRemaining') }}</div>
                      <div class="font-weight-medium text-body-2">{{ result.meta?.creditsRemaining ?? '—' }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-medium-emphasis">{{ t('playground.meta.responseTime') }}</div>
                      <div class="font-weight-medium text-body-2">{{ result.meta?.responseTimeMs ?? '—' }} ms</div>
                    </v-col>
                  </v-row>

                  <B2bCodeBlock :code="JSON.stringify(result, null, 2)" language="json" />
                </template>
              </v-card-text>
            </v-card>
          </template>

          <!-- Examples rail -->
          <template #examples>
            <v-card variant="outlined">
              <v-card-title class="text-body-2 font-weight-medium pa-4 pb-0">
                <v-icon icon="mdi-bookmark-multiple-outline" size="16" class="mr-1" />
                {{ t('playground.examples') }}
              </v-card-title>
              <v-card-text class="pa-4">
                <v-row>
                  <v-col
                    v-for="ex in examples"
                    :key="ex.gtin"
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-card
                      variant="tonal"
                      class="cursor-pointer"
                      @click="applyExample(ex)"
                    >
                      <v-card-text class="pa-3">
                        <div class="font-weight-medium text-body-2 mb-1">{{ ex.label }}</div>
                        <code class="text-caption text-medium-emphasis">{{ ex.gtin }}</code>
                        <div class="text-caption mt-1">
                          <v-chip size="x-small" :color="ex.billable ? 'success' : 'default'" variant="tonal">
                            {{ ex.billable ? t('playground.billable') : t('playground.notBillable') }}
                          </v-chip>
                        </div>
                      </v-card-text>
                    </v-card>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </template>
        </B2bPlaygroundShell>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
interface PlaygroundMeta {
  requestId: string
  gtin: string
  facet: string
  billable: boolean
  creditsConsumed: number
  creditsRemaining: number
  reason: string
  responseTimeMs: number
}

interface PlaygroundResponse {
  meta: PlaygroundMeta | null
  data: Record<string, unknown> | null
}

definePageMeta({ width: 'fluid' })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { session } = useAuthSession()
const { post, get } = useApiClient()

useLocalizedPageSeo({
  titleKey: 'playground.seo.title',
  descriptionKey: 'playground.seo.description'
})

const isAuthenticated = computed(() => !!session.value?.user)

const mode = ref<'sample' | 'live'>('sample')
const gtin = ref('0885909950805')
const language = ref('en')
const selectedKeyId = ref<string | null>(null)
const running = ref(false)
const result = ref<PlaygroundResponse | null>(null)
const runError = ref<{ title?: string, detail?: string } | null>(null)

const languageOptions = [
  { label: 'English', value: 'en' },
  { label: 'Français', value: 'fr' }
]

const examples = computed(() => [
  { label: t('playground.example.fresh'), gtin: '0885909950805', billable: true },
  { label: t('playground.example.stale'), gtin: '0194253408994', billable: false },
  { label: t('playground.example.notFound'), gtin: '0000000000000', billable: false },
  { label: t('playground.example.invalid'), gtin: '12345', billable: false }
])

function applyExample(ex: { gtin: string }) {
  gtin.value = ex.gtin
}

// API keys for live mode
interface ApiKeyOption { label: string, value: string }
const keyOptions = ref<ApiKeyOption[]>([])
const keysLoading = ref(false)

watch([isAuthenticated, mode], async ([auth, m]) => {
  if (!auth || m !== 'live') return
  keysLoading.value = true
  try {
    const keys = await get<{ id: string, name: string, keyPrefix: string, status: string }[]>('/api/v1/customer/api-keys')
    keyOptions.value = keys
      .filter((k) => k.status === 'ACTIVE')
      .map((k) => ({ label: `${k.name} (${k.keyPrefix}…)`, value: k.id }))
    if (keyOptions.value[0]) selectedKeyId.value = keyOptions.value[0].value
  } catch {
    // ignore
  } finally {
    keysLoading.value = false
  }
}, { immediate: true })

const canRun = computed(() => {
  if (!gtin.value?.trim()) return false
  if (mode.value === 'live' && !selectedKeyId.value) return false
  return true
})

const requestPreview = computed(() => {
  const g = gtin.value?.trim() || '{gtin}'
  const lang = language.value ? `?language=${language.value}` : ''
  const keyStr = mode.value === 'sample' ? 'pdapi_sample_key' : 'pdapi_YOUR_KEY'
  return `GET /api/v1/products/${g}/price${lang}\nAuthorization: Bearer ${keyStr}`
})

// Static sample responses keyed by gtin
const sampleResponses: Record<string, PlaygroundResponse> = {
  '0885909950805': {
    meta: { requestId: 'req_sample_01', gtin: '885909950805', facet: 'product.price', billable: true, creditsConsumed: 5, creditsRemaining: 2495, reason: 'fresh-offer', responseTimeMs: 28 },
    data: { bestPrice: { price: 699.0, currency: 'EUR', merchant: 'TechStore FR', condition: 'new' }, offers: [{ price: 699.0, currency: 'EUR', merchant: 'TechStore FR', condition: 'new', lastSeenDays: 1 }, { price: 739.0, currency: 'EUR', merchant: 'BigBox Online', condition: 'new', lastSeenDays: 4 }], offerCount: 2, freshness: { oldestOfferDays: 4, newestOfferDays: 1, windowDays: 30 } }
  },
  '0194253408994': {
    meta: { requestId: 'req_sample_02', gtin: '194253408994', facet: 'product.price', billable: false, creditsConsumed: 0, creditsRemaining: 2495, reason: 'stale-data', responseTimeMs: 11 },
    data: null
  },
  '0000000000000': {
    meta: { requestId: 'req_sample_03', gtin: '0000000000000', facet: 'product.price', billable: false, creditsConsumed: 0, creditsRemaining: 2495, reason: 'product-not-found', responseTimeMs: 8 },
    data: null
  }
}

async function run() {
  if (!canRun.value) return
  running.value = true
  result.value = null
  runError.value = null

  try {
    if (mode.value === 'sample') {
      await new Promise((r) => setTimeout(r, 350))
      const key = gtin.value.trim()
      if (key.length < 6 || !/^\d+$/.test(key)) {
        runError.value = { title: 'Invalid GTIN', detail: `GTIN '${key}' failed checksum validation.` }
        return
      }
      result.value = sampleResponses[key] ?? {
        meta: { requestId: 'req_sample_04', gtin: key, facet: 'product.price', billable: false, creditsConsumed: 0, creditsRemaining: 2495, reason: 'product-not-found', responseTimeMs: 9 },
        data: null
      }
    } else {
      result.value = await post<PlaygroundResponse>('/api/v1/customer/playground/products/price', {
        apiKeyId: selectedKeyId.value,
        gtin: gtin.value.trim(),
        language: language.value
      })
    }
  } catch (err) {
    const e = err as { problem?: { title?: string }, details?: string }
    runError.value = { title: e.problem?.title ?? 'Error', detail: e.details ?? String(err) }
  } finally {
    running.value = false
  }
}

watch(locale, () => {
  language.value = locale.value
})
</script>
