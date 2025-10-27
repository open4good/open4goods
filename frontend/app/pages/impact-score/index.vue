<template>
  <div class="impact-score-page">
    <section class="impact-score-page__hero">
      <v-container class="py-12">
        <v-row class="impact-score-page__hero-row" align="center" justify="space-between">
          <v-col cols="12" md="7">
            <p class="impact-score-page__eyebrow">Impact environnemental</p>
            <h1 class="impact-score-page__title">
              L’Impact Score : évaluation de l'impact environnemental de vos produits
            </h1>
            <div class="impact-score-page__intro text-body-1">
              <TextContent bloc-id="ECOSCORE:1:" />
            </div>
          </v-col>
          <v-col cols="12" md="5" class="impact-score-page__hero-figure">
            <v-img
              src="https://nudger.fr/img/impactscore-illustration.png"
              alt="Illustration Impact Score"
              aspect-ratio="4/3"
              contain
            />
          </v-col>
        </v-row>
      </v-container>
    </section>

    <v-container class="impact-score-page__content py-12">
      <v-row class="impact-score-page__layout" align="start" justify="center">
        <v-col cols="12" md="3" class="impact-score-page__nav-col">
          <StickySectionNavigation
            :sections="navigationSections"
            :active-section="activeSection"
            :orientation="orientation"
            aria-label="Navigation Impact Score"
            @navigate="scrollToSection"
          />
        </v-col>

        <v-col cols="12" md="9" class="impact-score-page__main">
          <section :id="sectionIds.overview" class="impact-score-section impact-score-section--intro">
            <v-sheet class="impact-score-section__surface" rounded="xl" elevation="0">
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">Présentation</span>
                <h2 class="impact-score-section__title">Qu’est-ce que l’Impact Score&nbsp;?</h2>
              </header>

              <v-row class="impact-score-section__row" align="start">
                <v-col cols="12" md="7">
                  <div class="impact-score-section__body">
                    <TextContent bloc-id="ECOSCORE:2:" />
                  </div>

                  <v-card variant="flat" class="impact-score-rating" elevation="0">
                    <div class="impact-score-rating__value">
                      <span class="impact-score-rating__number">{{ formatCoeff(localRating) }}</span>
                      <span class="impact-score-rating__suffix">/5</span>
                    </div>
                    <v-rating
                      v-model="localRating"
                      half-increments
                      length="5"
                      readonly
                      :aria-label="`Évaluation : ${localRating} sur 5`"
                    />
                    <p class="impact-score-rating__caption">Évaluation indicative basée sur notre méthodologie</p>
                  </v-card>
                </v-col>

                <v-col cols="12" md="5">
                  <div class="impact-score-section__figure">
                    <v-img
                      src="https://nudger.fr/img/what-impactscore.png"
                      alt="Présentation Impact Score"
                      aspect-ratio="4/3"
                      contain
                    />
                  </div>
                </v-col>
              </v-row>
            </v-sheet>
          </section>

          <section :id="sectionIds.ecoscore" class="impact-score-section">
            <v-sheet class="impact-score-section__surface impact-score-section__surface--muted" rounded="xl" elevation="0">
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">Notre démarche</span>
                <h2 class="impact-score-section__title">
                  Notre ecoscore&nbsp;: transparent, innovant et performant
                </h2>
              </header>

              <v-row class="impact-score-section__row" align="center">
                <v-col cols="12" md="7">
                  <div class="impact-score-section__body">
                    <TextContent bloc-id="ECOSCORE:3:" />
                  </div>
                </v-col>
                <v-col cols="12" md="5">
                  <div class="impact-score-section__figure impact-score-section__figure--compact">
                    <v-img
                      src="https://nudger.fr/img/impactscore-illustration.png"
                      alt="Écoscore Nudger"
                      aspect-ratio="1"
                      contain
                    />
                  </div>
                </v-col>
              </v-row>
            </v-sheet>
          </section>

          <section :id="sectionIds.calculation" class="impact-score-section">
            <v-sheet class="impact-score-section__surface" rounded="xl" elevation="0">
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">Méthodologie</span>
                <h2 class="impact-score-section__title">Comment est calculé l'Impact Score&nbsp;?</h2>
              </header>

              <div class="impact-score-section__body">
                <p>
                  Les règles de calcul de l’ecoscore sont détaillées sur chaque produit (section «&nbsp;bilan écologique&nbsp;»).
                  Chaque catégorie de produit a ses propres critères et pondérations. Voici les catégories couvertes :
                </p>
              </div>

              <v-sheet class="impact-score-section__list" rounded="lg" elevation="0">
                <v-list density="comfortable" bg-color="transparent">
                  <v-list-item
                    v-for="v in verticals"
                    :key="v.url"
                    :href="v.url"
                    :title="v.title"
                    link
                  >
                    <template #append>
                      <v-icon icon="mdi-chevron-right" />
                    </template>
                  </v-list-item>
                </v-list>
              </v-sheet>

              <div class="impact-score-section__body">
                <TextContent bloc-id="ECOSCORE:4:" />
              </div>
            </v-sheet>
          </section>

          <section :id="sectionIds.criteria" class="impact-score-section">
            <v-sheet class="impact-score-section__surface" rounded="xl" elevation="0">
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">Pondérations</span>
                <h2 class="impact-score-section__title">
                  Critères et coefficients de l'ecoscore téléviseurs
                </h2>
              </header>

              <v-table class="impact-score-table" density="comfortable">
                <thead>
                  <tr>
                    <th scope="col" class="text-left">Nom du critère</th>
                    <th scope="col" class="text-left">Coefficient (0–1)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(c, i) in criterias" :key="i">
                    <td>{{ c.name }}</td>
                    <td>{{ formatCoeff(c.coefficient) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <th scope="row">Total</th>
                    <td><strong>{{ formatCoeff(total) }}</strong></td>
                  </tr>
                </tfoot>
              </v-table>
            </v-sheet>
          </section>

          <section :id="sectionIds.dataQuality" class="impact-score-section">
            <v-sheet class="impact-score-section__surface impact-score-section__surface--muted" rounded="xl" elevation="0">
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">Analyse approfondie</span>
                <h2 class="impact-score-section__title">Relativisation et qualité de la donnée</h2>
              </header>

              <v-row class="impact-score-section__row" align="stretch" justify="space-between">
                <v-col cols="12" md="6">
                  <v-card variant="tonal" class="impact-score-card" color="primary">
                    <div class="impact-score-card__media">
                      <v-img
                        src="https://nudger.fr/img/relativisation.png"
                        alt="Relativisation"
                        aspect-ratio="1"
                        contain
                      />
                    </div>
                    <div class="impact-score-card__content">
                      <TextContent bloc-id="ECOSCORE:4-1:" />
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="12" md="6">
                  <v-card variant="tonal" class="impact-score-card" color="primary">
                    <div class="impact-score-card__media">
                      <v-img
                        src="https://nudger.fr/img/data-quality.png"
                        alt="Qualité de la donnée"
                        aspect-ratio="1"
                        contain
                      />
                    </div>
                    <div class="impact-score-card__content">
                      <TextContent bloc-id="ECOSCORE:4-2:" />
                    </div>
                  </v-card>
                </v-col>
              </v-row>
            </v-sheet>
          </section>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'

const props = defineProps({
  rating: { type: Number, default: 4.5 },
  verticals: { type: Array, default: () => [] },
  criterias: { type: Array, default: () => [] },
  total: { type: Number, default: 1.0 },
})

const display = useDisplay()

const localRating = ref(props.rating)

watch(
  () => props.rating,
  (value) => {
    localRating.value = typeof value === 'number' ? value : 0
  },
)

const sectionIds = {
  overview: 'impact-overview',
  ecoscore: 'impact-ecoscore',
  calculation: 'impact-calculation',
  criteria: 'impact-criteria',
  dataQuality: 'impact-data-quality',
} as const

const navigationSections = [
  { id: sectionIds.overview, label: 'Présentation', icon: 'mdi-information-outline' },
  { id: sectionIds.ecoscore, label: 'Notre démarche', icon: 'mdi-leaf' },
  { id: sectionIds.calculation, label: 'Calcul', icon: 'mdi-chart-timeline-variant' },
  { id: sectionIds.criteria, label: 'Critères', icon: 'mdi-table' },
  { id: sectionIds.dataQuality, label: 'Qualité des données', icon: 'mdi-shield-check-outline' },
]

const orientation = computed<'vertical' | 'horizontal'>(() => (display.mdAndDown.value ? 'horizontal' : 'vertical'))

const activeSection = ref<string>(sectionIds.overview)
const observer = ref<IntersectionObserver | null>(null)
const visibleSectionRatios = new Map<string, number>()
const MIN_SECTION_RATIO = 0.4

const refreshActiveSection = () => {
  if (!visibleSectionRatios.size) {
    activeSection.value = navigationSections[0]?.id ?? sectionIds.overview
    return
  }

  const sorted = [...visibleSectionRatios.entries()].sort((a, b) => b[1] - a[1])
  const [nextActive] = sorted.find(([, ratio]) => ratio >= MIN_SECTION_RATIO) ?? sorted[0] ?? []

  if (nextActive) {
    activeSection.value = nextActive
  }
}

const observeSections = () => {
  if (!import.meta.client) {
    return
  }

  observer.value?.disconnect()
  visibleSectionRatios.clear()
  refreshActiveSection()

  observer.value = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        const ratio = entry.intersectionRatio
        if (ratio > 0) {
          visibleSectionRatios.set(entry.target.id, ratio)
        } else {
          visibleSectionRatios.delete(entry.target.id)
        }
      })

      refreshActiveSection()
    },
    {
      rootMargin: '-15% 0px -35% 0px',
      threshold: Array.from({ length: 11 }, (_, index) => index / 10),
    },
  )

  nextTick(() => {
    navigationSections.forEach((section) => {
      const element = document.getElementById(section.id)
      if (element) {
        observer.value?.observe(element)
      }
    })
  })
}

