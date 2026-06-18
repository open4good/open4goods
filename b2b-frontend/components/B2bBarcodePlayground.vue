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
            <!-- Symbology -->
            <v-select
              v-model="symbology"
              :label="t('barcodes.playground.controls.symbology')"
              :items="symbologyOptions"
              variant="outlined"
              density="comfortable"
              class="mb-3"
            />

            <!-- Data/Payload -->
            <v-text-field
              v-model="payload"
              :label="t('barcodes.playground.controls.payload')"
              :placeholder="t('barcodes.playground.controls.payloadPlaceholder')"
              variant="outlined"
              density="comfortable"
              prepend-inner-icon="mdi-barcode"
              class="mb-3"
              clearable
            />

            <!-- Output Format -->
            <div class="mb-4">
              <div class="text-caption text-medium-emphasis mb-1">{{ t('barcodes.playground.controls.format') }}</div>
              <v-btn-toggle
                v-model="format"
                color="primary"
                variant="outlined"
                mandatory
                density="comfortable"
                class="w-100"
              >
                <v-btn value="png" class="flex-grow-1">PNG</v-btn>
                <v-btn value="svg" class="flex-grow-1">SVG</v-btn>
              </v-btn-toggle>
            </div>

            <!-- Advanced Panel -->
            <v-expansion-panels variant="accordion" class="mb-4">
              <v-expansion-panel
                :title="t('barcodes.playground.controls.dimensions')"
                elevation="0"
                class="border rounded-lg mb-2"
              >
                <v-expansion-panel-text>
                  <v-row dense>
                    <v-col cols="6">
                      <v-text-field
                        v-model.number="width"
                        :label="t('barcodes.playground.controls.width')"
                        type="number"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                    <v-col cols="6">
                      <v-text-field
                        v-model.number="height"
                        :label="t('barcodes.playground.controls.height')"
                        type="number"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                    <v-col cols="12">
                      <v-select
                        v-model.number="dpi"
                        :label="t('barcodes.playground.controls.dpi')"
                        :items="[150, 300, 600]"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                  </v-row>
                </v-expansion-panel-text>
              </v-expansion-panel>

              <v-expansion-panel
                :title="t('barcodes.playground.controls.colors')"
                elevation="0"
                class="border rounded-lg mb-2"
              >
                <v-expansion-panel-text>
                  <v-row dense>
                    <v-col cols="6">
                      <v-text-field
                        v-model="foreground"
                        :label="t('barcodes.playground.controls.foreground')"
                        variant="outlined"
                        density="compact"
                        type="color"
                      />
                    </v-col>
                    <v-col cols="6">
                      <v-text-field
                        v-model="background"
                        :label="t('barcodes.playground.controls.background')"
                        variant="outlined"
                        density="compact"
                        type="color"
                      />
                    </v-col>
                  </v-row>
                </v-expansion-panel-text>
              </v-expansion-panel>

              <v-expansion-panel
                :title="t('barcodes.playground.controls.options')"
                elevation="0"
                class="border rounded-lg mb-2"
              >
                <v-expansion-panel-text>
                  <v-checkbox
                    v-model="showText"
                    :label="t('barcodes.playground.controls.showText')"
                    density="compact"
                    hide-details
                    class="mb-2"
                  />
                  <v-checkbox
                    v-model="quietZone"
                    :label="t('barcodes.playground.controls.quietZone')"
                    density="compact"
                    hide-details
                    class="mb-3"
                  />
                  <v-row dense>
                    <v-col cols="6">
                      <v-text-field
                        v-model="moduleWidthMm"
                        :label="t('barcodes.playground.controls.moduleWidth')"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                    <v-col cols="6">
                      <v-text-field
                        v-model="barHeightMm"
                        :label="t('barcodes.playground.controls.barHeight')"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                    <v-col cols="6">
                      <v-text-field
                        v-model="fontSize"
                        :label="t('barcodes.playground.controls.fontSize')"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                    <v-col cols="6">
                      <v-select
                        v-model="rotation"
                        :label="t('barcodes.playground.controls.rotation')"
                        :items="[0, 90, 180, 270]"
                        variant="outlined"
                        density="compact"
                      />
                    </v-col>
                  </v-row>
                </v-expansion-panel-text>
              </v-expansion-panel>

              <v-expansion-panel
                :title="t('barcodes.playground.controls.metadata')"
                elevation="0"
                class="border rounded-lg"
              >
                <v-expansion-panel-text>
                  <v-text-field
                    v-model="copyright"
                    :label="t('barcodes.playground.controls.copyright')"
                    :placeholder="t('barcodes.playground.controls.copyrightPlaceholder')"
                    variant="outlined"
                    density="compact"
                    class="mb-2"
                  />
                  <v-text-field
                    v-model="author"
                    :label="t('barcodes.playground.controls.author')"
                    :placeholder="t('barcodes.playground.controls.authorPlaceholder')"
                    variant="outlined"
                    density="compact"
                    class="mb-2"
                  />
                  <v-text-field
                    v-model="description"
                    :label="t('barcodes.playground.controls.description')"
                    :placeholder="t('barcodes.playground.controls.descriptionPlaceholder')"
                    variant="outlined"
                    density="compact"
                  />
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>

            <!-- API key for live mode -->
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
        <v-card variant="outlined" class="d-flex flex-column h-100">
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
          <v-card-text class="pa-4 flex-grow-1">
            <!-- Empty state -->
            <div v-if="!result && !runError && !running" class="text-center py-10 text-medium-emphasis">
              <v-icon icon="mdi-flask-outline" size="48" class="mb-3 d-block mx-auto" />
              <div>{{ t('barcodes.playground.preview.empty') }}</div>
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
              <!-- Visual Preview Panel -->
              <v-card variant="tonal" class="mb-4 text-center overflow-hidden position-relative animate-fade-in">
                <v-card-title class="text-subtitle-2 pa-3 border-bottom d-flex align-center justify-center">
                  <v-icon icon="mdi-eye-outline" size="16" class="mr-2" />
                  {{ t('barcodes.playground.preview.title') }}
                </v-card-title>

                <div class="pa-8 d-flex justify-center align-center min-height-200 bg-white">
                  <!-- Rendered barcode wrapper -->
                  <!-- eslint-disable vue/no-v-html -->
                  <div
                    v-if="format === 'svg'"
                    class="barcode-svg-render-wrapper"
                    v-html="barcodeImageSource"
                  />
                  <!-- eslint-enable vue/no-v-html -->
                  <img
                    v-else
                    :src="barcodeImageSource"
                    alt="Generated Barcode"
                    class="barcode-img-render"
                    :style="{ maxWidth: '100%', maxHeight: '250px' }"
                  >
                </div>

                <v-divider />

                <v-card-actions class="pa-3 bg-background-light d-flex justify-space-between flex-wrap ga-2">
                  <v-chip size="small" color="primary" variant="flat" prepend-icon="mdi-clock-outline">
                    {{ t('barcodes.playground.preview.expiresIn') }}
                  </v-chip>
                  <div class="d-flex ga-2">
                    <v-btn
                      size="small"
                      variant="outlined"
                      prepend-icon="mdi-content-copy"
                      @click="copyUrl"
                    >
                      {{ copyButtonText }}
                    </v-btn>
                    <v-btn
                      size="small"
                      color="primary"
                      variant="flat"
                      prepend-icon="mdi-download"
                      @click="downloadAsset"
                    >
                      {{ t('barcodes.playground.preview.download') }}
                    </v-btn>
                  </div>
                </v-card-actions>
              </v-card>

              <!-- Warnings if any -->
              <v-alert
                v-if="result.warnings && result.warnings.length"
                type="warning"
                variant="tonal"
                class="mb-4"
              >
                <div class="font-weight-medium mb-1">{{ t('barcodes.playground.preview.warnings') }}</div>
                <ul class="pl-4">
                  <li v-for="(warning, idx) in result.warnings" :key="idx" class="text-caption">
                    {{ warning }}
                  </li>
                </ul>
              </v-alert>

              <!-- Metering strip -->
              <v-row dense class="mb-4">
                <v-col cols="6" md="3">
                  <div class="text-caption text-medium-emphasis">{{ t('playground.meta.reason') }}</div>
                  <div class="font-weight-medium text-body-2">{{ result.meta?.reason ?? 'success' }}</div>
                </v-col>
                <v-col cols="6" md="3">
                  <div class="text-caption text-medium-emphasis">{{ t('playground.meta.creditsConsumed') }}</div>
                  <div class="font-weight-medium text-body-2">{{ result.meta?.creditsConsumed ?? 1 }}</div>
                </v-col>
                <v-col cols="6" md="3">
                  <div class="text-caption text-medium-emphasis">{{ t('playground.meta.creditsRemaining') }}</div>
                  <div class="font-weight-medium text-body-2">{{ result.meta?.creditsRemaining ?? '—' }}</div>
                </v-col>
                <v-col cols="6" md="3">
                  <div class="text-caption text-medium-emphasis">{{ t('barcodes.playground.preview.inputHash') }}</div>
                  <div class="font-weight-medium text-body-2 text-truncate" style="max-width: 100px;">
                    {{ result.inputHash ? result.inputHash.substring(0, 8) : '—' }}
                  </div>
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
                :key="ex.label"
                cols="12"
                sm="6"
                md="4"
              >
                <v-card
                  variant="tonal"
                  class="cursor-pointer hover-card"
                  @click="applyExample(ex)"
                >
                  <v-card-text class="pa-3">
                    <div class="font-weight-medium text-body-2 mb-1">{{ ex.label }}</div>
                    <code class="text-caption text-medium-emphasis">{{ ex.payload }}</code>
                    <div class="text-caption mt-1">
                      <v-chip size="x-small" color="primary" variant="tonal">
                        {{ ex.symbology.toUpperCase() }}
                      </v-chip>
                      <v-chip size="x-small" color="success" variant="tonal" class="ml-1">
                        1 {{ t('playground.credits') }}
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface BarcodeResponse {
  meta: {
    requestId: string
    billable: boolean
    creditsConsumed: number
    creditsRemaining: number
    reason: string
  }
  assetUrl: string
  expiresAt: string
  dimensions: {
    width: number
    height: number
    dpi: number
  }
  contentType: string
  warnings: string[]
  inputHash: string
}

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { session } = useAuthSession()
const { post, get } = useApiClient()

