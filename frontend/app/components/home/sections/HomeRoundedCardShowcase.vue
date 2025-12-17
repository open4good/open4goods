<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AccentCorner } from '~/components/shared/cards/RoundedCornerCard.vue'
import RoundedCornerCard from '~/components/shared/cards/RoundedCornerCard.vue'

type CornerSize = 'sm' | 'md' | 'lg'
type CornerVariant = 'icon' | 'text' | 'custom' | 'none'
type RoundedSize = 'sm' | 'md' | 'lg'
type SurfaceTone = 'glass' | 'strong'

type ShowcaseCard = {
  id: string
  eyebrow: string
  title: string
  subtitle: string
  accentCorner: AccentCorner
  cornerVariant: CornerVariant
  cornerSize: CornerSize
  rounded: RoundedSize
  surface: SurfaceTone
  cornerLabel?: string
  cornerIcon?: string
  tooltip: string
  bullets: string[]
  selected: boolean
  selectable?: boolean
  ariaLabel: string
}

const { t, tm } = useI18n()

const cardState = ref<Record<string, boolean>>({
  impact: true,
  text: false,
  compact: false,
  custom: false,
  dual: true,
})

const customScore = 72
const customScoreLabel = computed(() =>
  t('home.roundedCards.samples.custom.progressLabel', { value: customScore })
)

const toStringArray = (value: unknown): string[] => {
  if (!Array.isArray(value)) {
    return []
  }

  return value
    .map(entry => (typeof entry === 'string' ? entry.trim() : ''))
    .filter((entry): entry is string => entry.length > 0)
}

const cards = computed<ShowcaseCard[]>(() => [
  {
    id: 'impact',
    eyebrow: t('home.roundedCards.samples.impact.eyebrow'),
    title: t('home.roundedCards.samples.impact.title'),
    subtitle: t('home.roundedCards.samples.impact.subtitle'),
    accentCorner: 'bottom-right',
    cornerVariant: 'icon',
    cornerSize: 'lg',
    rounded: 'lg',
    surface: 'strong',
    tooltip: t('home.roundedCards.samples.impact.tooltip'),
    bullets: toStringArray(tm('home.roundedCards.samples.impact.bullets')),
    selected: cardState.value.impact,
    ariaLabel: `${t('home.roundedCards.ariaLabel')} - ${t('home.roundedCards.samples.impact.title')}`,
  },
  {
    id: 'text',
    eyebrow: t('home.roundedCards.samples.text.eyebrow'),
    title: t('home.roundedCards.samples.text.title'),
    subtitle: t('home.roundedCards.samples.text.subtitle'),
    accentCorner: 'top-right',
    cornerVariant: 'text',
    cornerSize: 'md',
    rounded: 'md',
    surface: 'glass',
    cornerLabel: t('home.roundedCards.samples.text.cornerLabel'),
    cornerIcon: 'mdi-star-four-points-outline',
    tooltip: t('home.roundedCards.samples.text.tooltip'),
    bullets: toStringArray(tm('home.roundedCards.samples.text.bullets')),
    selected: cardState.value.text,
    ariaLabel: `${t('home.roundedCards.ariaLabel')} - ${t('home.roundedCards.samples.text.title')}`,
  },
  {
    id: 'compact',
    eyebrow: t('home.roundedCards.samples.compact.eyebrow'),
    title: t('home.roundedCards.samples.compact.title'),
    subtitle: t('home.roundedCards.samples.compact.subtitle'),
    accentCorner: 'top-left',
    cornerVariant: 'icon',
    cornerSize: 'sm',
    rounded: 'sm',
    surface: 'glass',
    tooltip: t('home.roundedCards.samples.compact.tooltip'),
    bullets: toStringArray(tm('home.roundedCards.samples.compact.bullets')),
    selected: cardState.value.compact,
    ariaLabel: `${t('home.roundedCards.ariaLabel')} - ${t('home.roundedCards.samples.compact.title')}`,
  },
  {
    id: 'custom',
    eyebrow: t('home.roundedCards.samples.custom.eyebrow'),
    title: t('home.roundedCards.samples.custom.title'),
    subtitle: t('home.roundedCards.samples.custom.subtitle'),
    accentCorner: 'bottom-left',
    cornerVariant: 'custom',
    cornerSize: 'md',
    rounded: 'md',
    surface: 'strong',
    tooltip: t('home.roundedCards.samples.custom.tooltip'),
    bullets: toStringArray(tm('home.roundedCards.samples.custom.bullets')),
    selected: cardState.value.custom,
    ariaLabel: `${t('home.roundedCards.ariaLabel')} - ${t('home.roundedCards.samples.custom.title')}`,
  },
  {
    id: 'dual',
    eyebrow: t('home.roundedCards.samples.dual.eyebrow'),
    title: t('home.roundedCards.samples.dual.title'),
    subtitle: t('home.roundedCards.samples.dual.subtitle'),
    accentCorner: 'bottom-right',
    cornerVariant: 'text',
    cornerSize: 'md',
    rounded: 'md',
    surface: 'glass',
    cornerLabel: t('home.roundedCards.samples.dual.cornerLabel'),
    tooltip: t('home.roundedCards.samples.dual.tooltip'),
    bullets: toStringArray(tm('home.roundedCards.samples.dual.bullets')),
    selected: cardState.value.dual,
    ariaLabel: `${t('home.roundedCards.ariaLabel')} - ${t('home.roundedCards.samples.dual.title')}`,
  },
])