onMounted(() => {
  observeSections()
})

onBeforeUnmount(() => {
  observer.value?.disconnect()
  visibleSectionRatios.clear()
})

watch(
  orientation,
  () => {
    nextTick(() => {
      observeSections()
    })
  },
)

const scrollToSection = (sectionId: string) => {
  if (!import.meta.client) {
    return
  }

  const element = document.getElementById(sectionId)
  if (!element) {
    return
  }

  activeSection.value = sectionId

  const offset = orientation.value === 'horizontal' ? 96 : 120
  const top = element.getBoundingClientRect().top + window.scrollY - offset
  window.scrollTo({ top, behavior: 'smooth' })
}

function formatCoeff(n: number | null | undefined) {
  if (n == null || Number.isNaN(Number(n))) return '—'
  return Number(n).toFixed(1)
}
</script>

<style scoped>
.impact-score-page {
  background: rgb(var(--v-theme-surface-muted));
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-page__hero {
  background: linear-gradient(140deg, rgba(var(--v-theme-hero-gradient-start), 0.85), rgba(var(--v-theme-hero-gradient-end), 0.85));
  color: white;
}

.impact-score-page__hero-row {
  gap: 2rem;
}

.impact-score-page__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-weight: 600;
  font-size: 0.78rem;
  margin-bottom: 0.75rem;
  color: rgba(255, 255, 255, 0.75);
}