const isAuthenticated = computed(() => !!session.value?.user)

const mode = ref<'sample' | 'live'>('sample')
const symbology = ref('ean13')
const payload = ref('4006381333931')
const format = ref<'png' | 'svg'>('png')

// Dimensions
const width = ref(200)
const height = ref(100)
const dpi = ref(300)

// Colors
const foreground = ref('#000000')
const background = ref('#ffffff')

// Extra settings
const showText = ref(true)
const quietZone = ref(true)
const moduleWidthMm = ref('0.33')
const barHeightMm = ref('15.0')
const fontSize = ref('8.0')
const rotation = ref(0)

// Metadata settings
const copyright = ref('')
const author = ref('')
const description = ref('')

const selectedKeyId = ref<string | null>(null)
const running = ref(false)
const result = ref<BarcodeResponse | null>(null)
const runError = ref<{ title?: string, detail?: string } | null>(null)

// Copy button text state
const urlCopied = ref(false)
const copyButtonText = computed(() => {
  return urlCopied.value
    ? t('barcodes.playground.preview.urlCopied')
    : t('barcodes.playground.preview.copyUrl')
})

const symbologyOptions = [
  { title: 'EAN-13', value: 'ean13' },
  { title: 'EAN-8', value: 'ean8' },
  { title: 'Code 128', value: 'code128' },
  { title: 'GS1-128', value: 'gs1-128' },
  { title: 'ITF-14', value: 'itf14' },
  { title: 'UPC-A', value: 'upca' },
  { title: 'UPC-E', value: 'upce' },
  { title: 'QR Code', value: 'qr' },
  { title: 'Aztec', value: 'aztec' },
  { title: 'Data Matrix', value: 'datamatrix' },
  { title: 'PDF417', value: 'pdf417' }
]

