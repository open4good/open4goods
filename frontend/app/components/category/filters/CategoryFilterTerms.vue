<template>
  <div class="category-filter-terms">
    <div class="category-filter-terms__header">
      <p class="category-filter-terms__title">{{ displayTitle }}</p>
      <v-text-field
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

    <div class="category-filter-terms__options" role="listbox">
      <v-checkbox
        v-for="option in filteredOptions"
        :key="option.key"
        :label="formatOptionLabel(option.key)"
        :model-value="localTerms.includes(option.key ?? '')"
        density="compact"
        hide-details
        color="primary"
        class="category-filter-terms__checkbox"
        @update:model-value="onCheckboxChange(option.key, $event)"
      >
        <template #append>
          <span class="category-filter-terms__count">{{ option.count ?? 0 }}</span>
        </template>
      </v-checkbox>

      <p v-if="!filteredOptions.length" class="category-filter-terms__empty">
        {{ $t('category.filters.noMatch') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AggregationResponseDto, FieldMetadataDto, Filter } from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()

const displayTitle = computed(() => resolveFilterFieldTitle(props.field, t))

const search = ref('')
const localTerms = ref<string[]>(props.modelValue?.terms ?? [])

watch(
  () => props.modelValue,
  (next) => {
    localTerms.value = next?.operator === 'term' ? [...(next.terms ?? [])] : []
  },
  { immediate: true },
)

const options = computed(() => props.aggregation?.buckets ?? [])

const filteredOptions = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) {
    return options.value
  }

  return options.value.filter((option) => option.key?.toLowerCase().includes(query))
})

const onCheckboxChange = (term: string | undefined, selected: boolean | null) => {
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
      : null,
  )
}

const formatOptionLabel = (key?: string) => key ?? t('category.filters.missingLabel')
</script>

<style scoped lang="sass">
.category-filter-terms
  display: flex
  flex-direction: column
  gap: 0.75rem

  &__header
    display: flex
    flex-direction: column
    gap: 0.5rem

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

  &__count
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    margin-inline-start: 0.5rem

  &__empty
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    margin: 0.5rem 0
</style>
