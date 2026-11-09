<template>
  <div class="impact-method" data-test="impact-method">
    <section
      class="impact-method__block"
      data-test="method-meaning"
      :aria-labelledby="`${idPrefix}-meaning`"
    >
      <h3 :id="`${idPrefix}-meaning`" class="impact-method__title">
        {{ t('category.ecoscorePage.method.meaning.title') }}
      </h3>
      <p class="impact-method__text" data-test="what-it-means">
        {{
          audit?.whatItMeansForUsers ||
          t('category.ecoscorePage.method.meaning.fallback', {
            category: categoryLabel,
          })
        }}
      </p>
      <h4 class="impact-method__subtitle">
        {{ t('category.ecoscorePage.method.limits.title') }}
      </h4>
      <v-alert
        v-if="audit?.notAnAbsoluteFootprint"
        class="impact-method__alert"
        type="warning"
        variant="tonal"
        density="comfortable"
        data-test="not-absolute"
      >
        {{ t('aiAudit.positioning.notAbsoluteWarning') }}
      </v-alert>
      <p v-if="audit?.whatIsMissingVsFullLca" class="impact-method__text">
        {{ audit.whatIsMissingVsFullLca }}
      </p>
      <p v-if="audit?.whyRelativeIsStillUseful" class="impact-method__text">
        {{ audit.whyRelativeIsStillUseful }}
      </p>
      <ul v-if="audit?.mitigations?.length" class="impact-method__list">
        <li v-for="item in audit.mitigations" :key="item">{{ item }}</li>
      </ul>
      <p class="impact-method__text">
        {{ t('category.ecoscorePage.method.limits.static') }}
      </p>
    </section>

    <section
      class="impact-method__block"
      data-test="method-criteria"
      :aria-labelledby="`${idPrefix}-method`"
    >
      <h3 :id="`${idPrefix}-method`" class="impact-method__title">
        {{ t('category.ecoscorePage.method.method.title') }}
      </h3>
      <p class="impact-method__text" data-test="method-version">
        {{
          t('category.ecoscorePage.method.method.version', {
            version: config?.methodologyVersion ?? '—',
            date: lastScoredLabel,
          })
        }}
      </p>
      <p v-if="minCoveragePercent !== null" class="impact-method__text">
        {{
          t('category.ecoscorePage.method.method.threshold', {
            percent: minCoveragePercent,
          })
        }}
      </p>
      <p class="impact-method__text">
        {{ t('category.ecoscorePage.method.method.redistribution') }}
      </p>
      <div class="impact-method__table-wrap">
        <table class="impact-method__table" data-test="criteria-table">
          <caption class="impact-method__sr">
            {{
              t('category.ecoscorePage.method.method.tableCaption')
            }}
          </caption>
          <thead>
            <tr>
              <th scope="col">
                {{ t('category.ecoscorePage.method.method.criterion') }}
              </th>
              <th scope="col">
                {{ t('category.ecoscorePage.method.method.weight') }}
              </th>
              <th scope="col">
                {{ t('category.ecoscorePage.method.method.direction') }}
              </th>
              <th scope="col">
                {{ t('category.ecoscorePage.method.method.why') }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in criteriaRows" :key="row.key">
              <th scope="row">{{ row.label }}</th>
              <td>{{ row.weightLabel }}</td>
              <td>{{ row.directionLabel }}</td>
              <td>{{ row.rationale }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section
      class="impact-method__block"
      data-test="method-products"
      :aria-labelledby="`${idPrefix}-products`"
    >
      <h3 :id="`${idPrefix}-products`" class="impact-method__title">
        {{ t('category.ecoscorePage.method.products.title') }}
      </h3>
      <p class="impact-method__text">
        {{
          t('category.ecoscorePage.method.products.scope', {
            category: categoryLabel,
          })
        }}
      </p>
      <template v-if="stats">
        <ul class="impact-method__list" data-test="counts">
          <li>
            {{
              t('category.ecoscorePage.method.products.rated', {
                count: fmt(stats.ratedCount),
              })
            }}
          </li>
          <li>
            {{
              t('category.ecoscorePage.method.products.notRated', {
                count: fmt(notRatedCount),
                lowCoverage: fmt(stats.notRatedLowCoverageCount),
                noCriteria: fmt(stats.notRatedNoCriteriaCount),
              })
            }}
          </li>
          <li v-if="stats.outdatedCount">
            {{
              t('category.ecoscorePage.method.products.outdated', {
                count: fmt(stats.outdatedCount),
              })
            }}
          </li>
          <li v-if="stats.excludedCount" data-test="excluded">
            {{
              t('category.ecoscorePage.method.products.excluded', {
                count: fmt(stats.excludedCount),
              })
            }}
          </li>
        </ul>

        <h4 class="impact-method__subtitle">
          {{ t('category.ecoscorePage.method.coverage.title') }}
        </h4>
        <p class="impact-method__text">
          {{ t('category.ecoscorePage.method.coverage.helper') }}
        </p>
        <ul
          v-if="coverageRows.length"
          class="impact-method__bars"
          data-test="coverage"
        >
          <li v-for="row in coverageRows" :key="row.key">
            <span class="impact-method__bar-label">{{ row.label }}</span>
            <span class="impact-method__bar" role="img" :aria-label="row.aria">
              <span
                class="impact-method__bar-fill"
                :style="{ width: `${row.percent}%` }"
              />
            </span>
            <span class="impact-method__bar-value">{{ row.percent }}%</span>
          </li>
        </ul>
        <p v-else class="impact-method__text">
          {{ t('category.ecoscorePage.method.coverage.unavailable') }}
        </p>

        <h4 class="impact-method__subtitle">
          {{ t('category.ecoscorePage.method.distribution.title') }}
        </h4>
        <ul
          v-if="histogramRows.length"
          class="impact-method__bars"
          data-test="distribution"
        >
          <li v-for="row in histogramRows" :key="row.label">
            <span class="impact-method__bar-label">{{ row.label }}</span>
            <span class="impact-method__bar" role="img" :aria-label="row.aria">
              <span
                class="impact-method__bar-fill impact-method__bar-fill--alt"
                :style="{ width: `${row.percent}%` }"
              />
            </span>
            <span class="impact-method__bar-value">{{ row.count }}</span>
          </li>
        </ul>
        <p v-else class="impact-method__text">
          {{ t('category.ecoscorePage.method.distribution.unavailable') }}
        </p>
        <p v-if="stats.scoreMedian != null" class="impact-method__text">
          {{
            t('category.ecoscorePage.method.distribution.summary', {
              min: stats.scoreMin,
              median: stats.scoreMedian,
              max: stats.scoreMax,
            })
          }}
        </p>
      </template>
      <p v-else class="impact-method__text" data-test="stats-unavailable">
        {{ t('category.ecoscorePage.method.products.unavailable') }}
      </p>
    </section>

    <section
      class="impact-method__block"
      data-test="method-sources"
      :aria-labelledby="`${idPrefix}-sources`"
    >
      <h3 :id="`${idPrefix}-sources`" class="impact-method__title">
        {{ t('category.ecoscorePage.method.sources.title') }}
      </h3>
      <p class="impact-method__text">
        {{ t('category.ecoscorePage.method.sources.data') }}
      </p>
      <p v-if="stats?.brandCount" class="impact-method__text" data-test="brands">
        {{
          t('category.ecoscorePage.method.sources.brands', {
            count: fmt(stats.brandCount),
          })
        }}
      </p>
      <p class="impact-method__text">
        <NuxtLink :to="partnersPath">
          {{ t('category.ecoscorePage.method.sources.partners') }}
        </NuxtLink>
      </p>
      <ul v-if="audit?.sources?.length" class="impact-method__list">
        <li v-for="source in audit.sources" :key="source.id ?? source.title">
          <a
            v-if="source.url"
            :href="source.url"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ sourceLabel(source) }}
          </a>
          <span v-else>{{ sourceLabel(source) }}</span>
        </li>
      </ul>
    </section>

    <section
      class="impact-method__block"
      data-test="method-update"
      :aria-labelledby="`${idPrefix}-update`"
    >
      <h3 :id="`${idPrefix}-update`" class="impact-method__title">
        {{ t('category.ecoscorePage.method.update.title') }}
      </h3>
      <p class="impact-method__text">
        {{ t('category.ecoscorePage.method.update.body') }}
      </p>
      <p v-if="stats?.outdatedCount" class="impact-method__text" data-test="outdated-fate">
        {{ t('category.ecoscorePage.method.update.outdated') }}
      </p>
      <p class="impact-method__text">
        <NuxtLink :to="contactPath">
          {{ t('category.ecoscorePage.method.update.report') }}
        </NuxtLink>
      </p>
    </section>

    <section
      class="impact-method__block"
      data-test="method-independence"
      :aria-labelledby="`${idPrefix}-independence`"
    >
      <h3 :id="`${idPrefix}-independence`" class="impact-method__title">
        {{ t('category.ecoscorePage.method.independence.title') }}
      </h3>
      <p class="impact-method__text">
        {{ t('category.ecoscorePage.method.independence.body') }}
      </p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  ImpactScoreAuditSourceDto,
  ImpactScoreConfigDto,
  ImpactScoreStatsDto,
} from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const props = defineProps<{
  config: ImpactScoreConfigDto | null
  stats: ImpactScoreStatsDto | null
  categoryLabel: string
  /** Display label of each criterion key. */
  criteriaLabels: Record<string, string>
  idPrefix: string
}>()

