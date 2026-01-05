<template>
  <div class="search-suggest-field__wrapper" v-bind="filteredAttrs">
    <v-autocomplete
      v-model="selectedItem"
      :search="internalSearch"
      :items="suggestionItems"
      item-value="id"
      :label="label"
      :placeholder="placeholder"
      :aria-label="ariaLabel"
      :loading="loading"
      :menu-props="menuProps"
      :hide-no-data="!showEmptyState"
      :no-data-text="''"
      :elevation="getFieldElevation(isHovering)"
      menu-icon=""
      prepend-inner-icon="mdi-magnify"
      variant="solo"
      density="comfortable"
      clearable
      hide-details
      return-object
      :class="[
        'search-suggest-field',
        { 'search-suggest-field--active': isHovering || isFieldFocused },
      ]"
      @mouseenter="isHovering = true"
      @mouseleave="isHovering = false"
      @update:model-value="handleSelection"
      @click:clear="handleClear"
      @keydown.enter="handleEnterKey"
      @update:search="handleSearchInput"
      @blur="handleBlur"
      @focus="handleFocus"
    >
      <template #append-inner>
        <v-btn
          v-if="shouldShowVoiceButton && isHydrated"
          class="search-suggest-field__voice-button"
          density="comfortable"
          variant="text"
          icon
          :color="isVoiceListening ? 'primary' : undefined"
          :title="
            voiceError ||
            (isVoiceListening
              ? t('search.suggestions.voice.stopLabel')
              : t('search.suggestions.voice.startLabel'))
          "
          :aria-label="
            isVoiceListening
              ? t('search.suggestions.voice.stopLabel')
              : t('search.suggestions.voice.startLabel')
          "
          data-test="search-voice-button"
          :disabled="!isVoiceSupported"
          @click="toggleVoiceListening"
        >
          <v-icon
            :icon="
              isVoiceListening ? 'mdi-microphone' : 'mdi-microphone-outline'
            "
            size="20"
            aria-hidden="true"
          />
        </v-btn>
        <v-btn
          v-if="shouldShowScannerButton"
          class="search-suggest-field__scanner-button"
          density="comfortable"
          variant="text"
          icon
          :aria-label="t('search.suggestions.scanner.openLabel')"
          data-test="search-scanner-button"
          @click="openScannerDialog"
        >
          <v-icon icon="mdi-barcode-scan" size="20" aria-hidden="true" />
        </v-btn>
        <slot v-if="$slots['append-inner']" name="append-inner" />
      </template>
      <template #item="{ item, props: itemProps, index }">
        <div class="search-suggest-field__entry" :data-section="item.raw.type">
          <p
            v-if="item.raw.type === 'category' && index === 0"
            class="search-suggest-field__section"
          >
            {{ t('search.suggestions.sections.categories') }}
          </p>
          <template v-if="item.raw.type === 'category'">
            <v-list-item
              v-bind="itemProps"
              class="search-suggest-field__item search-suggest-field__item--category"
              :aria-label="
                t('search.suggestions.categoryAria', {
                  category: item.raw.title,
                })
              "
            >
              <template #prepend>
                <v-avatar
                  class="search-suggest-field__avatar"
                  rounded="lg"
                  size="52"
                >
                  <v-img :src="item.raw.image" :alt="''" cover />
                </v-avatar>
              </template>
              <v-list-item-title class="search-suggest-field__title">
                {{ item.raw.title }}
              </v-list-item-title>
              <template #append>
                <v-icon
                  icon="mdi-arrow-top-right"
                  size="small"
                  aria-hidden="true"
                />
              </template>
            </v-list-item>
          </template>

          <p
            v-if="item.raw.type === 'product' && index === firstProductIndex"
            class="search-suggest-field__section search-suggest-field__section--compact"
          >
            {{ t('search.suggestions.sections.products') }}
          </p>
          <template v-if="item.raw.type === 'product'">
            <v-list-item
              v-bind="itemProps"
              class="search-suggest-field__item search-suggest-field__item--product"
              :aria-label="
                t('search.suggestions.productAria', {
                  product: item.raw.title,
                })
              "
            >
              <template #prepend>
                <v-avatar
                  class="search-suggest-field__avatar"
                  rounded="lg"
                  size="44"
                >
                  <v-img :src="item.raw.image" :alt="''" cover />
                </v-avatar>
              </template>
              <v-list-item-title class="search-suggest-field__product-title">
                {{ item.raw.title }}
              </v-list-item-title>
              <template #append>
                <ImpactScore
                  v-if="item.raw.ecoscoreValue !== null"
                  class="search-suggest-field__impact"
                  :score="item.raw.ecoscoreValue"
                  size="small"
                />
              </template>
            </v-list-item>
          </template>
        </div>
      </template>

      <template v-if="showEmptyState" #no-data>
        <div class="search-suggest-field__empty">
          <p v-if="!hasMinimumLength" class="search-suggest-field__empty-text">
            {{ t('search.suggestions.minimum', { min: minChars }) }}
          </p>
          <p
            v-else-if="hasError"
            class="search-suggest-field__empty-text search-suggest-field__empty-text--error"
          >
            {{ t('search.suggestions.error') }}
          </p>
          <p v-else class="search-suggest-field__empty-text">
            {{ t('search.suggestions.empty') }}
          </p>
        </div>
      </template>
    </v-autocomplete>
    <v-dialog
      v-model="isScannerDialogOpen"
      fullscreen
      transition="dialog-bottom-transition"
      scrollable
      content-class="search-suggest-field__scanner-dialog"
    >
      <template v-if="isScannerDialogOpen">
        <v-card class="search-suggest-field__scanner-card">
          <div class="search-suggest-field__scanner-header">
            <p class="search-suggest-field__scanner-title">
              {{ t('search.suggestions.scanner.title') }}
            </p>
            <v-btn
              icon
              variant="text"
              class="search-suggest-field__scanner-close"
              :aria-label="t('search.suggestions.scanner.closeLabel')"
              @click="closeScannerDialog"
            >
              <v-icon icon="mdi-close" aria-hidden="true" />
            </v-btn>
          </div>
          <div class="search-suggest-field__scanner-body">
            <ClientOnly>
              <PwaBarcodeScanner
                v-if="isScannerComponentReady"
                :active="isScannerActive"
                class="search-suggest-field__scanner-stream"
                :loading-label="t('search.suggestions.scanner.loading')"
                :error-label="t('search.suggestions.scanner.error')"
                @decode="handleScannerDecode"
              />
            </ClientOnly>
            <p
              v-if="isScannerDialogOpen && !isScannerComponentReady"
              class="search-suggest-field__scanner-loading"
            >
              {{ t('search.suggestions.scanner.loading') }}
            </p>
            <p class="search-suggest-field__scanner-helper">
              {{ t('search.suggestions.scanner.helper') }}
            </p>
          </div>
        </v-card>
      </template>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import {
  computed,
  defineAsyncComponent,
  nextTick,
  onMounted,
  onBeforeUnmount,
  reactive,
  ref,
  useAttrs,
  watch,
} from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import type {
  SearchSuggestCategoryMatchDto,
  SearchSuggestProductMatchDto,
  SearchSuggestResponseDto,
} from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'