const examples = [
  {
    label: 'Standard EAN-13 Product Barcode',
    symbology: 'ean13',
    payload: '4006381333931',
    format: 'png',
    width: 200,
    height: 100,
    copyright: 'Copyright 2026 Open4Goods',
    description: 'EAN-13 Barcode'
  },
  {
    label: 'High-Density Code 128 Asset Tag',
    symbology: 'code128',
    payload: 'NUDGER-12345',
    format: 'png',
    width: 250,
    height: 80,
    copyright: 'Internal Inventory System',
    description: 'Asset Code 128'
  },
  {
    label: 'Dynamic Website Link QR Code',
    symbology: 'qr',
    payload: 'https://open4goods.org',
    format: 'svg',
    width: 150,
    height: 150,
    copyright: 'Open4Goods',
    description: 'URL QR Code'
  }
]

function applyExample (ex: typeof examples[0]) {
  symbology.value = ex.symbology
  payload.value = ex.payload
  format.value = ex.format as 'png' | 'svg'
  width.value = ex.width
  height.value = ex.height
  copyright.value = ex.copyright
  description.value = ex.description
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
      .filter(k => k.status === 'ACTIVE')
      .map(k => ({ label: `${k.name} (${k.keyPrefix}…)`, value: k.id }))
    if (keyOptions.value[0]) selectedKeyId.value = keyOptions.value[0].value
  } catch {
    // ignore
  } finally {
    keysLoading.value = false
  }
}, { immediate: true })

