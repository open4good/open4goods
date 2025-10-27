<template>
  <div class="impact-score-page">
    <section class="impact-score-page__hero">
      <v-container class="py-12">
        <v-row class="impact-score-page__hero-row" align="center" justify="center">
          <v-col cols="12" md="10" lg="8" class="impact-score-page__hero-content">
            <p class="impact-score-page__eyebrow">Impact environnemental</p>
            <h1 class="impact-score-page__title">
              L’Impact Score : évaluation de l'impact environnemental de vos produits
            </h1>
            <div class="impact-score-page__intro text-body-1">
              <TextContent bloc-id="ECOSCORE:1:" />
            </div>
            <div class="impact-score-page__hero-actions">
              <v-btn
                class="impact-score-page__hero-button"
                color="surface"
                size="large"
                variant="elevated"
                prepend-icon="mdi-compass-outline"
                @click="scrollToSection(sectionIds.overview)"
              >
                Explorer la méthodologie
              </v-btn>
            </div>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <v-container class="impact-score-page__content py-12">
      <div class="impact-score-page__layout">
        <aside
          class="impact-score-page__nav"
          :class="{ 'impact-score-page__nav--mobile': orientation === 'horizontal' }"
          aria-label="Plan de page Impact Score"
        >
          <StickySectionNavigation
            :sections="navigationSections"
            :active-section="activeSection"
            :orientation="orientation"
            aria-label="Navigation Impact Score"
            @navigate="scrollToSection"
          />
        </aside>

        <main class="impact-score-page__sections" role="main">
          <section :id="sectionIds.overview" class="impact-score-section impact-score-section--intro">
            <v-sheet class="impact-score-section__surface" rounded="xl" elevation="0">
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">Présentation</span>
                <h2 class="impact-score-section__title">Qu’est-ce que l’Impact Score&nbsp;?</h2>
              </header>

              <v-row class="impact-score-section__row impact-score-section__row--balanced" align="stretch">
                <v-col cols="12" md="7">
                  <div class="impact-score-section__body">
                    <TextContent bloc-id="ECOSCORE:2:" />
                  </div>
                </v-col>

                <v-col cols="12" md="5" class="impact-score-section__rating">
                  <v-card variant="flat" class="impact-score-example" elevation="0">
                    <div class="impact-score-example__header">
                      <span class="impact-score-example__eyebrow">Évaluation indicative</span>
                      <strong class="impact-score-example__value">{{ formatCoeff(localRating) }}</strong>
                    </div>
                    <ImpactScore
                      :score="localRating"
                      :max="5"
                      size="large"
                      show-value
                    />
                    <p class="impact-score-example__caption">
                      Évaluation indicative basée sur notre méthodologie
                    </p>
                  </v-card>
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
                <v-col cols="12" md="5">
                  <div class="impact-score-section__figure">
                    <v-img
                      src="https://nudger.fr/img/impactscore-illustration.png"
                      alt="Illustration de notre démarche ecoscore"
                      aspect-ratio="1"
                      contain
                    />
                  </div>
                </v-col>
                <v-col cols="12" md="7">
                  <div class="impact-score-section__body">
                    <TextContent bloc-id="ECOSCORE:3:" />
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

              <div class="impact-score-insights">
                <article class="impact-score-insight impact-score-insight--reverse">
                  <div class="impact-score-insight__content">
                    <h3 class="impact-score-insight__title">Principe de relativisation</h3>
                    <TextContent bloc-id="ECOSCORE:4-1:" />
                  </div>
                  <div class="impact-score-insight__media">
                    <v-img
                      src="https://nudger.fr/img/relativisation.png"
                      alt="Illustration du principe de relativisation"
                      aspect-ratio="1"
                      contain
                    />
                  </div>
                </article>

                <article class="impact-score-insight">
                  <div class="impact-score-insight__media">
                    <v-img
                      src="https://nudger.fr/img/data-quality.png"
                      alt="Illustration sur la qualité de la donnée"
                      aspect-ratio="1"
                      contain
                    />
                  </div>
                  <div class="impact-score-insight__content">
                    <h3 class="impact-score-insight__title">La qualité de la donnée, enjeu majeur</h3>
                    <TextContent bloc-id="ECOSCORE:4-2:" />
                  </div>
                </article>
              </div>
            </v-sheet>
          </section>
        </main>
      </div>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
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
  background: linear-gradient(140deg, rgba(var(--v-theme-hero-gradient-start), 0.88), rgba(var(--v-theme-hero-gradient-end), 0.88));
  color: white;
  position: relative;
  overflow: hidden;
}

