<template>
  <div>
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
        <v-card variant="outlined" class="d-flex flex-column h-100">
          <v-card-title class="text-body-2 font-weight-medium pa-4 pb-0">
            <v-icon icon="mdi-send-outline" size="16" class="mr-1" />
            {{ t('playground.request') }}
          </v-card-title>
          <v-card-text class="pa-4 flex-grow-1">
            <v-text-field
              v-model="barcode"
              :label="t('barcodes.check.playground.barcodeLabel')"
              :placeholder="t('barcodes.check.playground.barcodePlaceholder')"
              variant="outlined"
              density="comfortable"
              prepend-inner-icon="mdi-barcode-scan"
              class="mb-3"
              clearable
              @keyup.enter="run"
            />

            <!-- Live mode: API key selector -->
            <B2bApiKeySelector
              v-if="mode === 'live' && isAuthenticated"
              v-model="selectedApiKeyId"
              class="mb-3"
            />
          </v-card-text>
          <v-card-actions class="pa-4 pt-0">
            <v-btn
              color="primary"
              variant="flat"
              :loading="loading"
              :disabled="!barcode || barcode.trim().length === 0"
              @click="run"
            >
              <v-icon start icon="mdi-play" />
              {{ t('barcodes.check.playground.run') }}
            </v-btn>
            <v-btn
              v-if="result || error"
              variant="text"
              @click="clear"
            >
              {{ t('barcodes.check.playground.clearLabel') }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </template>

      <!-- Response panel -->
      <template #response>
        <v-card variant="outlined" class="d-flex flex-column h-100">
          <v-card-title class="text-body-2 font-weight-medium pa-4 pb-0">
            <v-icon icon="mdi-receipt-text-outline" size="16" class="mr-1" />
            {{ t('playground.response') }}
          </v-card-title>
          <v-card-text class="pa-4 flex-grow-1">
            <!-- Idle state -->
            <div v-if="!result && !error && !loading" class="text-medium-emphasis text-body-2 text-center pa-6">
              <v-icon icon="mdi-barcode-scan" size="40" class="mb-2 d-block mx-auto" />
              Enter a barcode above and click Check to see the forensic analysis and product teaser.
            </div>

            <!-- Loading -->
            <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

            <!-- Error -->
            <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
              {{ error }}
            </v-alert>

            <!-- Result -->
            <div v-if="result">
              <!-- Validity chip + type -->
              <div class="d-flex align-center flex-wrap ga-2 mb-4">
                <v-chip
                  :color="result.forensics.valid ? 'success' : 'error'"
                  variant="tonal"
                  size="small"
                  prepend-icon="mdi-check-circle"
                >
                  {{ result.forensics.valid ? 'Valid checksum' : 'Invalid checksum' }}
                </v-chip>
                <v-chip v-if="result.forensics.type" color="primary" variant="tonal" size="small">
                  {{ result.forensics.type }}
                </v-chip>
                <v-chip v-if="result.forensics.gs1ClassLabel" variant="outlined" size="small">
                  {{ result.forensics.gs1ClassLabel }}
                </v-chip>
              </div>

              <!-- Forensics details -->
              <v-table density="compact" class="mb-4">
                <tbody>
                  <tr v-if="result.forensics.issuingCountryCode">
                    <td class="text-medium-emphasis" style="width:50%">GS1 Issuing country</td>
                    <td>
                      <img
                        v-if="result.forensics.flagUrl"
                        :src="result.forensics.flagUrl"
                        :alt="result.forensics.issuingCountryCode"
                        height="14"
                        class="mr-1 d-inline-block align-middle"
                      />
                      {{ result.forensics.issuingCountryName || result.forensics.issuingCountryCode }}
                      <span class="text-medium-emphasis text-caption ml-1">({{ result.forensics.gs1Prefix }})</span>
                    </td>
                  </tr>
                  <tr v-if="result.forensics.normalizedGtin13">
                    <td class="text-medium-emphasis">GTIN-13</td>
                    <td><code>{{ result.forensics.normalizedGtin13 }}</code></td>
                  </tr>
                  <tr v-if="result.forensics.normalizedGtin14">
                    <td class="text-medium-emphasis">GTIN-14</td>
                    <td><code>{{ result.forensics.normalizedGtin14 }}</code></td>
                  </tr>
                  <tr v-if="result.forensics.checkDigit !== null && result.forensics.checkDigit !== undefined">
                    <td class="text-medium-emphasis">Check digit</td>
                    <td>{{ result.forensics.checkDigit }}</td>
                  </tr>
                  <tr v-if="result.forensics.packagingIndicator !== null && result.forensics.packagingIndicator !== undefined">
                    <td class="text-medium-emphasis">Packaging indicator</td>
                    <td>{{ result.forensics.packagingIndicator }}</td>
                  </tr>
                  <tr v-if="result.forensics.isbnRegistrationGroup">
                    <td class="text-medium-emphasis">ISBN registration group</td>
                    <td>{{ result.forensics.isbnRegistrationGroup }}</td>
                  </tr>
                </tbody>
              </v-table>

              <!-- Product teaser -->
              <v-card v-if="result.product" variant="outlined" class="mb-4">
                <v-card-text>
                  <div class="d-flex align-start ga-3">
                    <v-img
                      v-if="result.product.coverImageUrl"
                      :src="result.product.coverImageUrl"
                      width="64"
                      height="64"
                      cover
                      class="rounded flex-shrink-0"
                    />
                    <div class="flex-grow-1">
                      <div class="text-body-2 font-weight-medium mb-1">{{ result.product.title || 'Unknown product' }}</div>
                      <div class="text-caption text-medium-emphasis mb-2">{{ result.product.gtin }}</div>
                      <div class="d-flex align-center ga-3 flex-wrap">
                        <span v-if="result.product.bestPrice" class="text-h6 font-weight-bold text-success">
                          {{ formatPrice(result.product.bestPrice, result.product.currency) }}
                        </span>
                        <v-chip size="x-small" variant="tonal" color="secondary">
                          {{ result.product.offersCount }} offer{{ result.product.offersCount !== 1 ? 's' : '' }}
                        </v-chip>
                      </div>
                    </div>
                  </div>
                  <v-btn
                    v-if="result.product.productUrl"
                    :href="result.product.productUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    variant="tonal"
                    size="small"
                    color="primary"
                    class="mt-3"
                    prepend-icon="mdi-open-in-new"
                  >
                    View on nudger.fr
                  </v-btn>
                </v-card-text>
              </v-card>

              <!-- No product found -->
              <v-alert
                v-else-if="result.forensics.valid"
                type="info"
                variant="tonal"
                density="compact"
              >
                No matching product found in the nudger.fr index.
              </v-alert>
            </div>
          </v-card-text>
        </v-card>
      </template>
    </B2bPlaygroundShell>
  </div>
</template>

<script setup lang="ts">
interface BarcodeForensicsDto {
  valid: boolean
  type: string | null
  gs1Prefix: string | null
  issuingCountryCode: string | null
  issuingCountryName: string | null
  flagUrl: string | null
  gs1Class: string | null
  gs1ClassLabel: string | null
  packagingIndicator: number | null
  isbnRegistrationGroup: string | null
  normalizedGtin14: string | null
  normalizedGtin13: string | null
  checkDigit: number | null
}

interface ProductTeaserDto {
  gtin: string
  title: string | null
  coverImageUrl: string | null
  offersCount: number
  bestPrice: number | null
  currency: string | null
  productUrl: string | null
}

interface BarcodeCheckResponse {
  barcode: string
  forensics: BarcodeForensicsDto
  product: ProductTeaserDto | null
}

const { t } = useI18n()
const localePath = useLocalePath()
const { get, post } = useApiClient()
const { isAuthenticated } = useAuth()

const mode = ref<'sample' | 'live'>('sample')
const barcode = ref('')
const selectedApiKeyId = ref<string | null>(null)
const loading = ref(false)
const result = ref<BarcodeCheckResponse | null>(null)
const error = ref<string | null>(null)

async function run() {
  if (!barcode.value || barcode.value.trim().length === 0) return

  loading.value = true
  result.value = null
  error.value = null

  try {
    if (mode.value === 'sample' || !isAuthenticated.value) {
      // Public endpoint — no auth needed
      result.value = await get<BarcodeCheckResponse>('/api/v1/barcodes/check', { barcode: barcode.value.trim() })
    } else {
      // Live mode via playground proxy
      const playgroundResult = await post<{ response: { body: BarcodeCheckResponse } }>(
        '/api/v1/customer/playground/barcodes/check',
        {
          apiKeyId: selectedApiKeyId.value,
          barcode: barcode.value.trim()
        }
      )
      result.value = playgroundResult?.response?.body ?? null
    }
  } catch (err: unknown) {
    const e = err as { statusMessage?: string; message?: string }
    error.value = e.statusMessage ?? e.message ?? 'An unexpected error occurred.'
  } finally {
    loading.value = false
  }
}

function clear() {
  result.value = null
  error.value = null
}

function formatPrice(price: number, currency: string | null): string {
  const currencyCode = currency ?? 'EUR'
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode }).format(price)
  } catch {
    return `${price} ${currencyCode}`
  }
}
</script>