.impact-score-page__title {
  font-size: clamp(2rem, 3vw, 2.75rem);
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 1.5rem;
}

.impact-score-page__intro :deep(p) {
  margin-bottom: 1rem;
}

.impact-score-page__hero-figure {
  display: flex;
  justify-content: center;
}

.impact-score-page__hero-figure .v-img {
  max-width: 380px;
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  background: rgba(255, 255, 255, 0.12);
  padding: 1.5rem;
}

.impact-score-page__content {
  margin-top: -64px;
}

.impact-score-page__layout {
  gap: 2rem;
}

.impact-score-page__nav-col {
  position: relative;
  z-index: 20;
}

.impact-score-page__main {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.impact-score-section__surface {
  padding: 2.5rem;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  background: rgb(var(--v-theme-surface-default));
}

.impact-score-section__surface--muted {
  background: rgba(var(--v-theme-surface-primary-080), 0.7);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.2);
}

.impact-score-section__header {
  margin-bottom: 1.5rem;
}

.impact-score-section__eyebrow {
  display: inline-block;
  text-transform: uppercase;
  font-size: 0.82rem;
  letter-spacing: 0.1em;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
  margin-bottom: 0.5rem;
}

.impact-score-section__title {
  margin: 0;
  font-weight: 700;
  font-size: clamp(1.4rem, 2.5vw, 1.9rem);
  line-height: 1.3;
}

