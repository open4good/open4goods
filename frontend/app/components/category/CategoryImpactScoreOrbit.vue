<template>
  <v-card
    class="impact-orbit"
    elevation="2"
    rounded="xl"
    :class="{ 'impact-orbit--stacked': isStacked }"
    data-test="impact-score-orbit"
  >
    <div class="impact-orbit__header">
      <div class="impact-orbit__eyebrow">{{ t('category.ecoscorePage.sections.overview.visualization.eyebrow') }}</div>
      <div class="impact-orbit__title">{{ t('category.ecoscorePage.sections.overview.visualization.title') }}</div>
      <p class="impact-orbit__subtitle">
        {{ t('category.ecoscorePage.sections.overview.visualization.subtitle', { category: categoryName }) }}
      </p>
    </div>

    <div class="impact-orbit__body">
      <div class="impact-orbit__center">
        <div class="impact-orbit__center-glow" aria-hidden="true" />
        <div class="impact-orbit__center-card" role="presentation">
          <v-avatar v-if="categoryIcon" class="impact-orbit__center-avatar" size="70" rounded>
            <v-img :src="categoryIcon" :alt="categoryName" cover />
          </v-avatar>

          <div class="impact-orbit__center-content">
            <ImpactScore :score="score" :max="max" size="xlarge" color="primary" inactive-color="surface-primary-080" show-value />
            <p class="impact-orbit__center-label">
              {{ t('category.ecoscorePage.sections.overview.visualization.centerLabel', { category: categoryName }) }}
            </p>
          </div>
        </div>

        <div v-if="!isStacked" class="impact-orbit__spokes">
          <div
            v-for="(criterion, index) in criteria"
            :key="criterion.key"
            class="impact-orbit__spoke"
            :style="getOrbitStyle(index, criteria.length)"
          >
            <div class="impact-orbit__connector" aria-hidden="true" />
            <article class="impact-orbit__card" :aria-label="criterion.label">
              <header class="impact-orbit__card-header">
                <div class="impact-orbit__card-icon" aria-hidden="true">
                  <v-icon v-if="criterion.icon" :icon="criterion.icon" size="22" />
                  <span v-else>{{ criterion.fallback }}</span>
                </div>
                <div class="impact-orbit__card-title">{{ criterion.label }}</div>
              </header>

              <p v-if="criterion.description" class="impact-orbit__card-description">
                {{ criterion.description }}
              </p>

              <div class="impact-orbit__meta">
                <v-chip
                  class="impact-orbit__chip"
                  color="primary"
                  size="small"
                  variant="tonal"
                >
                  {{ weightLabel(criterion.weight) }}
                </v-chip>
                <v-chip
                  v-for="stage in criterion.lifecycles"
                  :key="`${criterion.key}-${stage.code}`"
                  class="impact-orbit__chip"
                  color="secondary"
                  size="small"
                  variant="outlined"
                >
                  {{ stage.label }}
                </v-chip>
              </div>
            </article>
          </div>
        </div>

        <div v-else class="impact-orbit__stacked">
          <article v-for="criterion in criteria" :key="criterion.key" class="impact-orbit__stacked-card">
            <header class="impact-orbit__stacked-header">
              <div class="impact-orbit__card-icon" aria-hidden="true">
                <v-icon v-if="criterion.icon" :icon="criterion.icon" size="22" />
                <span v-else>{{ criterion.fallback }}</span>
              </div>
              <div>
                <div class="impact-orbit__card-title">{{ criterion.label }}</div>
                <div class="impact-orbit__stacked-weight">{{ weightLabel(criterion.weight) }}</div>
              </div>
            </header>
            <p v-if="criterion.description" class="impact-orbit__card-description">
              {{ criterion.description }}
            </p>
            <div class="impact-orbit__meta">
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
        </div>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useDisplay } from 'vuetify'
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
const display = useDisplay()

const isStacked = computed(() => display.mdAndDown.value)

const weightLabel = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return t('category.ecoscorePage.sections.overview.visualization.weightFallback')
  }

  return t('category.ecoscorePage.sections.overview.visualization.weight', {
    value: Math.round(value * 100),
  })
}

