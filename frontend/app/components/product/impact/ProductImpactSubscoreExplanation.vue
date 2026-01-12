<template>
  <section v-if="hasContent" class="impact-subscore-explanation">
    <div
      v-if="hasImportance"
      class="impact-subscore-explanation__card impact-subscore-explanation__card--importance"
    >
      <h5 class="impact-subscore-explanation__title">
        {{ t('product.impact.importanceTitle') }}
      </h5>
      <p class="impact-subscore-explanation__paragraph">
        {{ importanceDescription }}
      </p>
    </div>

    <div
      v-if="readIndicatorParagraphs.length || score.description"
      class="impact-subscore-explanation__card"
    >
      <h5 class="impact-subscore-explanation__title">
        {{ readIndicatorTitle }}
      </h5>
      <p
        v-for="(paragraph, index) in readIndicatorParagraphs"
        :key="`paragraph-${index}`"
        class="impact-subscore-explanation__paragraph"
      >
        {{ paragraph }}
      </p>
      <p
        v-if="score.description"
        class="impact-subscore-explanation__description"
      >
        {{ score.description }}
      </p>
    </div>

    <dl v-if="infoItems.length" class="impact-subscore-explanation__list">
      <div
        v-for="item in infoItems"
        :key="item.label"
        class="impact-subscore-explanation__row"
      >
        <dt class="impact-subscore-explanation__term">{{ item.label }}</dt>
        <dd class="impact-subscore-explanation__value">{{ item.value }}</dd>
      </div>
    </dl>

    <dl
      v-if="metadataItems.length"
      class="impact-subscore-explanation__list impact-subscore-explanation__list--metadata"
    >
      <div
        v-for="item in metadataItems"
        :key="item.label"
        class="impact-subscore-explanation__row"
      >
        <dt class="impact-subscore-explanation__term">{{ item.label }}</dt>
        <dd class="impact-subscore-explanation__value">{{ item.value }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  score: ScoreView
  absoluteValue: string | null
  productName: string
  productBrand: string
  productModel: string
  verticalTitle: string
  importanceDescription?: string | null
}>()

const { n, t, te, locale } = useI18n()

const normalizedScoreKey = computed(() => {
  const raw = props.score.id ?? ''
  const normalized = raw
    .toString()
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_')
    .replace(/[^a-z0-9_]+/g, '')
    .replace(/^_+|_+$/g, '')

  return normalized.length ? normalized : 'default'
})

const translationBaseKey = computed(
  () => `product.impact.subscores.${normalizedScoreKey.value}`
)
const translationFallbackBase = 'product.impact.subscores.default'

const betterIsLower = computed(() => props.score.betterIs === 'LOWER')

const resolveTranslation = (
  suffix: string,
  params: Record<string, unknown>
) => {
  const candidate = `${translationBaseKey.value}.${suffix}`
  if (te(candidate)) {
    return t(candidate, params)
  }

  return t(`${translationFallbackBase}.${suffix}`, params)
}

const readIndicatorOrientationKey = computed(() =>
  betterIsLower.value ? 'lower' : 'higher'
)

const resolveReadIndicatorTranslation = (
  suffix: string,
  params: Record<string, unknown>
) => {
  const orientedKey = `${translationBaseKey.value}.readIndicator.${readIndicatorOrientationKey.value}.${suffix}`
  if (te(orientedKey)) {
    return t(orientedKey, params)
  }

  const fallbackOrientedKey = `${translationFallbackBase}.readIndicator.${readIndicatorOrientationKey.value}.${suffix}`
  if (te(fallbackOrientedKey)) {
    return t(fallbackOrientedKey, params)
  }

  return resolveTranslation(`readIndicator.${suffix}`, params)
}

const normalizedVerticalTitle = computed(() => {
  const title = props.verticalTitle?.trim()
  if (!title?.length) {
    return ''
  }

  try {
    return title.toLocaleLowerCase(locale.value)
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to normalize vertical title.', error)
    }

    return title.toLowerCase()
  }
})

const productDisplayName = computed(() => {
  const brand = props.productBrand?.trim()
  const model = props.productModel?.trim()
  const segments = [brand, model].filter(segment => segment?.length)
  if (segments.length) {
    return segments.join(' ')
  }

  return props.productName
})

const absoluteStats = computed(() => props.score.absolute ?? null)

const importanceDescription = computed(() => {
  const scoreDescription =
    props.score.importanceDescription?.toString().trim() ?? ''
  const explicitDescription =
    props.importanceDescription?.toString().trim() ?? ''

  return explicitDescription || scoreDescription
})

const hasImportance = computed(() => importanceDescription.value.length > 0)

const scoreLabelLower = computed(() => {
  const label = props.score.label ?? ''
  if (!label.length) {
    return ''
  }

  try {
    return label.toLocaleLowerCase(locale.value)
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to normalize score label casing.', error)
    }

    return label.toLowerCase()
  }
})

const formatNumber = (
  value: number | null | undefined,
  options?: Intl.NumberFormatOptions
) => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return null
  }

  return n(value, {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
    ...options,
  })
}

