<template>
  <section
    v-if="variant"
    class="impact-why"
    :aria-labelledby="titleId"
    data-testid="impact-why"
  >
    <h3 :id="titleId" class="impact-why__title">
      {{
        variant === 'notRated'
          ? t('components.impactScore.v2.notRated')
          : t('components.impactScore.why.title')
      }}
    </h3>

    <p v-if="headline" class="impact-why__headline">{{ headline }}</p>
    <p v-if="notRatedSentence" class="impact-why__headline">
      {{ notRatedSentence }}
    </p>

    <ul v-if="lines.length" class="impact-why__list">
      <li v-for="line in lines" :key="line.id">{{ line.text }}</li>
    </ul>

    <NuxtLink
      v-if="methodologyHref"
      :to="methodologyHref"
      class="impact-why__link"
    >
      {{ t('components.impactScore.v2.howCalculated') }}
    </NuxtLink>
  </section>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductImpactScoreStatusDto } from '~~/shared/api-client'
import type { ScoreView } from './impact-types'
import { resolveBetterThanPercent } from '~/utils/_impact-score-v2'
import {
  computeWhyHighlights,
  resolveWhyVariant,
} from '~/utils/_impact-score-why'
import type { WhyCriterion } from '~/utils/_impact-score-why'

const props = defineProps({
  status: {
    type: Object as PropType<ProductImpactScoreStatusDto | null>,
    default: null,
  },
  scores: {
    type: Array as PropType<ScoreView[]>,
    default: () => [],
  },
  /** Product score on the 0-20 scale. */
  score: {
    type: Number as PropType<number | null>,
    default: null,
  },
  /** Plural name of the category items, e.g. "téléviseurs". */
  itemsLabel: {
    type: String,
    default: '',
  },
  methodologyHref: {
    type: String,
    default: '',
  },
})

const { t, n, locale } = useI18n()
const titleId = `impact-why-${useId()}`

// A sentence is shown only when all its variables are present: it is dropped
// otherwise, never filled with an average or a placeholder.
const variant = computed(() =>
  props.status ? resolveWhyVariant(props.status) : null
)
const highlights = computed(() => computeWhyHighlights(props.scores))

const listFormat = (items: string[]): string =>
  new Intl.ListFormat(locale.value, {
    style: 'long',
    type: 'conjunction',
  }).format(items)

const describe = (criterion: WhyCriterion, withAverage: boolean): string => {
  if (!criterion.value) return criterion.label
  return withAverage && criterion.categoryAverage
    ? t('components.impactScore.why.criterionVsAverage', {
        label: criterion.label,
        value: criterion.value,
        average: criterion.categoryAverage,
      })
    : t('components.impactScore.why.criterion', {
        label: criterion.label,
        value: criterion.value,
      })
}

const describeAll = (criteria: WhyCriterion[]): string =>
  listFormat(
    criteria.map((criterion, index) => describe(criterion, index === 0))
  )

const formatScoredAt = (value: number | null | undefined): string => {
  if (typeof value !== 'number' || !Number.isFinite(value)) return ''
  return new Intl.DateTimeFormat(locale.value, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(value))
}

const headline = computed(() => {
  const status = props.status
  if (!status || !variant.value || variant.value === 'notRated') return ''
  if (typeof props.score !== 'number' || !Number.isFinite(props.score)) {
    return ''
  }
  const score = n(props.score, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })
  const percent = resolveBetterThanPercent(status)
  if (percent == null) {
    return t('components.impactScore.why.onlyRated', { score })
  }
  if (!props.itemsLabel) return ''
  return t(
    variant.value === 'bottom'
      ? 'components.impactScore.why.headlineBottom'
      : 'components.impactScore.why.headline',
    {
      score,
      percent,
      count: status.ratedCount ?? 0,
      items: props.itemsLabel,
    }
  )
})

const notRatedSentence = computed(() => {
  const status = props.status
  if (!status || variant.value !== 'notRated') return ''
  const reason = status.notRatedReason ?? 'NOT_SCORED'
  const sentence = t(`components.impactScore.why.notRated.${reason}`, {
    count: status.realCriteriaCount ?? 0,
    total: status.totalCriteriaCount ?? 0,
    version: status.currentMethodologyVersion ?? '',
  })
  const missing = highlights.value.missing
  return reason === 'LOW_COVERAGE' && missing.length
    ? `${sentence} ${t('components.impactScore.why.missingData', {
        criteria: listFormat(missing),
      })}`
    : sentence
})

const lines = computed(() => {
  const status = props.status
  const kind = variant.value
  if (!status || !kind) return []
  const { strengths, weaknesses, missing } = highlights.value
  const out: Array<{ id: string; text: string }> = []

  if (kind === 'notRated') {
    out.push({
      id: 'notRatedAfter',
      text: t('components.impactScore.why.notRatedNotLower'),
    })
    return out
  }

  const strengthKey =
    kind === 'top'
      ? 'strengthsTop'
      : kind === 'bottom'
        ? 'strengthsBottomLow'
        : 'strengths'
  const weaknessKey =
    kind === 'top'
      ? 'weaknessesTop'
      : kind === 'bottom'
        ? 'weaknessesBottom'
        : 'weaknesses'
  const strengthItems = kind === 'top' ? strengths : strengths.slice(0, 1)
  const weaknessItems = kind === 'top' ? weaknesses.slice(0, 1) : weaknesses

  const positive = strengthItems.length
    ? {
        id: 'strengths',
        text: t(`components.impactScore.why.${strengthKey}`, {
          criteria: describeAll(strengthItems),
        }),
      }
    : null
  const negative = weaknessItems.length
    ? {
        id: 'weaknesses',
        text: t(`components.impactScore.why.${weaknessKey}`, {
          criteria: describeAll(weaknessItems),
        }),
      }
    : null
  // The bottom variant leads with what weighs on the score.
  out.push(
    ...(kind === 'bottom' ? [negative, positive] : [positive, negative]).filter(
      (line): line is { id: string; text: string } => line !== null
    )
  )

  if (missing.length) {
    out.push({
      id: 'missing',
      text: t('components.impactScore.why.missing', {
        criteria: listFormat(missing),
      }),
    })
  }

  const real = status.realCriteriaCount
  const total = status.totalCriteriaCount
  if (typeof real === 'number' && typeof total === 'number') {
    const date = formatScoredAt(status.scoredAt)
    const version = status.methodologyVersion
    out.push({
      id: 'basis',
      text:
        real === total && date && version != null
          ? t('components.impactScore.why.basisComplete', { date, version })
          : date && version != null
            ? t('components.impactScore.why.basis', {
                count: real,
                total,
                date,
                version,
              })
            : t('components.impactScore.why.basisShort', {
                count: real,
                total,
              }),
    })
  }

  if (kind === 'top' || kind === 'bottom') {
    out.push({
      id: 'caveat',
      text: t(`components.impactScore.why.caveat.${kind}`),
    })
  }
  return out
})
</script>

<style scoped>
.impact-why {
  padding: 16px 20px;
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.12);
  background: rgba(var(--v-theme-on-surface), 0.03);
}

.impact-why__title {
  margin: 0 0 8px;
  font-size: 1.1rem;
  font-weight: 700;
}

.impact-why__headline {
  margin: 0 0 8px;
  font-size: 0.95rem;
}

.impact-why__list {
  margin: 0 0 12px;
  padding-left: 1.25rem;
  font-size: 0.9rem;
}

.impact-why__link {
  font-size: 0.85rem;
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
}
</style>