const getOrbitStyle = (index: number, total: number) => {
  const angle = total > 0 ? (index / total) * 360 : 0
  return {
    '--orbit-angle': `${angle}deg`,
  }
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

.impact-orbit__body
  position: relative

.impact-orbit__center
  position: relative
  min-height: 360px

.impact-orbit--stacked .impact-orbit__center
  min-height: auto

.impact-orbit__center-glow
  position: absolute
  inset: 15% auto auto 50%
  width: 42%
  aspect-ratio: 1
  transform: translateX(-50%)
  background: radial-gradient(circle, rgba(var(--v-theme-primary), 0.14), rgba(var(--v-theme-surface-default), 0))
  filter: blur(25px)
  animation: orbitPulse 6s ease-in-out infinite
  pointer-events: none

.impact-orbit__center-card
  position: absolute
  top: 50%
  left: 50%
  transform: translate(-50%, -50%)
  display: flex
  flex-direction: column
  align-items: center
  gap: 0.75rem
  padding: 1.25rem 1.5rem
  border-radius: 20px
  background: rgba(var(--v-theme-surface-default), 0.95)
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.12)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  z-index: 2

.impact-orbit__center-avatar
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08)

.impact-orbit__center-content
  display: flex
  flex-direction: column
  align-items: center
  gap: 0.35rem

.impact-orbit__center-label
  margin: 0
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))
  text-align: center

.impact-orbit__spokes
  position: relative
  width: 100%
  height: 100%
  min-height: 360px

.impact-orbit__spoke
  position: absolute
  top: 50%
  left: 50%
  transform: rotate(var(--orbit-angle)) translate(calc(42% + 30px)) rotate(calc(-1 * var(--orbit-angle)))
  transform-origin: center
  transition: transform 350ms ease, opacity 350ms ease
  opacity: 0.96

.impact-orbit__card
  width: 220px
  background: rgba(var(--v-theme-surface-default), 0.96)
  border-radius: 16px
  padding: 0.95rem
  box-shadow: 0 16px 44px rgba(0, 0, 0, 0.12)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  backdrop-filter: blur(6px)

.impact-orbit__connector
  position: absolute
  top: 50%
  left: -34px
  width: 34px
  height: 2px
  background: linear-gradient(90deg, rgba(var(--v-theme-primary), 0.6), rgba(var(--v-theme-primary), 0))
  transform: translateY(-50%)

.impact-orbit__card-header
  display: flex
  align-items: center
  gap: 0.75rem
  margin-bottom: 0.5rem

.impact-orbit__card-icon
  display: inline-flex
  align-items: center
  justify-content: center
  width: 40px
  height: 40px
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-120), 0.75)
  color: rgb(var(--v-theme-primary))
  font-weight: 700

.impact-orbit__card-title
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))

.impact-orbit__card-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.95rem

.impact-orbit__meta
  display: flex
  flex-wrap: wrap
  gap: 0.5rem
  margin-top: 0.75rem

.impact-orbit__chip
  font-weight: 600

.impact-orbit__stacked
  display: flex
  flex-direction: column
  gap: 0.75rem
  margin-top: 1rem

.impact-orbit__stacked-card
  padding: 0.95rem 1rem
  border-radius: 16px
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  background: rgba(var(--v-theme-surface-default), 0.95)

.impact-orbit__stacked-header
  display: flex
  align-items: center
  gap: 0.75rem
  justify-content: space-between
  margin-bottom: 0.35rem

.impact-orbit__stacked-weight
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-weight: 600

@media (max-width: 960px)
  .impact-orbit
    padding: 1.25rem

  .impact-orbit__center-card
    position: relative
    top: 0
    left: 0
    transform: none
    margin: 0 auto

  .impact-orbit__spokes
    display: none

@media (prefers-reduced-motion: reduce)
  .impact-orbit__center-glow
    animation: none

  .impact-orbit__spoke
    transition: none

@keyframes orbitPulse
  0%, 100%
    opacity: 0.55
  50%
    opacity: 1
</style>