const updateSelected = (id: string, value: boolean) => {
  cardState.value = {
    ...cardState.value,
    [id]: value,
  }
}
</script>

<template>
  <section
    class="home-card-showcase"
    aria-labelledby="home-card-showcase-title"
  >
    <v-container fluid class="home-card-showcase__container">
      <div class="home-card-showcase__header">
        <p class="home-card-showcase__eyebrow">
          {{ t('home.roundedCards.eyebrow') }}
        </p>
        <h2 id="home-card-showcase-title" class="home-card-showcase__title">
          {{ t('home.roundedCards.title') }}
        </h2>
        <p class="home-card-showcase__subtitle">
          {{ t('home.roundedCards.subtitle') }}
        </p>
      </div>

      <v-row
        class="home-card-showcase__grid"
        align="stretch"
        justify="center"
        dense
      >
        <v-col v-for="card in cards" :key="card.id" cols="12" md="6" lg="4">
          <RoundedCornerCard
            class="home-card-showcase__card"
            :eyebrow="card.eyebrow"
            :title="card.title"
            :subtitle="card.subtitle"
            :accent-corner="card.accentCorner"
            :corner-size="card.cornerSize"
            :corner-variant="card.cornerVariant"
            :corner-label="card.cornerLabel"
            :corner-icon="card.cornerIcon"
            :corner-tooltip="card.tooltip"
            :rounded="card.rounded"
            :surface="card.surface"
            :selected="card.selected"
            :selectable="card.selectable ?? true"
            :hover-elevation="14"
            :elevation="9"
            :aria-label="card.ariaLabel"
            @update:selected="value => updateSelected(card.id, value)"
          >
            <ul v-if="card.bullets.length" class="home-card-showcase__bullets">
              <li
                v-for="(bullet, index) in card.bullets"
                :key="`${card.id}-bullet-${index}`"
              >
                {{ bullet }}
              </li>
            </ul>

            <template v-if="card.id === 'custom'" #corner>
              <div class="home-card-showcase__corner-meter">
                <v-progress-circular
                  :model-value="customScore"
                  size="44"
                  width="5"
                  color="accent-supporting"
                  rotate="-90"
                  :title="customScoreLabel"
                >
                  <span class="home-card-showcase__corner-meter-text">{{
                    customScoreLabel
                  }}</span>
                </v-progress-circular>
              </div>
            </template>

            <template v-if="card.id === 'dual'" #actions>
              <v-btn color="primary" variant="tonal" size="small">
                {{ t('home.cta.button') }}
              </v-btn>
              <v-btn variant="text" size="small" color="text-neutral-secondary">
                {{ t('home.roundedCards.samples.dual.tooltip') }}
              </v-btn>
            </template>
          </RoundedCornerCard>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-card-showcase
  padding: clamp(1.5rem, 5vw, 2.75rem) clamp(1.25rem, 6vw, 3.5rem)
  background: linear-gradient(140deg, rgba(var(--v-theme-surface-ice-050), 0.8), rgba(var(--v-theme-surface-muted), 0.9))
  border-radius: clamp(1.5rem, 4vw, 2.5rem)
  box-shadow: 0 18px 42px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-card-showcase__container
  max-width: 1260px
  margin: 0 auto

.home-card-showcase__header
  text-align: center
  display: flex
  flex-direction: column
  gap: 0.5rem
  margin-bottom: clamp(1.25rem, 4vw, 2rem)

.home-card-showcase__eyebrow
  margin: 0
  font-weight: 700
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)

.home-card-showcase__title
  margin: 0
  font-size: clamp(1.6rem, 4vw, 2.1rem)
  line-height: 1.2
  color: rgb(var(--v-theme-text-neutral-strong))

.home-card-showcase__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  max-width: 780px
  align-self: center
  line-height: 1.4

.home-card-showcase__grid
  --v-gutter-x: clamp(1rem, 3vw, 1.5rem)
  --v-gutter-y: clamp(1rem, 3vw, 1.5rem)

.home-card-showcase__card
  height: 100%

.home-card-showcase__bullets
  margin: 0
  padding-inline-start: 1rem
  display: grid
  gap: 0.35rem
  color: rgb(var(--v-theme-text-neutral-strong))
  line-height: 1.4

.home-card-showcase__bullets li
  margin: 0

.home-card-showcase__corner-meter
  display: grid
  place-items: center
  color: rgb(var(--v-theme-text-on-accent))

.home-card-showcase__corner-meter-text
  font-weight: 800
  font-size: 0.85rem

@media (max-width: 960px)
  .home-card-showcase
    padding: clamp(1.2rem, 5vw, 2rem)
    border-radius: clamp(1.1rem, 3vw, 1.6rem)

  .home-card-showcase__title
    font-size: clamp(1.45rem, 5vw, 1.8rem)

  .home-card-showcase__subtitle
    font-size: 0.95rem

@media (min-width: 1280px)
  .home-card-showcase__grid
    --v-gutter-y: 1.75rem
    --v-gutter-x: 1.75rem

  .home-card-showcase__card
    transform-origin: center

    &:hover
      transform: translateY(-2px)
</style>
