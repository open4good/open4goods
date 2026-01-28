<template>
  <section v-if="hasContent" class="impact-subscore-explanation">
    <div
      v-if="hasImportance"
      class="impact-subscore-explanation__section impact-subscore-explanation__section--importance"
    >
      <h5 class="impact-subscore-explanation__title">
        {{ importanceTitle }}
      </h5>
      <p class="impact-subscore-explanation__paragraph">
        {{ importanceDescription }}
      </p>
    </div>

    <div
      v-if="hasReadIndicatorContent"
      class="impact-subscore-explanation__section"
    >
      <h5 class="impact-subscore-explanation__title">
        {{ readIndicatorTitle }}
      </h5>
      <ul
        v-if="readIndicatorHighlights.length"
        class="impact-subscore-explanation__bullet-list"
      >
        <li
          v-for="item in readIndicatorHighlights"
          :key="item.id"
          class="impact-subscore-explanation__bullet-item"
        >
          <v-icon
            icon="mdi-check-circle-outline"
            size="18"
            class="impact-subscore-explanation__bullet-icon"
          />
          <span>{{ item.text }}</span>
        </li>
      </ul>
      <p
        v-for="(paragraph, index) in readIndicatorDetails"
        :key="`detail-${index}`"
        class="impact-subscore-explanation__paragraph"
      >
        {{ paragraph }}
      </p>
    </div>

    <div
      v-if="statisticalMethodItems.length"
      class="impact-subscore-explanation__section"
    >
      <h5 class="impact-subscore-explanation__title">
        {{ statisticalMethodTitle }}
      </h5>
      <ul class="impact-subscore-explanation__bullet-list">
        <li
          v-for="(item, index) in statisticalMethodItems"
          :key="`method-${index}`"
          class="impact-subscore-explanation__bullet-item"
        >
          <v-icon
            icon="mdi-chart-bell-curve"
            size="18"
            class="impact-subscore-explanation__bullet-icon"
          />
          <span>{{ item }}</span>
        </li>
      </ul>
      <v-card
        v-if="statisticalMethodInfo"
        variant="tonal"
        class="impact-subscore-explanation__info-card"
      >
        <div class="impact-subscore-explanation__info-card-content">
          <p class="impact-subscore-explanation__info-title">
            {{ statisticalMethodInfo.title }}
          </p>
          <p class="impact-subscore-explanation__info-body">
            {{ statisticalMethodInfo.body }}
          </p>
        </div>
      </v-card>
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

    <div v-if="rankingBadgeText" class="impact-subscore-explanation__ranking">
      <v-chip size="x-small" color="primary" variant="tonal">
        {{ rankingBadgeText }}
      </v-chip>
    </div>
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

const impactBetterIsLower = computed(
  () => (props.score.impactBetterIs ?? null) === 'LOWER'
)
const userBetterIsLower = computed(
  () => (props.score.userBetterIs ?? null) === 'LOWER'
)

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
  impactBetterIsLower.value ? 'lower' : 'higher'
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

const importanceTitle = computed(() =>
  t('product.impact.importanceTitle', {
    scoreName: props.score.label ?? '',
  })
)

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
  impactBetterIsLower.value
    ? absoluteStats.value?.max
    : absoluteStats.value?.min
)
const bestRawValue = computed(() =>
  impactBetterIsLower.value
    ? absoluteStats.value?.min
    : absoluteStats.value?.max
)

const worstValue = computed(() => formatNumber(worstRawValue.value))

const bestValue = computed(() => formatNumber(bestRawValue.value))
const averageValue = computed(() => formatNumber(absoluteStats.value?.avg))

const normalizationMethod = computed(
  () => props.score.scoring?.normalization?.method ?? 'SIGMA'
)
const normalizationParams = computed(
  () => props.score.scoring?.normalization?.params ?? null
)
const scaleMin = computed(() => props.score.scoring?.scale?.min ?? 0)
const scaleMax = computed(() => props.score.scoring?.scale?.max ?? 5)

const averageOn20Value = computed(() => {
  if (normalizationMethod.value !== 'SIGMA') {
    return null
  }
  return formatNumber(10, { maximumFractionDigits: 0 })
})