.impact-score-page__hero::after {
  content: '';
  position: absolute;
  inset: 10% -20% -40% -20%;
  background: radial-gradient(circle at top right, rgba(255, 255, 255, 0.25), transparent 65%);
  pointer-events: none;
}

.impact-score-page__hero-row {
  gap: 2.5rem;
  position: relative;
  z-index: 1;
}

.impact-score-page__hero-content {
  text-align: center;
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

.impact-score-page__hero-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: center;
}

.impact-score-page__hero-button {
  font-weight: 600;
  border-radius: 999px;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.25);
}

.impact-score-page__hero-button :deep(.v-icon) {
  color: rgb(var(--v-theme-accent-supporting));
}

.impact-score-page__content {
  margin-top: -64px;
}

.impact-score-page__layout {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
  gap: 2.5rem;
}

.impact-score-page__nav {
  position: sticky;
  top: 104px;
  align-self: start;
  height: fit-content;
  z-index: 10;
}

.impact-score-page__nav--mobile {
  position: static;
  top: auto;
  margin-bottom: 1.5rem;
}

.impact-score-page__sections {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.impact-score-section {
  scroll-margin-top: 120px;
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

.impact-score-section__row--balanced {
  align-items: stretch;
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

.impact-score-section__rating {
  display: flex;
  align-items: center;
}

.impact-score-example {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.75rem;
  border-radius: 20px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
  background: rgba(var(--v-theme-surface-primary-080), 0.8);
}

.impact-score-example__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.impact-score-example__eyebrow {
  text-transform: uppercase;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-example__value {
  font-size: 2.75rem;
  font-weight: 700;
  line-height: 1;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-example__caption {
  margin: 0;
  font-size: 0.88rem;
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

.impact-score-insights {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.impact-score-insight {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(200px, 280px);
  gap: 1.5rem;
  align-items: center;
}

.impact-score-insight--reverse {
  grid-template-columns: minmax(0, 1fr) minmax(200px, 280px);
}

.impact-score-insight--reverse .impact-score-insight__content {
  order: 1;
}

.impact-score-insight--reverse .impact-score-insight__media {
  order: 2;
}

.impact-score-insight__title {
  margin-bottom: 1rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-insight__media {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--v-theme-surface-primary-100), 0.8);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.2);
}

.impact-score-insight__media .v-img {
  border-radius: 16px;
}

.impact-score-insight__content :deep(p + p) {
  margin-top: 1rem;
}

@media (max-width: 1280px) {
  .impact-score-page__layout {
    grid-template-columns: 1fr;
  }

  .impact-score-page__nav {
    position: sticky;
    top: 0;
    z-index: 20;
  }

  .impact-score-page__nav--mobile {
    position: static;
    top: auto;
  }

  .impact-score-section {
    scroll-margin-top: 160px;
  }
}

@media (max-width: 960px) {
  .impact-score-page__content {
    margin-top: -32px;
  }

  .impact-score-page__hero-content {
    text-align: left;
  }

  .impact-score-page__hero-actions {
    justify-content: flex-start;
  }

  .impact-score-section__surface {
    padding: 1.9rem;
  }

  .impact-score-section__figure,
  .impact-score-insight__media {
    padding: 1.25rem;
  }

  .impact-score-insight {
    grid-template-columns: 1fr;
  }

  .impact-score-insight--reverse .impact-score-insight__content,
  .impact-score-insight--reverse .impact-score-insight__media {
    order: initial;
  }
}

@media (max-width: 600px) {
  .impact-score-page__hero {
    border-bottom-left-radius: 28px;
    border-bottom-right-radius: 28px;
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

  .impact-score-example {
    padding: 1.5rem;
  }
}
</style>