const canRun = computed(() => {
  if (!payload.value?.trim()) return false
  if (mode.value === 'live' && !selectedKeyId.value) return false
  return true
})

const requestPreview = computed(() => {
  const keyStr = mode.value === 'sample' ? 'pdapi_sample_key' : 'pdapi_YOUR_KEY'
  const body = {
    type: symbology.value,
    data: payload.value?.trim(),
    format: format.value,
    width: width.value,
    height: height.value,
    foreground: foreground.value,
    background: background.value,
    rotation: rotation.value,
    showText: showText.value,
    quietZone: quietZone.value,
    options: {
      dpi: dpi.value,
      moduleWidthMm: moduleWidthMm.value ? parseFloat(moduleWidthMm.value) : null,
      barHeightMm: barHeightMm.value ? parseFloat(barHeightMm.value) : null,
      fontSize: fontSize.value ? parseFloat(fontSize.value) : null,
      preset: 'print-safe'
    },
    metadata: {
      copyright: copyright.value || null,
      author: author.value || null,
      description: description.value || null
    }
  }
  return `POST /api/v1/barcodes/render\nAuthorization: Bearer ${keyStr}\nContent-Type: application/json\n\n${JSON.stringify(body, null, 2)}`
})

// Dynamic image sources based on responses
const barcodeImageSource = computed(() => {
  if (!result.value) return ''
  if (mode.value === 'sample') {
    // Return custom mock preview based on inputs
    if (format.value === 'svg') {
      return generateSampleSvg()
    } else {
      return generateSamplePngDataUrl()
    }
  }
  return result.value.assetUrl
})

async function run () {
  if (!canRun.value) return
  running.value = true
  result.value = null
  runError.value = null

  const renderRequestPayload = {
    type: symbology.value,
    data: payload.value.trim(),
    format: format.value,
    width: width.value,
    height: height.value,
    foreground: foreground.value,
    background: background.value,
    rotation: rotation.value,
    showText: showText.value,
    quietZone: quietZone.value,
    options: {
      dpi: dpi.value,
      moduleWidthMm: moduleWidthMm.value ? parseFloat(moduleWidthMm.value) : null,
      barHeightMm: barHeightMm.value ? parseFloat(barHeightMm.value) : null,
      fontSize: fontSize.value ? parseFloat(fontSize.value) : null,
      preset: 'print-safe'
    },
    metadata: {
      copyright: copyright.value || null,
      author: author.value || null,
      description: description.value || null
    }
  }

  try {
    if (mode.value === 'sample') {
      await new Promise(r => setTimeout(r, 400))
      // Mock validation logic
      if (symbology.value === 'ean13' && payload.value.trim().length !== 13) {
        runError.value = {
          title: 'Invalid EAN-13 Barcode',
          detail: 'EAN-13 data must be exactly 13 digits.'
        }
        return
      }

      result.value = {
        meta: {
          requestId: 'req_sample_barcode_01',
          billable: true,
          creditsConsumed: 1,
          creditsRemaining: 2494,
          reason: 'success'
        },
        assetUrl: 'https://api.product-data-api.com/api/v1/barcodes/assets/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample',
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        dimensions: {
          width: width.value,
          height: height.value,
          dpi: dpi.value
        },
        contentType: format.value === 'svg' ? 'image/svg+xml' : 'image/png',
        warnings: [],
        inputHash: 'hash_' + Math.random().toString(36).substring(2, 10)
      }
    } else {
      result.value = await post<BarcodeResponse>('/api/v1/customer/playground/barcodes/render', {
        apiKeyId: selectedKeyId.value,
        request: renderRequestPayload
      })
    }
  } catch (err) {
    const e = err as { problem?: { title?: string }, details?: string }
    runError.value = { title: e.problem?.title ?? 'Error', detail: e.details ?? String(err) }
  } finally {
    running.value = false
  }
}

