<template>
  <v-card
    class="impact-orbit"
    elevation="2"
    rounded="xl"
    data-test="impact-score-orbit"
  >
    <!-- HEADER -->
    <div class="impact-orbit__header">
      <div class="impact-orbit__eyebrow">
        {{ t('category.ecoscorePage.sections.overview.visualization.eyebrow') }}
      </div>

      <div class="impact-orbit__title">
        {{ t('category.ecoscorePage.sections.overview.visualization.title') }}
      </div>

      <p class="impact-orbit__subtitle">
        {{ t('category.ecoscorePage.sections.overview.visualization.subtitle', { category: categoryName }) }}
      </p>
    </div>

    <!-- BODY -->
    <div class="impact-orbit__layout">
      <!-- CENTER BLOCK -->
      <section class="impact-orbit__center-wrapper" aria-label="Impact score global">
        <div class="impact-orbit__center-orbit" aria-hidden="true">
          <div class="impact-orbit__ring impact-orbit__ring--outer" />
          <div class="impact-orbit__ring impact-orbit__ring--inner" />
        </div>

        <div class="impact-orbit__center-content">
          <div class="impact-orbit__center-badge">
            <div class="impact-orbit__center-icon" aria-hidden="true">
              <v-avatar v-if="categoryIcon" size="52" rounded>
                <v-img :src="categoryIcon" :alt="categoryName" cover />
              </v-avatar>
              <v-icon v-else icon="mdi-leaf" size="40" />
            </div>

            <div class="impact-orbit__center-meta">
              <div class="impact-orbit__center-name">
                {{ categoryName }}
              </div>
              <p class="impact-orbit__center-label">
                {{ t('category.ecoscorePage.sections.overview.visualization.centerLabel', { category: categoryName }) }}
              </p>
            </div>
          </div>

          <div class="impact-orbit__center-score">
            <div class="impact-orbit__center-score-label">
              {{ t('category.ecoscorePage.sections.overview.visualization.centerLabel', { category: categoryName }) }}
            </div>

            <ImpactScore
              :score="score"
              :max="max"
              size="xlarge"
              color="primary"
              inactive-color="surface-primary-080"
              show-value
              class="impact-orbit__center-score-gauge"
            />
          </div>
        </div>
      </section>

      <!-- CRITERIA LIST -->
      <section class="impact-orbit__criteria" aria-label="Répartition par critère">
        <article
          v-for="criterion in criteria"
          :key="criterion.key"
          class="impact-orbit__criterion-card"
        >
          <header class="impact-orbit__criterion-header">
            <div class="impact-orbit__criterion-icon" aria-hidden="true">
              <v-icon v-if="criterion.icon" :icon="criterion.icon" size="22" />
              <span v-else>{{ criterion.fallback }}</span>
            </div>

            <div class="impact-orbit__criterion-header-main">
              <div class="impact-orbit__criterion-title">
                {{ criterion.label }}
              </div>

              <div class="impact-orbit__criterion-topline">
                <span class="impact-orbit__criterion-weight">
                  {{ weightLabel(criterion.weight) }}
                </span>

                <span
                  v-if="criterion.score != null"
                  class="impact-orbit__criterion-score"
                >
                  {{ formatCriterionScore(criterion.score, criterion.maxScore) }}
                </span>
              </div>

              <div
                v-if="criterion.score != null"
                class="impact-orbit__criterion-score-bar"
              >
                <div
                  class="impact-orbit__criterion-score-bar-fill"
                  :style="{ '--score-ratio': getCriterionRatio(criterion.score, criterion.maxScore) }"
                />
              </div>
            </div>
          </header>

          <p
            v-if="criterion.description"
            class="impact-orbit__criterion-description"
          >
            {{ criterion.description }}
          </p>

          <div class="impact-orbit__criterion-meta">
            <v-chip
              v-for="stage in criterion.lifecycles"
              :key="`${criterion.key}-${stage.code}`"
              class="impact-orbit__chip"
              color="secondary"
              size="small"
              variant="tonal"
            >
              {{ stage.label }}
            </v-chip>
          </div>
        </article>
      </section>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'

type LifecycleStage = {
  code: string
  label: string
}

type OrbitCriterion = {
  key: string
  label: string
  description?: string | null
  weight?: number | null
  icon?: string | null
  fallback: string
  lifecycles: LifecycleStage[]
  // optionnel : score par critère
  score?: number | null
  maxScore?: number | null
}

const _props = defineProps({
  score: {
    type: Number,
    required: true,
  },
  max: {
    type: Number,
    default: 5,
  },
  categoryName: {
    type: String,
    required: true,
  },
  categoryIcon: {
    type: String,
    default: '',
  },
  criteria: {
    type: Array as () => OrbitCriterion[],
    default: () => [],
  },
})

const { t } = useI18n()

const weightLabel = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return t('category.ecoscorePage.sections.overview.visualization.weightFallback')
  }

  return t('category.ecoscorePage.sections.overview.visualization.weight', {
    value: Math.round(value * 100),
  })
}

const formatCriterionScore = (
  value?: number | null,
  maxValue?: number | null,
) => {
  if (value == null || Number.isNaN(value)) {
    return '–'
  }

  const formatter = new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 1,
  })

  const effectiveMax = maxValue ?? _props.max

  return `${formatter.format(value)} / ${formatter.format(effectiveMax)}`
}

const getCriterionRatio = (
  value?: number | null,
  maxValue?: number | null,
) => {
  if (value == null || Number.isNaN(value)) {
    return 0
  }

  const effectiveMax = maxValue ?? _props.max
  if (!effectiveMax || Number.isNaN(effectiveMax)) {
    return 0
  }

  const ratio = value / effectiveMax
  return Math.max(0, Math.min(1, ratio))
}
</script>