interface CategorySuggestionItem {
  type: 'category'
  id: string
  title: string
  image: string | null
  url: string | null
  verticalId: string | null
}

interface ProductSuggestionItem {
  type: 'product'
  id: string
  title: string
  image: string | null
  gtin: string | null
  verticalId: string | null
  ecoscoreValue: number | null
}

type SuggestionItem = CategorySuggestionItem | ProductSuggestionItem

export type { CategorySuggestionItem, ProductSuggestionItem }

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()

const filteredAttrs = computed(() => {
  return attrs
})

const props = withDefaults(
  defineProps<{
    modelValue: string
    label: string
    placeholder: string
    ariaLabel: string
    minChars?: number
    enableScan?: boolean
    enableSuggest?: boolean
    enableVoice?: boolean
    scanMobile?: boolean
    scanDesktop?: boolean
    suggestMobile?: boolean
    suggestDesktop?: boolean
    voiceMobile?: boolean
    voiceDesktop?: boolean
  }>(),
  {
    minChars: 2,
    enableScan: true,
    enableSuggest: true,
    enableVoice: true,
    scanMobile: true,
    scanDesktop: false,
    suggestMobile: true,
    suggestDesktop: true,
    voiceMobile: true,
    voiceDesktop: false,
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'clear' | 'submit'): void
  (event: 'select-category', value: CategorySuggestionItem): void
  (event: 'select-product', value: ProductSuggestionItem): void
}>()