// Client-side SVG barcode generator for premium interactive sample preview
function generateSampleSvg () {
  const bg = background.value || '#ffffff'
  const fg = foreground.value || '#000000'
  const w = width.value
  const h = height.value
  const data = payload.value || '123456789'

  if (symbology.value === 'qr') {
    // Render QR matrix
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 100 100" style="background-color: ${bg}; fill: ${fg};">
      <!-- Outer QR Finders -->
      <rect x="5" y="5" width="25" height="25" fill="${fg}" />
      <rect x="8" y="8" width="19" height="19" fill="${bg}" />
      <rect x="11" y="11" width="13" height="13" fill="${fg}" />

      <rect x="70" y="5" width="25" height="25" fill="${fg}" />
      <rect x="73" y="8" width="19" height="19" fill="${bg}" />
      <rect x="76" y="11" width="13" height="13" fill="${fg}" />

      <rect x="5" y="70" width="25" height="25" fill="${fg}" />
      <rect x="8" y="73" width="19" height="19" fill="${bg}" />
      <rect x="11" y="76" width="13" height="13" fill="${fg}" />

      <!-- Small alignment pattern -->
      <rect x="73" y="73" width="9" height="9" fill="${fg}" />
      <rect x="75" y="75" width="5" height="5" fill="${bg}" />
      <rect x="77" y="77" width="1" height="1" fill="${fg}" />

      <!-- Random simulated data pixels -->
      <rect x="35" y="5" width="5" height="5" />
      <rect x="45" y="5" width="5" height="5" />
      <rect x="55" y="5" width="5" height="5" />
      <rect x="35" y="15" width="5" height="5" />
      <rect x="40" y="20" width="5" height="5" />
      <rect x="50" y="25" width="5" height="5" />
      <rect x="60" y="20" width="5" height="5" />

      <rect x="5" y="35" width="5" height="5" />
      <rect x="15" y="35" width="5" height="5" />
      <rect x="25" y="35" width="5" height="5" />
      <rect x="10" y="45" width="5" height="5" />
      <rect x="20" y="50" width="5" height="5" />
      <rect x="5" y="60" width="5" height="5" />
      <rect x="15" y="55" width="5" height="5" />

      <rect x="35" y="35" width="10" height="10" />
      <rect x="50" y="35" width="5" height="5" />
      <rect x="60" y="35" width="10" height="10" />
      <rect x="75" y="35" width="5" height="5" />
      <rect x="85" y="35" width="5" height="5" />

      <rect x="35" y="50" width="5" height="5" />
      <rect x="45" y="45" width="5" height="5" />
      <rect x="55" y="55" width="10" height="5" />
      <rect x="70" y="50" width="5" height="5" />
      <rect x="80" y="50" width="5" height="5" />

      <rect x="35" y="65" width="5" height="5" />
      <rect x="40" y="70" width="5" height="5" />
      <rect x="50" y="60" width="5" height="5" />
      <rect x="55" y="70" width="5" height="5" />
      <rect x="60" y="65" width="5" height="5" />

      <rect x="35" y="80" width="5" height="5" />
      <rect x="45" y="85" width="5" height="5" />
      <rect x="55" y="80" width="5" height="5" />
      <rect x="65" y="85" width="5" height="5" />

      <!-- Text metadata comment in XML -->
      <desc>Copyright: ${copyright.value || 'None'}</desc>
    </svg>`
  } else {
    // Render 1D barcode lines
    let barsHtml = ''
    let x = quietZone.value ? 20 : 5
    const barWidths = [1, 2, 1, 3, 1, 1, 2, 4, 1, 2, 3, 1, 1, 2, 1, 3, 1, 1, 2, 2, 1, 1, 3, 2, 1, 4, 1, 2, 1, 3, 1, 2, 1, 1, 2, 4, 1]

    for (let i = 0; i < barWidths.length; i++) {
      const wVal = barWidths[i]!
      if (i % 2 === 0) {
        barsHtml += `<rect x="${x}" y="10" width="${wVal}" height="60" fill="${fg}" />`
      }
      x += wVal
    }

    const textHtml = showText.value
      ? `<text x="50%" y="85" font-family="monospace" font-size="12" fill="${fg}" text-anchor="middle">${data}</text>`
      : ''

    return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 100 100" style="background-color: ${bg};">
      ${barsHtml}
      ${textHtml}
      <desc>Copyright: ${copyright.value || 'None'}</desc>
    </svg>`
  }
}