const { t, locale } = useI18n()
const partnersPath = computed(() =>
  resolveLocalizedRoutePath('partners', locale.value)
)
const contactPath = computed(() =>
  resolveLocalizedRoutePath('contact', locale.value)
)

const audit = computed(() => props.config?.audit ?? null)

const numberFormat = computed(() => new Intl.NumberFormat(locale.value))
const fmt = (value: number | undefined) => numberFormat.value.format(value ?? 0)

const notRatedCount = computed(
  () =>
    (props.stats?.notRatedLowCoverageCount ?? 0) +
    (props.stats?.notRatedNoCriteriaCount ?? 0)
)

const minCoveragePercent = computed(() => {
  const value = props.config?.minRealWeightCoverage
  return value == null ? null : Math.round(value * 100)
})

const lastScoredLabel = computed(() => {
  const at = props.stats?.lastScoredAt
  if (!at) {
    return t('category.ecoscorePage.method.method.dateUnknown')
  }
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'long',
    timeZone: 'UTC',
  }).format(new Date(at))
})

const directionLabel = (direction: string | null | undefined) => {
  switch (direction) {
    case 'higher_is_better':
      return t('category.ecoscorePage.method.method.higher')
    case 'lower_is_better':
      return t('category.ecoscorePage.method.method.lower')
    case 'categorical':
      return t('category.ecoscorePage.method.method.categorical')
    default:
      return '—'
  }
}