const { t, locale } = useI18n()
const requestURL = useRequestURL()
const runtimeConfig = useRuntimeConfig()
const display = useDisplay()
const router = useRouter()
const isMobile = computed(() => display.smAndDown.value)

const internalSearch = ref(props.modelValue ?? '')
const lastCommittedValue = ref(internalSearch.value)
const selectedItem = ref<SuggestionItem | null>(null)
const loading = ref(false)
const hasError = ref(false)
const categories = ref<CategorySuggestionItem[]>([])
const products = ref<ProductSuggestionItem[]>([])
const currentRequest = ref(0)
const pendingSubmit = ref(false)
const isScannerDialogOpen = ref(false)
const isScannerActive = ref(false)
const isScannerComponentReady = ref(false)
const isFieldFocused = ref(false)
const isHovering = ref(false)
const isVoiceSupported = ref(false)
const isVoiceListening = ref(false)
const voiceError = ref<string | null>(null)
const speechRecognition = ref<SpeechRecognition | null>(null)
const isHydrated = ref(false)

onMounted(() => {
  isHydrated.value = true
})

const minChars = computed(() => Math.max(props.minChars ?? 2, 1))

const hasMinimumLength = computed(
  () => internalSearch.value.trim().length >= minChars.value
)

const isSuggestEnabled = computed(() => {
  const deviceAllowance = isMobile.value
    ? props.suggestMobile
    : props.suggestDesktop

  return props.enableSuggest && Boolean(deviceAllowance)
})

const showEmptyState = computed(
  () =>
    isSuggestEnabled.value &&
    !loading.value &&
    hasMinimumLength.value &&
    (hasError.value || (!categories.value.length && !products.value.length))
)

const firstProductIndex = computed(() => categories.value.length)

const menuProps = reactive({
  maxHeight: 420,
  offset: 4,
  elevation: 16,
})

const getFieldElevation = (isHovering: boolean) =>
  isHovering || isFieldFocused.value ? 2 : 3

const suggestionItems = computed<SuggestionItem[]>(() => [
  ...categories.value,
  ...products.value,
])

const shouldShowScannerButton = computed(
  () =>
    props.enableScan &&
    Boolean(isMobile.value ? props.scanMobile : props.scanDesktop)
)
const shouldShowVoiceButton = computed(
  () =>
    props.enableVoice &&
    Boolean(isMobile.value ? props.voiceMobile : props.voiceDesktop)
)

let cachedScannerComponent: Component | null = null
let scannerLoadPromise: Promise<Component> | null = null

const loadScannerComponent = async (): Promise<Component> => {
  if (cachedScannerComponent) {
    return cachedScannerComponent
  }

  if (!scannerLoadPromise) {
    scannerLoadPromise = import('~/components/pwa/PwaBarcodeScanner.vue')
      .then(module => {
        cachedScannerComponent = module.default
        isScannerComponentReady.value = true
        return cachedScannerComponent
      })
      .catch(error => {
        scannerLoadPromise = null
        throw error
      })
  }

  return scannerLoadPromise
}

const PwaBarcodeScanner = defineAsyncComponent(loadScannerComponent)

const openScannerDialog = async () => {
  if (isScannerDialogOpen.value) {
    return
  }

  isScannerDialogOpen.value = true

  try {
    await loadScannerComponent()
    if (isScannerDialogOpen.value) {
      isScannerActive.value = true
    }
  } catch (error) {
    if (import.meta.dev) {
      console.error('Failed to load barcode scanner component', error)
    }
  }
}

const closeScannerDialog = () => {
  isScannerActive.value = false
  isScannerDialogOpen.value = false
}

watch(isScannerDialogOpen, isOpen => {
  if (!isOpen) {
    isScannerActive.value = false
  }
})

const stopVoiceListening = () => {
  isVoiceListening.value = false
  speechRecognition.value?.stop()
}

const handleVoiceResult = (event: SpeechRecognitionEvent) => {
  const transcript = event.results?.[0]?.[0]?.transcript?.trim()

  if (!transcript) {
    return
  }

  lastCommittedValue.value = transcript
  internalSearch.value = transcript
  emit('update:modelValue', transcript)
}

const handleVoiceError = () => {
  voiceError.value = t('search.suggestions.voice.error')
  stopVoiceListening()
}

