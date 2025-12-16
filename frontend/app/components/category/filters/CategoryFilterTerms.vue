<template>
  <div class="category-filter-terms">
    <div class="category-filter-terms__header">
      <p class="category-filter-terms__title">{{ displayTitle }}</p>
      <v-text-field
        v-if="showSearch"
        v-model="search"
        :label="$t('category.filters.searchOptions')"
        density="compact"
        variant="outlined"
        prepend-inner-icon="mdi-magnify"
        clearable
        hide-details
        class="category-filter-terms__search"
      />
    </div>

    <div
      ref="optionsContainer"
      class="category-filter-terms__options"
      role="listbox"
    >
      <v-checkbox
        v-for="option in filteredOptions"
        :key="option.key ?? '__missing__'"
        :label="formatOptionLabel(option.key)"
        :model-value="localTerms.includes(option.key ?? '')"
        density="compact"
        hide-details
        color="primary"
        :class="[
          'category-filter-terms__checkbox',
          {
            'category-filter-terms__checkbox--zero':
              option.count === 0 && !localTerms.includes(option.key ?? ''),
          },
        ]"
        @update:model-value="onCheckboxChange(option.key, $event)"
      >
        <template #append>
          <span class="category-filter-terms__count">{{ option.count }}</span>
        </template>
      </v-checkbox>

      <p v-if="!filteredOptions.length" class="category-filter-terms__empty">
        {{ $t('category.filters.noMatch') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useResizeObserver } from '@vueuse/core'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  baselineAggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()

const displayTitle = computed(() => resolveFilterFieldTitle(props.field, t))

const search = ref('')
const localTerms = ref<string[]>(props.modelValue?.terms ?? [])
const optionsContainer = ref<HTMLElement | null>(null)
const hasScrollableOptions = ref(false)

watch(
  () => props.modelValue,
  next => {
    localTerms.value = next?.operator === 'term' ? [...(next.terms ?? [])] : []
  },
  { immediate: true }
)

const MISSING_TERM_KEY = '__missing__'

const toTermValue = (value: unknown): string | undefined => {
  if (value == null) {
    return undefined
  }

  return String(value)
}

const normalizeTermKey = (key?: string | null) => key ?? MISSING_TERM_KEY

type TermOption = {
  key?: string
  count: number
}

const currentBuckets = computed(() => props.aggregation?.buckets ?? [])
const baselineBuckets = computed(() => props.baselineAggregation?.buckets ?? [])

const currentCounts = computed(() => {
  const map = new Map<string, number>()

  currentBuckets.value.forEach(bucket => {
    const key = normalizeTermKey(toTermValue(bucket.key))
    map.set(key, bucket.count ?? 0)
  })

  return map
})

const mergedOptions = computed<TermOption[]>(() => {
  const seen = new Set<string>()
  const options: TermOption[] = []

  const upsertOption = (key: string | undefined, count: number) => {
    const normalized = normalizeTermKey(key)

    if (seen.has(normalized)) {
      const index = options.findIndex(
        option => normalizeTermKey(option.key) === normalized
      )
      if (index !== -1) {
        options[index] = { key, count }
      }
      return
    }

    seen.add(normalized)
    options.push({ key, count })
  }

  baselineBuckets.value.forEach(bucket => {
    const key = toTermValue(bucket.key)
    const normalized = normalizeTermKey(key)
    const currentCount = currentCounts.value.get(normalized) ?? 0
    upsertOption(key, currentCount)
  })

  currentBuckets.value.forEach(bucket => {
    const key = toTermValue(bucket.key)
    const normalized = normalizeTermKey(key)
    const count = currentCounts.value.get(normalized) ?? bucket.count ?? 0
    upsertOption(key, count)
  })

  const selectedTerms = new Set(localTerms.value)
  selectedTerms.forEach(term => {
    const normalized = normalizeTermKey(term)
    if (!seen.has(normalized)) {
      seen.add(normalized)
      const count = currentCounts.value.get(normalized) ?? 0
      options.push({ key: term, count })
    }
  })

  return options
})

const filteredOptions = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) {
    return mergedOptions.value
  }

  return mergedOptions.value.filter(option =>
    option.key?.toLowerCase().includes(query)
  )
})

const updateScrollState = () => {
  const element = optionsContainer.value
  if (!element) {
    hasScrollableOptions.value = false
    return
  }

  hasScrollableOptions.value = element.scrollHeight - element.clientHeight > 1
}

const { stop: stopResizeObserver } = useResizeObserver(optionsContainer, () => {
  updateScrollState()
})

watch(
  () => [
    mergedOptions.value.length,
    filteredOptions.value.length,
    search.value,
  ],
  () => {
    nextTick(() => updateScrollState())
  },
  { immediate: true }
)

onMounted(() => {
  nextTick(() => updateScrollState())
})

onBeforeUnmount(() => {
  stopResizeObserver()
})

const showSearch = computed(
  () => hasScrollableOptions.value || search.value.trim().length > 0
)

const onCheckboxChange = (
  term: string | undefined,
  selected: boolean | null
) => {
  if (!term) {
    return
  }

  const next = new Set(localTerms.value)

  if (!selected) {
    next.delete(term)
  } else {
    next.add(term)
  }

  localTerms.value = Array.from(next)

  emit(
    'update:modelValue',
    localTerms.value.length
      ? {
          field: props.field.mapping,
          operator: 'term',
          terms: [...localTerms.value],
        }
      : null
  )
}

const formatOptionLabel = (key?: string) => {
  if (!key) {
    return t('category.filters.missingLabel')
  }

  const mapping = props.field.mapping
  if (mapping) {
    const translationKey = `category.filters.options.${mapping}.${key}`
    const translated = t(translationKey)

    if (translated && translated !== translationKey) {
      return translated
    }
  }

  return key
}
</script>

<style scoped lang="sass">
.category-filter-terms
  display: flex
  flex-direction: column
  gap: 0.75rem
  height: 100%

  &__header
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__search
    width: 100%
    min-width: 0

  &__title
    margin: 0
    font-size: 1rem
    font-weight: 600

  &__options
    max-height: 320px
    overflow-y: auto
    padding-right: 0.5rem
    display: flex
    flex-direction: column
    gap: 0.25rem

  &__checkbox--zero
    opacity: 0.6

  &__checkbox--zero.v-selection-control--dirty
    opacity: 1

  &__count
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    margin-inline-start: 0.5rem

  &__empty
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    margin: 0.5rem 0
</style>