const infoItems = computed(() => {
  const items: Array<{ label: string; value: string }> = []

  if (
    props.score.ranking != null &&
    Number.isFinite(Number(props.score.ranking))
  ) {
    items.push({
      label: t('product.impact.tableHeaders.ranking'),
      value: n(Number(props.score.ranking), {
        maximumFractionDigits: 0,
        minimumFractionDigits: 0,
      }),
    })
  }

  return items
})

const metadataItems = computed(() => {
  const entries = Object.entries(props.score.metadatas ?? {})
    .map(([key, value]) => ({ key, value }))
    .filter(
      entry => entry.value != null && String(entry.value).trim().length > 0
    )

  return entries.map(({ key, value }) => ({
    label: formatMetadataLabel(key),
    value: String(value),
  }))
})

const rankingValue = computed(() => {
  const ranking = Number(props.score.ranking)
  if (!Number.isFinite(ranking)) {
    return null
  }

  return n(ranking, { maximumFractionDigits: 0, minimumFractionDigits: 0 })
})

const populationValue = computed(() => {
  const count = absoluteStats.value?.count
  if (typeof count !== 'number' || Number.isNaN(count)) {
    return null
  }

  return {
    raw: count,
    formatted: n(count, { maximumFractionDigits: 0, minimumFractionDigits: 0 }),
  }
})

const worstRawValue = computed(() =>
  betterIsLower.value ? absoluteStats.value?.max : absoluteStats.value?.min
)
const bestRawValue = computed(() =>
  betterIsLower.value ? absoluteStats.value?.min : absoluteStats.value?.max
)

const worstValue = computed(() => formatNumber(worstRawValue.value))

const bestValue = computed(() => formatNumber(bestRawValue.value))
const averageValue = computed(() => formatNumber(absoluteStats.value?.avg))
const averageOn20Value = computed(() => {
  // Sigma scoring definition: Average is always the pivot at 10/20 (2.5/5)
  return formatNumber(10, { maximumFractionDigits: 0 })
})

const productAbsoluteValue = computed(() => {
  if (
    typeof absoluteStats.value?.value === 'number' &&
    Number.isFinite(absoluteStats.value?.value)
  ) {
    return formatNumber(absoluteStats.value?.value)
  }

  if (
    typeof props.score.value === 'number' &&
    Number.isFinite(props.score.value)
  ) {
    return formatNumber(props.score.value)
  }

  if (
    typeof props.score.absoluteValue === 'number' &&
    Number.isFinite(props.score.absoluteValue)
  ) {
    return formatNumber(props.score.absoluteValue)
  }

  return typeof props.absoluteValue === 'string' ? props.absoluteValue : null
})

const productOn20Value = computed(() => {
  if (typeof props.score.on20 !== 'number' || Number.isNaN(props.score.on20)) {
    return null
  }

  return formatNumber(props.score.on20, { maximumFractionDigits: 1 })
})

const readIndicatorParams = computed(() => ({
  scoreLabel: props.score.label,
  scoreLabelLower: scoreLabelLower.value,
  verticalTitle: normalizedVerticalTitle.value,
  worst: worstValue.value,
  best: bestValue.value,
  average: averageValue.value,
  averageOn20: averageOn20Value.value,
  productName: productDisplayName.value,
  productAbsolute: productAbsoluteValue.value,
  productOn20: productOn20Value.value,
  count: populationValue.value?.formatted,
  ranking: rankingValue.value,
}))

const readIndicatorTitle = computed(() =>
  resolveTranslation('readIndicator.title', readIndicatorParams.value)
)

const readIndicatorParagraphs = computed(() => {
  const paragraphs: string[] = []
  const params = readIndicatorParams.value

  if (worstValue.value) {
    paragraphs.push(resolveReadIndicatorTranslation('worst', params))
  }

  if (bestValue.value && populationValue.value) {
    paragraphs.push(resolveReadIndicatorTranslation('best', params))
  }

  if (averageValue.value && populationValue.value && averageOn20Value.value) {
    paragraphs.push(resolveReadIndicatorTranslation('average', params))
  }

  if (productAbsoluteValue.value && productOn20Value.value) {
    paragraphs.push(resolveReadIndicatorTranslation('product', params))
  }

  return paragraphs.filter(paragraph => paragraph?.toString().trim().length)
})

const hasContent = computed(
  () =>
    readIndicatorParagraphs.value.length > 0 ||
    Boolean(props.score.description) ||
    infoItems.value.length > 0 ||
    metadataItems.value.length > 0 ||
    hasImportance.value
)

function formatMetadataLabel(rawKey: string): string {
  const humanized = rawKey
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/\s+/g, ' ')
    .trim()

  return humanized
    .split(' ')
    .filter(Boolean)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
</script>

<style scoped>
.impact-subscore-explanation {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.impact-subscore-explanation__card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.impact-subscore-explanation__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore-explanation__paragraph {
  margin: 0;
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  line-height: 1.6;
}

.impact-subscore-explanation__description {
  margin: 0;
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-subscore-explanation__list {
  margin: 0;
  display: grid;
  gap: 0.5rem;
}

.impact-subscore-explanation__list--metadata {
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
  padding-top: 0.75rem;
}

.impact-subscore-explanation__row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.impact-subscore-explanation__term {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-subscore-explanation__value {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

@media (max-width: 640px) {
  .impact-subscore-explanation__row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