const initializeSpeechRecognition = () => {
  if (!import.meta.client) {
    return
  }

  const SpeechRecognitionConstructor =
    (
      globalThis as typeof globalThis & {
        webkitSpeechRecognition?: typeof SpeechRecognition
      }
    ).SpeechRecognition ||
    (
      globalThis as typeof globalThis & {
        webkitSpeechRecognition?: typeof SpeechRecognition
      }
    ).webkitSpeechRecognition

  if (!SpeechRecognitionConstructor) {
    voiceError.value = t('search.suggestions.voice.unsupported')
    return
  }

  const recognition = new SpeechRecognitionConstructor()
  recognition.continuous = false
  recognition.interimResults = false
  recognition.maxAlternatives = 1
  recognition.lang = locale.value
  recognition.onresult = event => {
    handleVoiceResult(event as unknown as SpeechRecognitionEvent)
    stopVoiceListening()
  }
  recognition.onerror = handleVoiceError
  recognition.onend = () => {
    isVoiceListening.value = false
  }

  speechRecognition.value = recognition
  isVoiceSupported.value = true
}

const startVoiceListening = () => {
  if (!speechRecognition.value) {
    voiceError.value = t('search.suggestions.voice.unsupported')
    return
  }

  voiceError.value = null
  speechRecognition.value.lang = locale.value
  speechRecognition.value.start()
  isVoiceListening.value = true
}

const toggleVoiceListening = () => {
  if (isVoiceListening.value) {
    stopVoiceListening()
    return
  }

  startVoiceListening()
}

watch(
  () => shouldShowVoiceButton.value,
  allowed => {
    if (allowed) {
      initializeSpeechRecognition()
    }
  },
  { immediate: true }
)

const staticServerBase = computed(() => {
  const configured = runtimeConfig.public?.staticServer

  if (configured) {
    try {
      return new URL(configured).toString()
    } catch {
      // Ignore invalid runtime configuration and fall back to the request origin
    }
  }

  return requestURL.origin
})

const toAbsoluteUrl = (value?: string | null): string | null => {
  if (!value) {
    return null
  }

  try {
    return new URL(value, staticServerBase.value).toString()
  } catch {
    return null
  }
}

const normalizeCategory = (
  match: SearchSuggestCategoryMatchDto,
  index: number
): CategorySuggestionItem | null => {
  const title =
    match.verticalHomeTitle?.trim() || t('search.suggestions.unknownCategory')
  const normalizedUrl = (() => {
    const raw = match.verticalHomeUrl?.trim()
    if (!raw) {
      return null
    }

    return raw.startsWith('/') ? raw : `/${raw}`
  })()

  return {
    type: 'category',
    id: `category-${match.verticalId ?? index}`,
    title,
    image: toAbsoluteUrl(match.imageSmall) ?? null,
    url: normalizedUrl,
    verticalId: match.verticalId ?? null,
  }
}

const unknownProductLabel = computed(() =>
  t('search.suggestions.unknownProduct')
)

const normalizeProduct = (
  match: SearchSuggestProductMatchDto,
  index: number
): ProductSuggestionItem | null => {
  const brand = match.brand?.trim()
  const model = match.model?.trim()

  const title =
    brand && model
      ? `${brand} – ${model}`
      : brand || model || unknownProductLabel.value

  const gtin = match.gtin?.trim() || null

  return {
    type: 'product',
    id: `product-${gtin ?? index}`,
    title,
    image: toAbsoluteUrl(match.coverImagePath) ?? null,
    gtin,
    verticalId: match.verticalId ?? null,
    ecoscoreValue: Number.isFinite(match.ecoscoreValue)
      ? Number(match.ecoscoreValue)
      : null,
  }
}

const resetSuggestions = () => {
  categories.value = []
  products.value = []
}

