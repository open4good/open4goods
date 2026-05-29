<template>
  <!--
    Competition (offersCount) filter rendered as three lightweight bands.
    When the aggregation does not provide a coherent spread, we transparently
    fall back to the generic numeric range filter.
  -->
  <CategoryFilterNumeric
    v-if="!isCoherent"
    :field="field"
    :aggregation="aggregation"
    :baseline-aggregation="baselineAggregation"
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
  />

  <div v-else class="category-filter-competition">
    <p class="category-filter-competition__title">
      {{ t('category.filters.competition.title') }}
    </p>

    <div class="category-filter-competition__bands" role="group">
      <v-chip
        v-for="band in bands"
        :key="band.key"
        :color="band.key === activeBand ? band.color : undefined"
        :variant="band.key === activeBand ? 'flat' : 'tonal'"
        size="small"
        label
        class="category-filter-competition__band"
        :aria-pressed="band.key === activeBand"
        @click="onBandClick(band)"
      >
        <v-icon :icon="band.icon" start size="14" />
        {{ band.label }}
        <span class="category-filter-competition__count">{{ band.count }}</span>
      </v-chip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import CategoryFilterNumeric from './CategoryFilterNumeric.vue'

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  baselineAggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()

type BandKey = 'low' | 'medium' | 'high'

interface BandDefinition {
  key: BandKey
  min: number
  max?: number
  color: string
  icon: string
}

const BAND_DEFINITIONS: BandDefinition[] = [
  {
    key: 'low',
    min: 1,
    max: 2,
    color: 'warning',
    icon: 'mdi-signal-cellular-1',
  },
  {
    key: 'medium',
    min: 3,
    max: 4,
    color: 'info',
    icon: 'mdi-signal-cellular-2',
  },
  { key: 'high', min: 5, color: 'success', icon: 'mdi-signal-cellular-3' },
]

const bucketCounts = computed(() => {
  const source = props.aggregation?.buckets?.length
    ? props.aggregation.buckets
    : (props.baselineAggregation?.buckets ?? [])

  const counts: Record<BandKey, number> = { low: 0, medium: 0, high: 0 }

  for (const bucket of source) {
    const value = Number(bucket.key)
    if (!Number.isFinite(value)) {
      continue
    }
    const count = bucket.count ?? 0
    if (value >= 5) {
      counts.high += count
    } else if (value >= 3) {
      counts.medium += count
    } else if (value >= 1) {
      counts.low += count
    }
  }

  return counts
})

const bands = computed(() =>
  BAND_DEFINITIONS.map(band => ({
    ...band,
    label: t(`category.filters.competition.${band.key}`),
    count: bucketCounts.value[band.key],
  }))
)

// The band UI is only meaningful when at least two bands hold products.
const isCoherent = computed(() => {
  const populated = Object.values(bucketCounts.value).filter(count => count > 0)
  return populated.length >= 2
})

const activeBand = computed<BandKey | null>(() => {
  const filter = props.modelValue
  if (!filter || filter.operator !== 'range') {
    return null
  }

  const match = BAND_DEFINITIONS.find(
    band =>
      band.min === filter.min &&
      (band.max ?? undefined) === (filter.max ?? undefined)
  )
  return match?.key ?? null
})

const onBandClick = (band: BandDefinition) => {
  if (!props.field.mapping) {
    return
  }

  // Toggle off when clicking the already-active band.
  if (band.key === activeBand.value) {
    emit('update:modelValue', null)
    return
  }

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min: band.min,
    max: band.max,
  })
}
</script>

<style scoped lang="sass">
.category-filter-competition
  display: flex
  flex-direction: column
  gap: 0.75rem

  &__title
    margin: 0
    font-size: 1rem
    font-weight: 600

  &__bands
    display: flex
    flex-wrap: wrap
    gap: 0.5rem

  &__band
    transition: transform 0.15s ease, background-color 0.15s ease
    cursor: pointer

    &:hover
      transform: translateY(-1px)

  &__count
    margin-inline-start: 0.4rem
    font-size: 0.7rem
    font-weight: 600
    opacity: 0.8
</style>
