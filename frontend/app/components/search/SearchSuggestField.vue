<template>
  <v-autocomplete
    v-model="selectedItem"
    v-model:search="internalSearch"
    :items="suggestionItems"
    :label="label"
    :placeholder="placeholder"
    :aria-label="ariaLabel"
    :loading="loading"
    :menu-props="menuProps"
    :hide-no-data="!showEmptyState"
    :no-data-text="''"
    menu-icon=""
    prepend-inner-icon="mdi-magnify"
    variant="solo"
    density="comfortable"
    clearable
    hide-details
    return-object
    class="search-suggest-field"
    @update:model-value="handleSelection"
    @click:clear="handleClear"
    @keydown.enter="handleEnterKey"
  >
    <template v-if="$slots['append-inner']" #append-inner>
      <slot name="append-inner" />
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
            :aria-label="t('search.suggestions.categoryAria', { category: item.raw.title })"
          >
            <template #prepend>
              <v-avatar class="search-suggest-field__avatar" rounded="lg" size="52">
                <v-img :src="item.raw.image" :alt="''" cover />
              </v-avatar>
            </template>
            <v-list-item-title class="search-suggest-field__title">
              {{ item.raw.title }}
            </v-list-item-title>
            <template #append>
              <v-icon icon="mdi-arrow-top-right" size="small" aria-hidden="true" />
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
            :aria-label="t('search.suggestions.productAria', { product: item.raw.title })"
          >
            <template #prepend>
              <v-avatar class="search-suggest-field__avatar" rounded="lg" size="44">
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
        <p v-else-if="hasError" class="search-suggest-field__empty-text search-suggest-field__empty-text--error">
          {{ t('search.suggestions.error') }}
        </p>
        <p v-else class="search-suggest-field__empty-text">
          {{ t('search.suggestions.empty') }}
        </p>
      </div>
    </template>
  </v-autocomplete>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
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

const props = withDefaults(
  defineProps<{
    modelValue: string
    label: string
    placeholder: string
    ariaLabel: string
    minChars?: number
  }>(),
  {
    minChars: 2,
  },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'clear' | 'submit'): void
  (event: 'select-category', value: CategorySuggestionItem): void
  (event: 'select-product', value: ProductSuggestionItem): void
}>()

const { t } = useI18n()
const requestURL = useRequestURL()
const runtimeConfig = useRuntimeConfig()

const internalSearch = ref(props.modelValue ?? '')
const selectedItem = ref<SuggestionItem | null>(null)
const loading = ref(false)
const hasError = ref(false)
const categories = ref<CategorySuggestionItem[]>([])
const products = ref<ProductSuggestionItem[]>([])
const currentRequest = ref(0)
const pendingSubmit = ref(false)

const minChars = computed(() => Math.max(props.minChars ?? 2, 1))

const hasMinimumLength = computed(
  () => internalSearch.value.trim().length >= minChars.value,
)

const showEmptyState = computed(
  () =>
    !loading.value &&
    hasMinimumLength.value &&
    (hasError.value || (!categories.value.length && !products.value.length)),
)

const firstProductIndex = computed(() => categories.value.length)

const menuProps = reactive({
  maxHeight: 420,
  offset: 4,
})

const suggestionItems = computed<SuggestionItem[]>(() => [
  ...categories.value,
  ...products.value,
])

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
  index: number,
): CategorySuggestionItem | null => {
  const title = match.verticalHomeTitle?.trim() || t('search.suggestions.unknownCategory')
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

const unknownProductLabel = computed(() => t('search.suggestions.unknownProduct'))

const normalizeProduct = (
  match: SearchSuggestProductMatchDto,
  index: number,
): ProductSuggestionItem | null => {
  const brand = match.brand?.trim()
  const model = match.model?.trim()

  const title = brand && model
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
      },
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

  return loadSuggestions(value)
}, 300)

watch(
  () => props.modelValue,
  (value) => {
    const normalized = value ?? ''

    if (normalized !== internalSearch.value) {
      internalSearch.value = normalized
    }

    debouncedLoad(normalized)
  },
  { flush: 'post' },
)

watch(
  internalSearch,
  (value) => {
    if (value !== props.modelValue) {
      emit('update:modelValue', value)
    }
  },
  { flush: 'sync' },
)

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
</script>

<style scoped lang="sass">
.search-suggest-field
  width: 100%

  :deep(.v-field)
    background-color: rgba(var(--v-theme-hero-overlay-soft), 0.94)
    border-radius: 1rem
    box-shadow: 0 16px 30px -20px rgba(15, 23, 42, 0.55)

  :deep(.v-field__prepend-inner .v-icon)
    color: rgba(var(--v-theme-text-on-accent), 0.7)

  :deep(input)
    color: rgb(var(--v-theme-text-on-accent))

  :deep(.v-overlay__content)
    border-radius: 1rem
    box-shadow: 0 20px 40px -24px rgba(15, 23, 42, 0.55)
    overflow: hidden

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