const loadSuggestions = async (query: string) => {
  if (!isSuggestEnabled.value) {
    return
  }

  const trimmed = query.trim()

  if (trimmed.length < minChars.value) {
    resetSuggestions()
    hasError.value = false
    return
  }

  const requestId = ++currentRequest.value
  loading.value = true
  hasError.value = false

  try {
    const response = await $fetch<SearchSuggestResponseDto>(
      '/api/search/suggest',
      {
        params: { query: trimmed },
      }
    )

    if (requestId !== currentRequest.value) {
      return
    }

    const normalizedCategories = (response?.categoryMatches ?? [])
      .map(normalizeCategory)
      .filter((item): item is CategorySuggestionItem => Boolean(item))

    const normalizedProducts = (response?.productMatches ?? [])
      .map(normalizeProduct)
      .filter((item): item is ProductSuggestionItem => Boolean(item))

    categories.value = normalizedCategories
    products.value = normalizedProducts
  } catch (error) {
    if (requestId !== currentRequest.value) {
      return
    }

    if (import.meta.dev) {
      console.error('Failed to load search suggestions', error)
    }

    hasError.value = true
    resetSuggestions()
  } finally {
    if (requestId === currentRequest.value) {
      loading.value = false
    }
  }
}

const debouncedLoad = useDebounceFn((value: string) => {
  if (!import.meta.client) {
    return
  }

  if (!isSuggestEnabled.value) {
    return
  }

  return loadSuggestions(value)
}, 300)

let suppressInternalWatch = false
const isBlurring = ref(false)
let blurResetTimeout: ReturnType<typeof setTimeout> | null = null

const setInternalSearchValue = (value: string) => {
  if (internalSearch.value === value) {
    return
  }

  suppressInternalWatch = true
  internalSearch.value = value
}

watch(
  internalSearch,
  value => {
    if (suppressInternalWatch) {
      suppressInternalWatch = false
      return
    }

    if (value !== props.modelValue) {
      emit('update:modelValue', value)
    }

    debouncedLoad(value)
  },
  { flush: 'sync' }
)

watch(
  () => props.modelValue,
  value => {
    const normalized = value ?? ''

    lastCommittedValue.value = normalized

    if (normalized === internalSearch.value) {
      return
    }

    setInternalSearchValue(normalized)
    debouncedLoad(normalized)
  },
  { flush: 'post' }
)

watch(
  isSuggestEnabled,
  enabled => {
    if (!enabled) {
      debouncedLoad.cancel?.()
      resetSuggestions()
      hasError.value = false
      return
    }

    debouncedLoad(internalSearch.value)
  },
  { immediate: true }
)

const handleBlur = () => {
  isBlurring.value = true
  isFieldFocused.value = false

  if (blurResetTimeout !== null) {
    clearTimeout(blurResetTimeout)
    blurResetTimeout = null
  }

  nextTick(() => {
    setInternalSearchValue(lastCommittedValue.value)
    blurResetTimeout = setTimeout(() => {
      isBlurring.value = false
      blurResetTimeout = null
    }, 0)
  })
}

const handleFocus = () => {
  if (blurResetTimeout !== null) {
    clearTimeout(blurResetTimeout)
    blurResetTimeout = null
  }

  isBlurring.value = false
  isFieldFocused.value = true
}

const handleSearchInput = (value: string) => {
  if (isBlurring.value) {
    return
  }

  // Prevent Vuetify from clearing the search input on mount when it's not focused
  if (!isFieldFocused.value && !value && props.modelValue) {
    return
  }

  internalSearch.value = value
}

let submitTimeout: ReturnType<typeof setTimeout> | null = null

const cancelPendingSubmit = () => {
  pendingSubmit.value = false

  if (submitTimeout !== null) {
    clearTimeout(submitTimeout)
    submitTimeout = null
  }
}

const handleEnterKey = (event: KeyboardEvent) => {
  if (event.isComposing) {
    return
  }

  pendingSubmit.value = true

  if (submitTimeout !== null) {
    clearTimeout(submitTimeout)
  }

  submitTimeout = setTimeout(() => {
    if (pendingSubmit.value) {
      emit('submit')
    }

    cancelPendingSubmit()
  }, 0)
}

onBeforeUnmount(() => {
  cancelPendingSubmit()

  if (blurResetTimeout !== null) {
    clearTimeout(blurResetTimeout)
    blurResetTimeout = null
  }

  stopVoiceListening()
})

const handleSelection = (item: SuggestionItem | null) => {
  if (!item) {
    return
  }

  cancelPendingSubmit()

  if (item.type === 'category') {
    emit('select-category', item)
  } else if (item.type === 'product') {
    emit('select-product', item)
  }

  selectedItem.value = null
}