<style scoped lang="sass">
.impact-orbit
  position: relative
  overflow: hidden
  padding: clamp(1.5rem, 2vw, 2rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-050), 0.95), rgba(var(--v-theme-surface-glass), 0.9))
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.impact-orbit__header
  display: flex
  flex-direction: column
  gap: 0.25rem
  text-align: left
  margin-bottom: 1.25rem

.impact-orbit__eyebrow
  display: inline-flex
  align-items: center
  gap: 0.35rem
  padding: 0.35rem 0.75rem
  border-radius: 999px
  font-size: 0.85rem
  font-weight: 600
  color: rgb(var(--v-theme-primary))
  background-color: rgba(var(--v-theme-surface-primary-120), 0.65)
  width: fit-content

.impact-orbit__title
  font-size: clamp(1.4rem, 1vw + 1rem, 1.8rem)
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))

.impact-orbit__subtitle
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

/* LAYOUT */

.impact-orbit__layout
  display: flex
  flex-direction: column
  gap: 1.5rem

@media (min-width: 960px)
  .impact-orbit__layout
    flex-direction: row
    align-items: stretch
    gap: 2rem

/* CENTER BLOCK */

.impact-orbit__center-wrapper
  position: relative
  display: flex
  align-items: center
  justify-content: center
  padding: 1rem 0
  flex: 0 0 100%

@media (min-width: 960px)
  .impact-orbit__center-wrapper
    flex: 0 0 40%

.impact-orbit__center-orbit
  position: absolute
  inset: 0
  display: grid
  place-items: center
  pointer-events: none

.impact-orbit__ring
  border-radius: 50%
  aspect-ratio: 1
  border: 1px dashed rgba(var(--v-theme-primary), 0.25)
  width: min(70vw, 260px)

.impact-orbit__ring--inner
  width: min(48vw, 190px)
  border-style: solid
  border-color: rgba(var(--v-theme-primary), 0.15)

.impact-orbit__center-content
  position: relative
  display: grid
  place-items: center
  gap: 0.9rem
  z-index: 1

.impact-orbit__center-badge
  display: inline-flex
  align-items: center
  gap: 0.75rem
  padding: 0.6rem 0.9rem
  border-radius: 999px
  background: rgba(var(--v-theme-surface-default), 0.98)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08)
  max-width: min(100%, 320px)

.impact-orbit__center-icon
  display: inline-flex
  align-items: center
  justify-content: center
  width: 56px
  height: 56px
  border-radius: 18px
  background: rgba(var(--v-theme-surface-primary-120), 0.85)
  color: rgb(var(--v-theme-primary))
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.08)
  flex-shrink: 0

.impact-orbit__center-meta
  text-align: left

.impact-orbit__center-name
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 1.05rem

.impact-orbit__center-label
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.9rem

.impact-orbit__center-score
  width: 210px
  height: 210px
  display: grid
  grid-template-rows: auto 1fr
  place-items: center
  padding: 0.9rem
  border-radius: 50%
  background: rgba(var(--v-theme-surface-default), 0.95)
  box-shadow: inset 0 0 0 2px rgba(var(--v-theme-border-primary-strong), 0.18), 0 16px 40px rgba(0, 0, 0, 0.08)

.impact-orbit__center-score-label
  font-size: 0.8rem
  font-weight: 600
  text-transform: uppercase
  letter-spacing: 0.06em
  color: rgb(var(--v-theme-text-neutral-secondary))

.impact-orbit__center-score-gauge
  width: 100%
  height: 100%
  display: grid
  place-items: center

@media (max-width: 599px)
  .impact-orbit__center-score
    width: 180px
    height: 180px

/* CRITERIA LIST */

.impact-orbit__criteria
  display: flex
  flex-direction: column
  gap: 0.75rem
  flex: 1

.impact-orbit__criterion-card
  padding: 0.9rem 1rem
  border-radius: 16px
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  background: rgba(var(--v-theme-surface-default), 0.98)
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.04)

.impact-orbit__criterion-header
  display: flex
  align-items: flex-start
  gap: 0.75rem

.impact-orbit__criterion-header-main
  flex: 1
  min-width: 0

.impact-orbit__criterion-icon
  display: inline-flex
  align-items: center
  justify-content: center
  width: 40px
  height: 40px
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-120), 0.75)
  color: rgb(var(--v-theme-primary))
  font-weight: 700
  flex-shrink: 0

.impact-orbit__criterion-title
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 0.95rem
  margin-bottom: 0.15rem

.impact-orbit__criterion-topline
  display: flex
  flex-wrap: wrap
  gap: 0.5rem
  align-items: baseline
  margin-bottom: 0.2rem

.impact-orbit__criterion-weight
  font-size: 0.8rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-secondary))

.impact-orbit__criterion-score
  font-size: 0.8rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.impact-orbit__criterion-score-bar
  position: relative
  width: 100%
  height: 4px
  border-radius: 999px
  background: rgba(var(--v-theme-surface-primary-120), 0.8)
  overflow: hidden

.impact-orbit__criterion-score-bar-fill
  position: absolute
  inset: 0
  transform-origin: left center
  transform: scaleX(var(--score-ratio))
  background: rgba(var(--v-theme-primary), 0.9)

.impact-orbit__criterion-description
  margin: 0.45rem 0 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.9rem

.impact-orbit__criterion-meta
  display: flex
  flex-wrap: wrap
  gap: 0.4rem
  margin-top: 0.5rem

.impact-orbit__chip
  font-weight: 600
  font-size: 0.8rem

@media (max-width: 959px)
  .impact-orbit
    padding: 1.25rem

  .impact-orbit__header
    margin-bottom: 0.9rem
</style>