const sigmaKValue = computed(() => normalizationParams.value?.sigmaK ?? 2)
const sigmaLowerBound = computed(() => {
  if (!absoluteStats.value?.avg || absoluteStats.value?.stdDev == null) {
    return null
  }
  return formatNumber(
    absoluteStats.value.avg - sigmaKValue.value * absoluteStats.value.stdDev
  )
})
const sigmaUpperBound = computed(() => {
  if (!absoluteStats.value?.avg || absoluteStats.value?.stdDev == null) {
    return null
  }
  return formatNumber(
    absoluteStats.value.avg + sigmaKValue.value * absoluteStats.value.stdDev
  )
})
const percentileValue = computed(() => {
  if (normalizationMethod.value !== 'PERCENTILE') {
    return null
  }
  if (typeof props.score.relativeValue !== 'number') {
    return null
  }
  return formatNumber((props.score.relativeValue / 5) * 100, {
    maximumFractionDigits: 0,
  })
})
const fixedMinValue = computed(() =>
  formatNumber(normalizationParams.value?.fixedMin ?? null)
)
const fixedMaxValue = computed(() =>
  formatNumber(normalizationParams.value?.fixedMax ?? null)
)
const quantileLowValue = computed(() =>
  formatNumber(normalizationParams.value?.quantileLow ?? null)
)
const quantileHighValue = computed(() =>
  formatNumber(normalizationParams.value?.quantileHigh ?? null)
)
const thresholdValue = computed(() =>
  formatNumber(normalizationParams.value?.threshold ?? null)
)
const constantValue = computed(() =>
  formatNumber(normalizationParams.value?.constantValue ?? null)
)
const mappingCount = computed(() => {
  const mapping = normalizationParams.value?.mapping
  if (!mapping) {
    return null
  }
  return Object.keys(mapping).length
})
const binaryUsesGreater = computed(
  () => normalizationParams.value?.greaterIsPass ?? true
)

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
  unit: props.score.unit ?? '',
  sigma: formatNumber(absoluteStats.value?.stdDev),
  sigmaK: formatNumber(sigmaKValue.value, { maximumFractionDigits: 1 }),
  sigmaLower: sigmaLowerBound.value,
  sigmaUpper: sigmaUpperBound.value,
  percentile: percentileValue.value,
  scaleMin: formatNumber(scaleMin.value),
  scaleMax: formatNumber(scaleMax.value),
  fixedMin: fixedMinValue.value,
  fixedMax: fixedMaxValue.value,
  quantileLow: quantileLowValue.value,
  quantileHigh: quantileHighValue.value,
  threshold: thresholdValue.value,
  constantValue: constantValue.value,
  mappingCount: mappingCount.value,
  userBetterIs: userBetterIsLower.value ? 'lower' : 'higher',
}))

const rankingBadgeText = computed(() => {
  if (!rankingValue.value) {
    return null
  }

  if (populationValue.value?.formatted) {
    return t('product.impact.rankingBadge', {
      ranking: rankingValue.value,
      count: populationValue.value.formatted,
    })
  }

  return t('product.impact.rankingBadgeSingle', {
    ranking: rankingValue.value,
  })
})

const readIndicatorTitle = computed(() =>
  resolveTranslation('readIndicator.title', readIndicatorParams.value)
)

const readIndicatorHighlights = computed(() => {
  const items: Array<{ id: string; text: string }> = []
  const params = readIndicatorParams.value

  if (worstValue.value) {
    items.push({
      id: 'worst',
      text: resolveReadIndicatorTranslation('worst', params),
    })
  }

  if (bestValue.value && populationValue.value) {
    items.push({
      id: 'best',
      text: resolveReadIndicatorTranslation('best', params),
    })
  }

  if (averageValue.value && populationValue.value) {
    items.push({
      id: 'average',
      text: resolveReadIndicatorTranslation('average', params),
    })
  }

  return items.filter(item => item.text?.toString().trim().length)
})

const readIndicatorDetails = computed(() => {
  const paragraphs: string[] = []
  const params = readIndicatorParams.value


  if (productAbsoluteValue.value && productOn20Value.value) {
    paragraphs.push(resolveReadIndicatorTranslation('product', params))
  }

  return paragraphs.filter(paragraph => paragraph?.toString().trim().length)
})

const resolveStatisticalMethodTranslation = (
  suffix: string,
  params: Record<string, unknown>
) => {
  const candidate = `${translationBaseKey.value}.statisticalMethod.${suffix}`
  if (te(candidate)) {
    return t(candidate, params)
  }

  return t(`${translationFallbackBase}.statisticalMethod.${suffix}`, params)
}

const statisticalMethodTitle = computed(() => {
  const methodKey = normalizationMethod.value?.toLowerCase() ?? 'sigma'
  return resolveStatisticalMethodTranslation(
    `titles.${methodKey}`,
    readIndicatorParams.value
  )
})