const criteriaRows = computed(() => {
  const weights = props.config?.criteriasPonderation ?? {}
  const auditByKey = new Map(
    (audit.value?.criteria ?? []).map(criterion => [criterion.key, criterion])
  )
  return Object.entries(weights)
    .filter(([, weight]) => (weight ?? 0) > 0)
    .sort((a, b) => (b[1] ?? 0) - (a[1] ?? 0))
    .map(([key, weight]) => {
      const reasoned = auditByKey.get(key)
      return {
        key,
        label: props.criteriaLabels[key] ?? key,
        weightLabel: `${Math.round((weight ?? 0) * 100)}%`,
        directionLabel: directionLabel(reasoned?.directionality),
        rationale: reasoned?.rationale || '—',
      }
    })
})

const coverageRows = computed(() => {
  const stats = props.stats
  const coverage = stats?.coverageByCriterion ?? {}
  const rated = stats?.ratedCount ?? 0
  if (!rated || !Object.keys(coverage).length) {
    return []
  }
  return criteriaRows.value.map(row => {
    const percent = Math.round(((coverage[row.key] ?? 0) / rated) * 100)
    return {
      key: row.key,
      label: row.label,
      percent,
      aria: t('category.ecoscorePage.method.coverage.aria', {
        criterion: row.label,
        percent,
      }),
    }
  })
})

const histogramRows = computed(() => {
  const stats = props.stats
  const histogram = stats?.scoreHistogram ?? []
  const width = stats?.scoreBandWidth || 2
  const max = Math.max(0, ...histogram)
  if (!max) {
    return []
  }
  return histogram.map((count, index) => {
    const label = `${index * width}–${(index + 1) * width}`
    return {
      label,
      count,
      percent: Math.round((count / max) * 100),
      aria: t('category.ecoscorePage.method.distribution.aria', {
        band: label,
        count,
      }),
    }
  })
})

const sourceLabel = (source: ImpactScoreAuditSourceDto) =>
  [source.title, source.authorsOrOrg, source.year]
    .filter(part => part != null && part !== '')
    .join(' · ')
</script>

<style scoped lang="sass">
.impact-method
  display: flex
  flex-direction: column
  gap: 2rem

.impact-method__title
  font-size: 1.25rem
  margin-bottom: 0.5rem

.impact-method__subtitle
  font-size: 1.05rem
  margin: 1rem 0 0.25rem

.impact-method__text
  margin-bottom: 0.75rem

.impact-method__list
  padding-left: 1.25rem
  margin-bottom: 0.75rem

.impact-method__alert
  margin-bottom: 0.75rem

.impact-method__table-wrap
  overflow-x: auto

.impact-method__table
  width: 100%
  border-collapse: collapse

  th,
  td
    text-align: left
    vertical-align: top
    padding: 0.5rem
    border-bottom: 1px solid rgba(var(--v-border-color), var(--v-border-opacity))

.impact-method__sr
  position: absolute
  width: 1px
  height: 1px
  overflow: hidden
  clip: rect(0, 0, 0, 0)

.impact-method__bars
  list-style: none
  padding: 0
  margin: 0 0 0.75rem

  li
    display: grid
    grid-template-columns: minmax(6rem, 12rem) 1fr 3.5rem
    align-items: center
    gap: 0.75rem
    padding: 0.25rem 0

.impact-method__bar
  display: block
  height: 0.75rem
  border-radius: 0.375rem
  background: rgba(var(--v-theme-on-surface), 0.12)
  overflow: hidden

.impact-method__bar-fill
  display: block
  height: 100%
  background: rgb(var(--v-theme-primary))

.impact-method__bar-fill--alt
  background: rgb(var(--v-theme-secondary))

.impact-method__bar-value
  text-align: right
  font-variant-numeric: tabular-nums

@media (max-width: 600px)
  .impact-method__bars li
    grid-template-columns: 1fr 3rem
    .impact-method__bar-label
      grid-column: 1 / -1
</style>