function generateSamplePngDataUrl () {
  // We can create a simple base64 encoded PNG representation or paint on a dynamic canvas.
  // In a browser environment, let's use an HTML5 canvas to generate a real dataUrl!
  if (typeof window === 'undefined') return ''

  const canvas = document.createElement('canvas')
  canvas.width = width.value
  canvas.height = height.value
  const ctx = canvas.getContext('2d')

  if (ctx) {
    ctx.fillStyle = background.value || '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    const fg = foreground.value || '#000000'
    ctx.fillStyle = fg

    if (symbology.value === 'qr') {
      // Draw simulated QR patterns
      const size = Math.min(canvas.width, canvas.height) * 0.8
      const offsetX = (canvas.width - size) / 2
      const offsetY = (canvas.height - size) / 2

      // Draw finders
      const finderSize = size * 0.25
      ctx.fillRect(offsetX, offsetY, finderSize, finderSize)
      ctx.fillStyle = background.value || '#ffffff'
      ctx.fillRect(offsetX + 3, offsetY + 3, finderSize - 6, finderSize - 6)
      ctx.fillStyle = fg
      ctx.fillRect(offsetX + 6, offsetY + 6, finderSize - 12, finderSize - 12)

      ctx.fillRect(offsetX + size - finderSize, offsetY, finderSize, finderSize)
      ctx.fillStyle = background.value || '#ffffff'
      ctx.fillRect(offsetX + size - finderSize + 3, offsetY + 3, finderSize - 6, finderSize - 6)
      ctx.fillStyle = fg
      ctx.fillRect(offsetX + size - finderSize + 6, offsetY + 6, finderSize - 12, finderSize - 12)

      ctx.fillRect(offsetX, offsetY + size - finderSize, finderSize, finderSize)
      ctx.fillStyle = background.value || '#ffffff'
      ctx.fillRect(offsetX + 3, offsetY + size - finderSize + 3, finderSize - 6, finderSize - 6)
      ctx.fillStyle = fg
      ctx.fillRect(offsetX + 6, offsetY + size - finderSize + 6, finderSize - 12, finderSize - 12)

      // Random blocks
      ctx.fillStyle = fg
      for (let x = finderSize + 2; x < size - finderSize; x += 6) {
        for (let y = 0; y < size; y += 6) {
          if (Math.random() > 0.4) {
            ctx.fillRect(offsetX + x, offsetY + y, 4, 4)
          }
        }
      }
    } else {
      // Draw 1D barcode lines
      const left = quietZone.value ? canvas.width * 0.1 : 5
      const right = quietZone.value ? canvas.width * 0.9 : canvas.width - 5
      const barHeight = showText.value ? canvas.height * 0.65 : canvas.height * 0.8

      let currX = left
      let toggle = true
      while (currX < right) {
        const thickness = Math.floor(Math.random() * 4) + 1
        if (toggle) {
          ctx.fillRect(currX, 10, thickness, barHeight)
        }
        currX += thickness + Math.floor(Math.random() * 3) + 1
        toggle = !toggle
      }

      if (showText.value) {
        ctx.fillStyle = fg
        ctx.font = `${fontSize.value || '10'}px monospace`
        ctx.textAlign = 'center'
        ctx.fillText(payload.value || '1234567890', canvas.width / 2, canvas.height - 10)
      }
    }
  }

  return canvas.toDataURL('image/png')
}

// Utilities for Copying and Downloading
async function copyUrl () {
  if (!result.value) return
  try {
    await navigator.clipboard.writeText(result.value.assetUrl)
    urlCopied.value = true
    setTimeout(() => {
      urlCopied.value = false
    }, 2000)
  } catch {
    // fallback
  }
}

function downloadAsset () {
  if (!result.value) return
  const url = barcodeImageSource.value
  const filename = `${symbology.value}_render.${format.value}`

  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

watch(locale, () => {
  if (locale.value === 'fr') {
    copyright.value = copyright.value || 'Copyright 2026 open4goods'
    author.value = author.value || 'API Code-barres Open4Goods'
    description.value = description.value || 'Code-barres Produit'
  } else {
    copyright.value = copyright.value || 'Copyright 2026 open4goods'
    author.value = author.value || 'Open4Goods B2B API'
    description.value = description.value || 'Product GTIN Barcode'
  }
}, { immediate: true })
</script>

<style scoped>
.min-height-200 {
  min-height: 220px;
}
.border-bottom {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
.barcode-svg-render-wrapper {
  max-width: 100%;
  max-height: 250px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.hover-card:hover {
  transform: translateY(-2px);
  transition: all 0.2s ease-in-out;
  border-color: rgb(var(--v-theme-primary)) !important;
}
.bg-background-light {
  background-color: rgba(0, 0, 0, 0.02);
}
.animate-fade-in {
  animation: fadeIn 0.35s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
