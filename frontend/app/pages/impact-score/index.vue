<template>
  <div class="impact-score-page">
    <PageHeader
      variant="section-header"
      :title="t('impactScorePage.hero.title')"
      background="image"
      background-image-asset-key="impactScoreBackground"
      surface-variant="mesh"
      layout="single-column"
      container="xl"
      content-align="center"
      heading-level="h1"
      schema-type="WebPage"
      padding-y="clamp(5rem, 9vw, 7.5rem)"
      heading-id="impact-score-hero-title"
      aria-described-by="impact-score-hero-intro"
    />

    <v-container class="impact-score-page__content py-12">
      <div class="impact-score-page__layout">
        <aside
          class="impact-score-page__nav"
          :class="{
            'impact-score-page__nav--mobile': orientation === 'horizontal',
          }"
          :aria-label="t('impactScorePage.aria.pageOutline')"
        >
          <StickySectionNavigation
            :sections="navigationSections"
            :active-section="activeSection"
            :orientation="orientation"
            :aria-label="t('impactScorePage.aria.navigation')"
            @navigate="scrollToSection"
          />
        </aside>

        <main class="impact-score-page__sections" role="main">
          <section
            :id="sectionIds.overview"
            class="impact-score-section impact-score-section--intro"
            :aria-labelledby="`${sectionIds.overview}-title`"
            role="region"
          >
            <v-sheet
              class="impact-score-section__surface"
              rounded="xl"
              elevation="0"
            >
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">
                  {{ t('impactScorePage.sections.overview.eyebrow') }}
                </span>
                <h2
                  :id="`${sectionIds.overview}-title`"
                  class="impact-score-section__title"
                >
                  {{ t('impactScorePage.sections.overview.title') }}
                </h2>
              </header>

              <v-row
                class="impact-score-section__row impact-score-section__row--balanced"
                align="stretch"
              >
                <v-col cols="12" sm="7" md="6" lg="7">
                  <div class="impact-score-section__body">
                    <TextContent bloc-id="ECOSCORE:2:" />
                  </div>
                </v-col>

                <v-col
                  cols="12"
                  sm="5"
                  md="6"
                  lg="5"
                  class="impact-score-section__rating"
                >
                  <v-card
                    variant="flat"
                    class="impact-score-example"
                    elevation="0"
                    role="group"
                    :aria-labelledby="`${sectionIds.overview}-rating-title`"
                    :aria-describedby="`${sectionIds.overview}-rating-caption`"
                  >
                    <div class="impact-score-example__header">
                      <span
                        :id="`${sectionIds.overview}-rating-title`"
                        class="impact-score-example__eyebrow"
                      >
                        {{
                          t('impactScorePage.sections.overview.sample.title')
                        }}
                      </span>
                      <strong class="impact-score-example__value">{{
                        formatCoeff(localRating)
                      }}</strong>
                    </div>
                    <ImpactScore
                      :score="localRating"
                      :max="5"
                      size="large"
                      show-value
                    />
                    <p
                      :id="`${sectionIds.overview}-rating-caption`"
                      class="impact-score-example__caption"
                    >
                      {{
                        t('impactScorePage.sections.overview.sample.caption')
                      }}
                    </p>
                  </v-card>
                </v-col>
              </v-row>
            </v-sheet>
          </section>

          <section
            :id="sectionIds.ecoscore"
            class="impact-score-section"
            :aria-labelledby="`${sectionIds.ecoscore}-title`"
            role="region"
          >
            <v-sheet
              class="impact-score-section__surface impact-score-section__surface--muted"
              rounded="xl"
              elevation="0"
            >
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">
                  {{ t('impactScorePage.sections.approach.eyebrow') }}
                </span>
                <h2
                  :id="`${sectionIds.ecoscore}-title`"
                  class="impact-score-section__title"
                >
                  {{ t('impactScorePage.sections.approach.title') }}
                </h2>
              </header>

              <v-row
                class="impact-score-section__row impact-score-section__row--media"
                align="stretch"
              >
                <v-col
                  cols="12"
                  md="5"
                  lg="4"
                  class="impact-score-section__media"
                >
                  <figure class="impact-score-media" role="presentation">
                    <v-img
                      src="https://nudger.fr/img/impactscore-illustration.webp"
                      :alt="t('impactScorePage.sections.approach.imageAlt')"
                      class="impact-score-media__image"
                      cover
                    />
                  </figure>
                </v-col>
                <v-col cols="12" md="7" lg="8">
                  <div class="impact-score-section__body">
                    <TextContent bloc-id="ECOSCORE:3:" />
                  </div>
                </v-col>
              </v-row>
            </v-sheet>
          </section>

          <section
            :id="sectionIds.calculation"
            class="impact-score-section"
            :aria-labelledby="`${sectionIds.calculation}-title`"
            role="region"
          >
            <v-sheet
              class="impact-score-section__surface"
              rounded="xl"
              elevation="0"
            >
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">
                  {{ t('impactScorePage.sections.methodology.eyebrow') }}
                </span>
                <h2
                  :id="`${sectionIds.calculation}-title`"
                  class="impact-score-section__title"
                >
                  {{ t('impactScorePage.sections.methodology.title') }}
                </h2>
              </header>

              <div class="impact-score-section__body">
                <p>{{ t('impactScorePage.sections.methodology.intro') }}</p>
              </div>

              <ResponsiveCarousel
                v-if="verticalCards.length"
                class="impact-score-verticals"
                :items="verticalCards"
                :breakpoints="{ xs: 1, sm: 1, md: 2, lg: 3 }"
                :aria-label="
                  t('impactScorePage.sections.methodology.verticalCarouselAria')
                "
              >
                <template #item="{ item }">
                  <NuxtLink
                    :to="item.href"
                    class="impact-score-vertical-card"
                    :aria-label="
                      t(
                        'impactScorePage.sections.methodology.verticalCardAria',
                        { vertical: item.displayName }
                      )
                    "
                  >
                    <v-img
                      v-if="item.image"
                      :src="item.image"
                      :alt="
                        t(
                          'impactScorePage.sections.methodology.verticalImageAlt',
                          { vertical: item.displayName }
                        )
                      "
                      class="impact-score-vertical-card__image"
                      cover
                    />
                    <div class="impact-score-vertical-card__content">
                      <span class="impact-score-vertical-card__eyebrow">
                        {{
                          t(
                            'impactScorePage.sections.methodology.verticalLabel'
                          )
                        }}
                      </span>
                      <strong class="impact-score-vertical-card__title">{{
                        item.displayName
                      }}</strong>
                      <span class="impact-score-vertical-card__cta">
                        {{
                          t('impactScorePage.sections.methodology.verticalCta')
                        }}
                      </span>
                    </div>
                  </NuxtLink>
                </template>
              </ResponsiveCarousel>
              <v-alert
                v-else
                type="info"
                variant="tonal"
                class="impact-score-verticals__empty"
                border="start"
                density="comfortable"
                :aria-label="t('impactScorePage.sections.methodology.empty')"
              >
                {{ t('impactScorePage.sections.methodology.empty') }}
              </v-alert>

              <div class="impact-score-section__body">
                <TextContent bloc-id="ECOSCORE:4:" />
              </div>
            </v-sheet>
          </section>

          <section
            :id="sectionIds.dataQuality"
            class="impact-score-section"
            :aria-labelledby="`${sectionIds.dataQuality}-title`"
            role="region"
          >
            <v-sheet
              class="impact-score-section__surface impact-score-section__surface--muted"
              rounded="xl"
              elevation="0"
            >
              <header class="impact-score-section__header">
                <span class="impact-score-section__eyebrow">
                  {{ t('impactScorePage.sections.analysis.eyebrow') }}
                </span>
                <h2
                  :id="`${sectionIds.dataQuality}-title`"
                  class="impact-score-section__title"
                >
                  {{ t('impactScorePage.sections.analysis.title') }}
                </h2>
              </header>

              <div class="impact-score-insights">
                <article
                  class="impact-score-insight impact-score-insight--reverse"
                >
                  <div class="impact-score-insight__content">
                    <h3 class="impact-score-insight__title">
                      {{
                        t(
                          'impactScorePage.sections.analysis.relativisation.title'
                        )
                      }}
                    </h3>
                    <TextContent bloc-id="ECOSCORE:4-1:" />
                  </div>
                  <div class="impact-score-insight__media">
                    <v-img
                      src="https://nudger.fr/img/relativisation.webp"
                      :alt="
                        t(
                          'impactScorePage.sections.analysis.relativisation.imageAlt'
                        )
                      "
                      class="impact-score-media__image"
                      cover
                    />
                  </div>
                </article>

                <article class="impact-score-insight">
                  <div class="impact-score-insight__media">
                    <v-img
                      src="https://nudger.fr/img/data-quality.webp"
                      :alt="
                        t(
                          'impactScorePage.sections.analysis.dataQuality.imageAlt'
                        )
                      "
                      class="impact-score-media__image"
                      cover
                    />
                  </div>
                  <div class="impact-score-insight__content">
                    <h3 class="impact-score-insight__title">
                      {{
                        t('impactScorePage.sections.analysis.dataQuality.title')
                      }}
                    </h3>
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
import { useI18n } from 'vue-i18n'
import type { VerticalConfigDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ResponsiveCarousel from '~/components/shared/ui/ResponsiveCarousel.vue'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'
import PageHeader from '~/components/shared/header/PageHeader.vue'

const props = defineProps({
  rating: { type: Number, default: 4.5 },
})

const { t, locale } = useI18n()
const display = useDisplay()

const { data: fetchedVerticals } = await useAsyncData<VerticalConfigDto[]>(
  'impact-score-verticals',
  () =>
    $fetch<VerticalConfigDto[]>('/api/categories', {
      params: { onlyEnabled: true },
    }),
  {
    default: () => [],
    server: true,
  }
)

type VerticalCard = {
  id: string
  href: string
  image: string | null
  displayName: string
}

const normalizedLocale = computed(() => locale.value ?? 'fr-FR')

const verticalCards = computed<VerticalCard[]>(() => {
  const data = fetchedVerticals.value ?? []

  return data
    .filter(vertical =>
      Boolean(vertical?.verticalHomeTitle && vertical?.verticalHomeUrl)
    )
    .map(vertical => {
      const rawUrl = (vertical.verticalHomeUrl ?? '').trim()
      const trimmedBase = rawUrl.replace(/\/+$/, '')
      if (!trimmedBase.length) {
        return null
      }

      const rawTitle = (vertical.verticalHomeTitle ?? '').trim()
      if (!rawTitle.length) {
        return null
      }

      const hrefBase = trimmedBase.startsWith('/')
        ? trimmedBase
        : `/${trimmedBase}`
      const displayName = rawTitle.toLocaleLowerCase(normalizedLocale.value)
      const href = `${hrefBase}/ecoscore`
      const image = vertical.imageSmall ?? null

      return {
        id: vertical.id ?? href,
        href,
        image,
        displayName,
      }
    })
    .filter((card): card is VerticalCard => Boolean(card))
})

const localRating = ref(props.rating)

watch(
  () => props.rating,
  value => {
    localRating.value = typeof value === 'number' ? value : 0
  }
)

const sectionIds = {
  overview: 'impact-overview',
  ecoscore: 'impact-ecoscore',
  calculation: 'impact-calculation',
  dataQuality: 'impact-data-quality',
} as const

const navigationSections = computed(() => [
  {
    id: sectionIds.overview,
    label: t('impactScorePage.navigation.overview'),
    icon: 'mdi-information-outline',
  },
  {
    id: sectionIds.ecoscore,
    label: t('impactScorePage.navigation.approach'),
    icon: 'mdi-leaf',
  },
  {
    id: sectionIds.calculation,
    label: t('impactScorePage.navigation.methodology'),
    icon: 'mdi-chart-timeline-variant',
  },
  {
    id: sectionIds.dataQuality,
    label: t('impactScorePage.navigation.analysis'),
    icon: 'mdi-shield-check-outline',
  },
])

const orientation = computed<'vertical' | 'horizontal'>(() =>
  display.mdAndDown.value ? 'horizontal' : 'vertical'
)

const activeSection = ref<string>(sectionIds.overview)
const observer = ref<IntersectionObserver | null>(null)
const visibleSectionRatios = new Map<string, number>()
const MIN_SECTION_RATIO = 0.4

const refreshActiveSection = () => {
  const sections = navigationSections.value
  if (!visibleSectionRatios.size) {
    activeSection.value = sections[0]?.id ?? sectionIds.overview
    return
  }

  const sorted = [...visibleSectionRatios.entries()].sort((a, b) => b[1] - a[1])
  const [nextActive] =
    sorted.find(([, ratio]) => ratio >= MIN_SECTION_RATIO) ?? sorted[0] ?? []

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
    entries => {
      entries.forEach(entry => {
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
    }
  )

  nextTick(() => {
    navigationSections.value.forEach(section => {
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

watch(orientation, () => {
  nextTick(() => {
    observeSections()
  })
})

watch(
  navigationSections,
  () => {
    nextTick(() => {
      observeSections()
    })
  },
  { deep: true }
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
  background: linear-gradient(
    180deg,
    rgba(var(--v-theme-hero-gradient-start), 0.95) 0%,
    rgba(var(--v-theme-hero-gradient-end), 0.92) 48%,
    rgb(var(--v-theme-surface-muted)) 48%
  );
  color: rgb(var(--v-theme-text-neutral-strong));
  min-height: 100%;
}

.impact-score-page__content {
  margin-top: 0;
  padding-top: clamp(2.75rem, 4vw, 4.5rem);
}

.impact-score-page__layout {
  display: grid;
  grid-template-columns: minmax(200px, min(22vw, 280px)) minmax(0, 1fr);
  gap: clamp(2.25rem, 3.5vw, 3.5rem);
  align-items: start;
}

.impact-score-page__nav {
  position: sticky;
  top: 96px;
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
  padding: clamp(1.8rem, 2vw + 1.5rem, 2.6rem);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  background: rgb(var(--v-theme-surface-default));
  border-radius: 28px;
}

.impact-score-section__surface--muted {
  background: rgba(var(--v-theme-surface-primary-080), 0.7);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.24);
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
  font-size: clamp(1.45rem, 2.4vw, 2rem);
  line-height: 1.3;
}

.impact-score-section__row {
  gap: 1.75rem;
}

.impact-score-section__row--balanced {
  align-items: stretch;
}

.impact-score-section__row--media {
  align-items: flex-start;
}

.impact-score-section__body :deep(p + p) {
  margin-top: 1rem;
}

.impact-score-section__media {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.impact-score-media,
.impact-score-insight__media {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: rgba(var(--v-theme-surface-primary-100), 0.85);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.22);
  width: 100%;
  max-width: 320px;
  margin-inline: auto;
}

.impact-score-section__body {
  width: 100%;
}

.impact-score-media__image {
  width: 100%;
  max-width: 260px;
  aspect-ratio: 1;
  border-radius: 18px;
}

.impact-score-section__rating {
  display: flex;
  align-items: stretch;
  justify-content: center;
}

.impact-score-example {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: clamp(1.6rem, 2vw + 1rem, 2.2rem);
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  border-radius: 24px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-surface-primary-100), 0.35);
}

.impact-score-example__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
}

.impact-score-example__eyebrow {
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-example__value {
  font-size: clamp(2.1rem, 2vw + 1.8rem, 2.75rem);
  font-weight: 700;
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.impact-score-example__caption {
  margin: 0;
  font-size: 0.92rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-verticals {
  margin: 1.75rem 0 0;
}

.impact-score-verticals :deep(.responsive-carousel__container) {
  box-shadow: none;
}

.impact-score-verticals :deep(.responsive-carousel__slide) {
  padding-block: 0.25rem 0.5rem;
}

.impact-score-verticals :deep(.responsive-carousel__item) {
  display: flex;
}

.impact-score-vertical-card {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  height: 100%;
  padding: 1.75rem;
  border-radius: 24px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.28);
  background: rgba(var(--v-theme-surface-glass), 0.9);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.06);
  text-decoration: none;
  color: inherit;
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.impact-score-vertical-card:focus-visible,
.impact-score-vertical-card:hover {
  transform: translateY(-4px);
  border-color: rgba(var(--v-theme-accent-primary-highlight), 0.55);
  box-shadow: 0 22px 40px rgba(15, 23, 42, 0.12);
}

.impact-score-vertical-card__image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-primary-100), 0.6);
}

.impact-score-vertical-card__content {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.impact-score-vertical-card__eyebrow {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-vertical-card__title {
  font-size: 1.15rem;
  font-weight: 700;
  text-transform: capitalize;
}

.impact-score-vertical-card__cta {
  font-size: 0.9rem;
  font-weight: 600;
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.impact-score-verticals__empty {
  margin-top: 1.5rem;
}

.impact-score-insights {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.impact-score-insight {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(200px, 300px);
  gap: 1.75rem;
  align-items: center;
}

.impact-score-insight--reverse {
  grid-template-columns: minmax(0, 1fr) minmax(200px, 300px);
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

.impact-score-insight__content :deep(p + p) {
  margin-top: 1rem;
}

@media (max-width: 1200px) {
  .impact-score-page__content {
    padding-top: 3rem;
  }

  .impact-score-page__layout {
    grid-template-columns: 1fr;
  }

  .impact-score-page__nav {
    position: sticky;
    top: 0;
    z-index: 20;
  }

  .impact-score-section {
    scroll-margin-top: 160px;
  }
}

@media (max-width: 960px) {
  .impact-score-page__content {
    padding-top: 2rem;
  }

  .impact-score-section__surface {
    padding: 1.8rem;
    border-radius: 24px;
  }

  .impact-score-section__row {
    gap: 1.5rem;
  }

  .impact-score-section__media {
    margin-bottom: 0.5rem;
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
  .impact-score-section__surface {
    padding: 1.5rem;
    border-radius: 20px;
  }

  .impact-score-section__title {
    font-size: 1.4rem;
  }

  .impact-score-example {
    padding: 1.6rem;
  }

  .impact-score-vertical-card {
    padding: 1.5rem;
  }
}
</style>