const resolveStatisticalMethodInfoTranslation = (
  suffix: string,
  params: Record<string, unknown>
) => {
  const candidate = `${translationBaseKey.value}.statisticalMethodInfo.${suffix}`
  if (te(candidate)) {
    return t(candidate, params)
  }

  return t(`${translationFallbackBase}.statisticalMethodInfo.${suffix}`, params)
}

const statisticalMethodInfo = computed(() => {
  const params = readIndicatorParams.value
  const methodKey = normalizationMethod.value?.toLowerCase() ?? 'sigma'
  const methodKeyCandidate = `${translationBaseKey.value}.statisticalMethodInfo.${methodKey}`
  const fallbackKey = `${translationFallbackBase}.statisticalMethodInfo.${methodKey}`

  if (!te(methodKeyCandidate) && !te(fallbackKey)) {
    return null
  }

  return {
    title: resolveStatisticalMethodInfoTranslation('title', params),
    body: resolveStatisticalMethodInfoTranslation(methodKey, params),
  }
})

const statisticalMethodItems = computed(() => {
  const items: string[] = []
  const params = readIndicatorParams.value
  const methodKey = normalizationMethod.value?.toUpperCase() ?? 'SIGMA'

  if (methodKey === 'SIGMA') {
    if (sigmaLowerBound.value && sigmaUpperBound.value) {
      items.push(resolveStatisticalMethodTranslation('sigma.bounds', params))
    }
    if (sigmaKValue.value != null) {
      items.push(resolveStatisticalMethodTranslation('sigma.scoring', params))
    }
    if (absoluteStats.value?.stdDev != null) {
      items.push(
        resolveStatisticalMethodTranslation('sigma.distribution', params)
      )
    }
  } else if (methodKey === 'PERCENTILE') {
    items.push(
      resolveStatisticalMethodTranslation('percentile.scoring', params)
    )
    if (percentileValue.value) {
      items.push(
        resolveStatisticalMethodTranslation('percentile.value', params)
      )
    }
  } else if (methodKey === 'MINMAX_OBSERVED') {
    if (worstValue.value && bestValue.value) {
      items.push(
        resolveStatisticalMethodTranslation('minmax_observed.scoring', params)
      )
    }
  } else if (methodKey === 'MINMAX_FIXED') {
    if (fixedMinValue.value && fixedMaxValue.value) {
      items.push(
        resolveStatisticalMethodTranslation('minmax_fixed.scoring', params)
      )
    }
  } else if (methodKey === 'MINMAX_QUANTILE') {
    if (quantileLowValue.value && quantileHighValue.value) {
      items.push(
        resolveStatisticalMethodTranslation('minmax_quantile.scoring', params)
      )
    }
  } else if (methodKey === 'FIXED_MAPPING') {
    items.push(
      resolveStatisticalMethodTranslation('fixed_mapping.scoring', params)
    )
  } else if (methodKey === 'BINARY') {
    const binaryKey = binaryUsesGreater.value
      ? 'binary.scoring_greater'
      : 'binary.scoring_lower'
    items.push(resolveStatisticalMethodTranslation(binaryKey, params))
  } else if (methodKey === 'CONSTANT') {
    if (constantValue.value) {
      items.push(
        resolveStatisticalMethodTranslation('constant.scoring', params)
      )
    }
  }

  return items.filter(item => item?.toString().trim().length)
})

const hasReadIndicatorContent = computed(
  () =>
    readIndicatorHighlights.value.length > 0 ||
    readIndicatorDetails.value.length > 0 ||
    Boolean(props.score.description)
)

const hasContent = computed(
  () =>
    hasReadIndicatorContent.value ||
    statisticalMethodItems.value.length > 0 ||
    infoItems.value.length > 0 ||
    metadataItems.value.length > 0 ||
    Boolean(rankingBadgeText.value) ||
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

.impact-subscore-explanation__section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.impact-subscore-explanation__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore-explanation__bullet-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.5rem;
}

.impact-subscore-explanation__bullet-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  line-height: 1.6;
}

.impact-subscore-explanation__bullet-icon {
  color: rgba(var(--v-theme-accent-supporting), 0.9);
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

.impact-subscore-explanation__info-card {
  padding: 0.75rem 1rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-050), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-subscore-explanation__info-card-content {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.impact-subscore-explanation__info-title {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore-explanation__info-body {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  line-height: 1.5;
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

.impact-subscore-explanation__ranking {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .impact-subscore-explanation__row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