const handleClear = () => {
  cancelPendingSubmit()
  internalSearch.value = ''
  resetSuggestions()
  hasError.value = false
  emit('clear')
}

const handleScannerDecode = (rawValue: string | null) => {
  const normalized = (rawValue ?? '').replace(/\s+/gu, '')

  if (!normalized) {
    return
  }

  closeScannerDialog()
  lastCommittedValue.value = normalized
  internalSearch.value = normalized

  router.push(`/${encodeURIComponent(normalized)}`)
}
</script>

<style scoped lang="sass">
.search-suggest-field__wrapper
  width: 100%

.search-suggest-field
  width: 100%

  :deep(.v-field)
    background-color: rgba(var(--v-theme-surface-glass-strong), 0.96)
    border-radius: 1rem
    cursor: text
    transition: transform 0.25s ease, background-color 0.25s ease

  :deep(.v-field__overlay)
    cursor: text

  :deep(.v-field__field)
    cursor: text

  :deep(.v-field__prepend-inner .v-icon)
    color: rgba(var(--v-theme-text-neutral-secondary), 0.5)

  :deep(input)
    color: rgb(var(--v-theme-text-neutral-strong))
    cursor: text

  :deep(.v-overlay__content)
    border-radius: 1rem
    overflow: hidden

  &--active :deep(.v-field)
    background-color: rgba(var(--v-theme-surface-default), 0.98)
    transform: translateY(-2px)
    box-shadow: 0 4px 16px rgba(var(--v-theme-shadow-primary-400), 0.12)

.search-suggest-field__voice-button,
.search-suggest-field__scanner-button
  min-width: auto
  margin-inline-start: 0.35rem
  color: rgb(var(--v-theme-primary))

  :deep(.v-btn__overlay)
    display: none

.search-suggest-field__scanner-dialog
  backdrop-filter: blur(4px)

.search-suggest-field__scanner-card
  height: 100%
  display: flex
  flex-direction: column
  background-color: rgba(var(--v-theme-surface-default), 0.98)

.search-suggest-field__scanner-header
  display: flex
  align-items: center
  justify-content: space-between
  padding: 0.75rem 1rem
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

.search-suggest-field__scanner-title
  margin: 0
  font-size: 1rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.search-suggest-field__scanner-body
  flex: 1
  display: flex
  flex-direction: column
  gap: 1rem
  padding: 1rem

.search-suggest-field__scanner-stream
  flex: 1
  border-radius: 1rem
  overflow: hidden
  background-color: rgb(7, 12, 24)
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.45)

.search-suggest-field__scanner-helper
  margin: 0
  text-align: center
  font-size: 0.9rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.search-suggest-field__scanner-loading
  margin: 0
  text-align: center
  font-size: 0.95rem
  font-weight: 600
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)

.search-suggest-field__entry
  display: flex
  flex-direction: column

.search-suggest-field__section
  margin: 0
  padding: 0.75rem 1rem 0.25rem
  font-size: 0.9rem
  font-weight: 600
  color: rgba(var(--v-theme-text-neutral-strong), 0.72)
  text-transform: uppercase
  letter-spacing: 0.08em

  &--compact
    padding-top: 0.5rem

.search-suggest-field__item
  border-radius: 0.9rem
  margin: 0 0.5rem
  transition: background-color 0.2s ease, transform 0.2s ease

  &:hover,
  &:focus-visible
    background-color: rgba(var(--v-theme-surface-primary-080), 0.85)
    transform: translateY(-1px)

  :deep(.v-list-item__overlay)
    display: none

.search-suggest-field__item--category
  padding-block: 0.9rem

.search-suggest-field__item--product
  padding-block: 0.6rem

.search-suggest-field__avatar
  background-color: rgba(var(--v-theme-surface-glass), 0.8)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

.search-suggest-field__title
  font-weight: 600
  font-size: 1.05rem
  color: rgb(var(--v-theme-text-neutral-strong))

.search-suggest-field__product-title
  display: flex
  flex-direction: column
  font-size: 0.95rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.search-suggest-field__impact
  margin-inline-start: 0.5rem

.search-suggest-field__empty
  padding: 1rem 1.25rem
  text-align: center
  color: rgb(var(--v-theme-text-neutral-secondary))

.search-suggest-field__empty-text
  margin: 0
  font-size: 0.9rem

  &--error
    color: rgb(var(--v-theme-error))
</style>
