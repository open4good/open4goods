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
      <div class="impact-orbit__diagram">
        <div class="impact-orbit__halo" aria-hidden="true" />
        <div class="impact-orbit__ring" aria-hidden="true" />

        <div class="impact-orbit__center" role="presentation">
          <div class="impact-orbit__center-badge">
            <div class="impact-orbit__center-icon" aria-hidden="true">
              <v-avatar v-if="categoryIcon" size="68" rounded>
                <v-img :src="categoryIcon" :alt="categoryName" cover />
              </v-avatar>
              <v-icon v-else icon="mdi-leaf" size="44" />
            </div>
            <div class="impact-orbit__center-meta">
              <div class="impact-orbit__center-name">{{ categoryName }}</div>
              <p class="impact-orbit__center-label">
                {{ t('category.ecoscorePage.sections.overview.visualization.centerLabel', { category: categoryName }) }}
              </p>
            </div>
          </div>

          <ImpactScore
            :score="score"
            :max="max"
            size="xlarge"
            color="primary"
            inactive-color="surface-primary-080"
            show-value
            class="impact-orbit__center-score"
          />
        </div>

        <div v-if="!isStacked" class="impact-orbit__nodes">
          <div
            v-for="(criterion, index) in criteria"
            :key="criterion.key"
            class="impact-orbit__node"
            :style="getOrbitStyle(index, criteria.length)"
          >
            <div class="impact-orbit__connector" aria-hidden="true" />
            <article class="impact-orbit__node-card" :aria-label="criterion.label">
              <div class="impact-orbit__node-head">
                <div class="impact-orbit__node-icon" aria-hidden="true">
                  <v-icon v-if="criterion.icon" :icon="criterion.icon" size="22" />
                  <span v-else>{{ criterion.fallback }}</span>
                </div>
                <div class="impact-orbit__node-title">{{ criterion.label }}</div>
              </div>

              <p v-if="criterion.description" class="impact-orbit__node-description">
                {{ criterion.description }}
              </p>

              <div class="impact-orbit__node-meta">
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
              <div class="impact-orbit__node-icon" aria-hidden="true">
                <v-icon v-if="criterion.icon" :icon="criterion.icon" size="22" />
                <span v-else>{{ criterion.fallback }}</span>
              </div>
              <div>
                <div class="impact-orbit__node-title">{{ criterion.label }}</div>
                <div class="impact-orbit__stacked-weight">{{ weightLabel(criterion.weight) }}</div>
              </div>
            </header>
            <p v-if="criterion.description" class="impact-orbit__node-description">
              {{ criterion.description }}
            </p>
            <div class="impact-orbit__node-meta">
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

const isStacked = computed(() => display.smAndDown.value)

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
    '--orbit-radius': '46%'
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

.impact-orbit__diagram
  position: relative
  min-height: 420px
  display: flex
  align-items: center
  justify-content: center

.impact-orbit__halo
  position: absolute
  inset: 20% 18%
  background: radial-gradient(circle, rgba(var(--v-theme-primary), 0.08), rgba(var(--v-theme-surface-default), 0))
  filter: blur(30px)
  pointer-events: none

.impact-orbit__ring
  position: relative
  width: 78%
  max-width: 560px
  aspect-ratio: 1
  border-radius: 50%
  border: 1px dashed rgba(var(--v-theme-primary), 0.35)
  background: radial-gradient(circle, rgba(var(--v-theme-surface-glass), 0.92), rgba(var(--v-theme-surface-default), 0.94))
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.15)

.impact-orbit__center
  position: absolute
  top: 50%
  left: 50%
  transform: translate(-50%, -50%)
  display: grid
  place-items: center
  text-align: center
  gap: 1rem
  z-index: 2

.impact-orbit__center-badge
  display: inline-flex
  align-items: center
  gap: 0.85rem
  padding: 0.75rem 1rem
  border-radius: 999px
  background: rgba(var(--v-theme-surface-default), 0.95)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08)

.impact-orbit__center-icon
  display: inline-flex
  align-items: center
  justify-content: center
  width: 70px
  height: 70px
  border-radius: 18px
  background: rgba(var(--v-theme-surface-primary-120), 0.8)
  color: rgb(var(--v-theme-primary))
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.08)

.impact-orbit__center-meta
  text-align: left

.impact-orbit__center-name
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 1.1rem

.impact-orbit__center-label
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.95rem

.impact-orbit__center-score
  width: 240px
  height: 240px
  display: grid
  place-items: center
  padding: 0.5rem
  border-radius: 50%
  background: rgba(var(--v-theme-surface-default), 0.85)
  box-shadow: inset 0 0 0 2px rgba(var(--v-theme-border-primary-strong), 0.18), 0 16px 40px rgba(0, 0, 0, 0.08)

.impact-orbit__nodes
  position: relative
  width: 100%
  height: 100%
  min-height: 420px

.impact-orbit__node
  position: absolute
  top: 50%
  left: 50%
  transform: rotate(var(--orbit-angle)) translate(var(--orbit-radius)) rotate(calc(-1 * var(--orbit-angle)))
  transform-origin: center
  transition: transform 350ms ease, opacity 350ms ease
  opacity: 0.98

.impact-orbit__node-card
  width: 215px
  background: rgba(var(--v-theme-surface-default), 0.96)
  border-radius: 14px
  padding: 0.85rem
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.12)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  backdrop-filter: blur(4px)

.impact-orbit__connector
  position: absolute
  top: 50%
  left: calc(-1 * var(--orbit-radius))
  width: calc(var(--orbit-radius) + 40px)
  height: 2px
  background: linear-gradient(90deg, rgba(var(--v-theme-primary), 0.65), rgba(var(--v-theme-primary), 0))
  transform: translate(-30px, -50%)
  transform-origin: right center

.impact-orbit__node-head
  display: flex
  align-items: center
  gap: 0.65rem
  margin-bottom: 0.35rem

.impact-orbit__node-icon
  display: inline-flex
  align-items: center
  justify-content: center
  width: 40px
  height: 40px
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-120), 0.75)
  color: rgb(var(--v-theme-primary))
  font-weight: 700

.impact-orbit__node-title
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))

.impact-orbit__node-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.95rem

.impact-orbit__node-meta
  display: flex
  flex-wrap: wrap
  gap: 0.5rem
  margin-top: 0.5rem

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

  .impact-orbit__nodes
    display: none

@media (prefers-reduced-motion: reduce)
  .impact-orbit__node
    transition: none
</style>