.impact-score-section__row {
  gap: 1.5rem;
}

.impact-score-section__body :deep(p + p) {
  margin-top: 1rem;
}

.impact-score-section__figure {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--v-theme-surface-primary-100), 0.8);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.2);
}

.impact-score-section__figure--compact {
  max-width: 280px;
  margin-left: auto;
  margin-right: auto;
}

.impact-score-rating {
  margin-top: 1.5rem;
  padding: 1.5rem;
  border-radius: 18px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
  background: rgba(var(--v-theme-surface-primary-080), 0.8);
  display: grid;
  gap: 0.5rem;
  justify-items: flex-start;
}

.impact-score-rating__value {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.impact-score-rating__number {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-rating__suffix {
  font-size: 1rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-rating__caption {
  margin: 0;
  font-size: 0.85rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-section__list {
  margin: 1.5rem 0;
  padding: 0.75rem 0.5rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25);
}

.impact-score-section__list :deep(.v-list-item) {
  border-radius: 12px;
  transition: background 0.25s ease, transform 0.25s ease;
}

.impact-score-section__list :deep(.v-list-item:hover) {
  background: rgba(var(--v-theme-surface-primary-100), 0.8);
  transform: translateX(4px);
}

.impact-score-table {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
}

.impact-score-table thead {
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
}

.impact-score-table tfoot {
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
}

.impact-score-card {
  height: 100%;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  border-radius: 20px;
  box-shadow: none;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25);
  background: rgba(var(--v-theme-surface-primary-080), 0.65);
}

.impact-score-card__media {
  align-self: center;
  width: 140px;
}

.impact-score-card__media .v-img {
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-default), 0.85);
  padding: 1rem;
}

.impact-score-card__content :deep(p + p) {
  margin-top: 1rem;
}

@media (max-width: 960px) {
  .impact-score-page__content {
    margin-top: -32px;
  }

  .impact-score-section__surface {
    padding: 1.75rem;
  }

  .impact-score-section__figure {
    padding: 1rem;
  }

  .impact-score-card {
    padding: 1.25rem;
  }

  .impact-score-card__media {
    width: 120px;
  }
}

@media (max-width: 600px) {
  .impact-score-page__hero {
    border-bottom-left-radius: 28px;
    border-bottom-right-radius: 28px;
  }

  .impact-score-page__hero-figure .v-img {
    max-width: 280px;
    padding: 1rem;
  }

  .impact-score-section__surface {
    padding: 1.5rem;
  }

  .impact-score-section__title {
    font-size: 1.4rem;
  }

  .impact-score-section__list {
    padding: 0.5rem 0.25rem;
  }
}
</style>
