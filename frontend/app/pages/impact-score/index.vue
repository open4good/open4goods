<template>
  <div class="impact-score-page">
    <ImpactScoreHero />

    <v-container class="impact-score-page__sections py-12 py-md-16">
      <section
        id="what"
        class="impact-score-page__section impact-score-page__block"
        aria-labelledby="impact-score-what-title"
      >
        <h2 id="impact-score-what-title" class="text-h4 font-weight-bold">
          {{ t('impactScorePage.what.title') }}
        </h2>
        <!-- eslint-disable vue/no-v-html -->
        <p class="text-body-1" v-html="t('impactScorePage.what.scope')"></p>
        <!-- eslint-enable vue/no-v-html -->
        <p class="text-body-1">{{ t('impactScorePage.what.criteria') }}</p>
        <p class="text-body-1">{{ t('impactScorePage.what.data') }}</p>
        <p class="text-body-1">{{ t('impactScorePage.what.rating') }}</p>
      </section>

      <ImpactScoreMethodology
        class="impact-score-page__section"
        :verticals="verticals"
      />
      <ImpactScoreAvailableCriteria class="impact-score-page__section" />

      <section
        id="not"
        class="impact-score-page__section impact-score-page__block"
        aria-labelledby="impact-score-not-title"
      >
        <h2 id="impact-score-not-title" class="text-h4 font-weight-bold">
          {{ t('impactScorePage.not.title') }}
        </h2>
        <ul class="impact-score-page__list text-body-1">
          <!-- eslint-disable vue/no-v-html -->
          <li
            v-for="key in NOT_ITEM_KEYS"
            :key="key"
            v-html="t(`impactScorePage.not.items.${key}`)"
          ></li>
          <!-- eslint-enable vue/no-v-html -->
        </ul>
        <p class="text-body-2 text-medium-emphasis">
          {{ t('components.impactScore.v2.disclaimer') }}
        </p>
      </section>

      <section
        id="read"
        class="impact-score-page__section impact-score-page__block"
        aria-labelledby="impact-score-read-title"
      >
        <h2 id="impact-score-read-title" class="text-h4 font-weight-bold">
          {{ t('impactScorePage.read.title') }}
        </h2>
        <p class="text-body-1">{{ t('impactScorePage.read.intro') }}</p>
        <figure class="impact-score-page__example">
          <ImpactScore
            :score="DEMO_SCORE"
            :v2="demoStatus"
            :vertical-plural-name="t('impactScorePage.read.exampleCategory')"
            size="lg"
            :show-range="false"
            :show-methodology="false"
          />
          <figcaption class="text-caption text-medium-emphasis">
            {{ t('impactScorePage.read.exampleCaption') }}
          </figcaption>
        </figure>
        <ul class="impact-score-page__list text-body-1">
          <!-- eslint-disable vue/no-v-html -->
          <li
            v-for="key in READ_ITEM_KEYS"
            :key="key"
            v-html="t(`impactScorePage.read.items.${key}`)"
          ></li>
          <!-- eslint-enable vue/no-v-html -->
        </ul>
      </section>

      <section
        id="limits"
        class="impact-score-page__section impact-score-page__block"
        aria-labelledby="impact-score-limits-title"
      >
        <h2 id="impact-score-limits-title" class="text-h4 font-weight-bold">
          {{ t('impactScorePage.limits.title') }}
        </h2>
        <ul class="impact-score-page__list text-body-1">
          <!-- eslint-disable vue/no-v-html -->
          <li
            v-for="key in LIMIT_ITEM_KEYS"
            :key="key"
            v-html="t(`impactScorePage.limits.items.${key}`)"
          ></li>
          <!-- eslint-enable vue/no-v-html -->
        </ul>
        <p class="text-body-1">
          {{ t('impactScorePage.limits.error') }}
          <NuxtLink :to="contactPath">
            {{ t('impactScorePage.limits.errorLink') }}
          </NuxtLink>
        </p>
      </section>

      <section
        id="history"
        class="impact-score-page__section impact-score-page__block"
        aria-labelledby="impact-score-history-title"
      >
        <h2 id="impact-score-history-title" class="text-h4 font-weight-bold">
          {{ t('impactScorePage.history.title') }}
        </h2>
        <p class="text-body-1">{{ t('impactScorePage.history.intro') }}</p>
        <p class="text-body-1">
          <NuxtLink :to="versionsPath">
            {{ t('impactScorePage.history.link') }}
          </NuxtLink>
        </p>
        <!-- eslint-disable vue/no-v-html -->
        <p class="text-body-1" v-html="t('impactScorePage.history.why')"></p>
        <!-- eslint-enable vue/no-v-html -->
      </section>

      <ImpactScoreDocsPanel class="impact-score-page__section" />

    </v-container>
  </div>
</template>

<script setup lang="ts">
import type {
  ProductImpactScoreStatusDto,
  VerticalConfigDto,
} from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ImpactScoreAvailableCriteria from '~/components/impact-score/ImpactScoreAvailableCriteria.vue'
import ImpactScoreHero from '~/components/impact-score/ImpactScoreHero.vue'
import ImpactScoreMethodology from '~/components/impact-score/ImpactScoreMethodology.vue'
import ImpactScoreDocsPanel from '~/components/impact-score/ImpactScoreDocsPanel.vue'

import {
  IMPACT_SCORE_CURRENT_METHOD_VERSION,
  IMPACT_SCORE_VERSIONS_LAST_MODIFIED,
} from '~~/shared/utils/impact-score-versions'
import { buildImpactScoreMethodJsonLd } from '~/utils/impact-score-jsonld'

const NOT_ITEM_KEYS = ['lca', 'footprint', 'label', 'perfect', 'advice']
const READ_ITEM_KEYS = ['score', 'position', 'criteria', 'version', 'notRated']
const LIMIT_ITEM_KEYS = [
  'declared',
  'manufacturing',
  'cohort',
  'catalog',
  'quality',
]

/** Illustrative values for the "how to read" block: not a real product. */
const DEMO_SCORE = 14.2
const demoStatus: ProductImpactScoreStatusDto = {
  status: 'RATED',
  methodologyVersion: 2,
  currentMethodologyVersion: 2,
  scoredAt: Date.UTC(2026, 10, 9),
  realCriteriaCount: 11,
  totalCriteriaCount: 13,
  ratedCount: 120,
  betterThanPercent: 72,
  level: 'good',
}

const { t, locale } = useI18n()
const canonicalUrl = useCanonicalUrl()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const contactPath = computed(() =>
  resolveLocalizedRoutePath('contact', locale.value)
)
const localePath = useLocalePath()
const versionsPath = computed(() => localePath('/impact-score/versions'))

const { data: verticalsData } = await useAsyncData<VerticalConfigDto[]>(
  'impact-score-verticals',
  () =>
    $fetch<VerticalConfigDto[]>('/api/categories', {
      headers: requestHeaders,
      params: { onlyEnabled: true },
    })
)

const verticals = computed(() => verticalsData.value ?? [])

useSeoMeta({
  title: t('impactScorePage.seo.title'),
  description: t('impactScorePage.seo.description'),
  ogTitle: t('impactScorePage.seo.title'),
  ogDescription: t('impactScorePage.seo.description'),
})

useHead(() => ({
  script: [
    {
      key: 'impact-score-jsonld',
      type: 'application/ld+json',
      innerHTML: JSON.stringify(
        buildImpactScoreMethodJsonLd({
          type: 'TechArticle',
          name: String(t('impactScorePage.seo.title')),
          description: String(t('impactScorePage.seo.description')),
          url: canonicalUrl.value,
          dateModified: IMPACT_SCORE_VERSIONS_LAST_MODIFIED,
          version: IMPACT_SCORE_CURRENT_METHOD_VERSION,
          inLanguage: locale.value,
          aboutName: 'Impact Score',
        })
      ),
    },
  ],
}))
</script>

<style scoped>
.impact-score-page {
  background: linear-gradient(
    120deg,
    rgba(var(--v-theme-hero-gradient-end), 0.85) 0%,
    rgba(var(--v-theme-hero-gradient-start), 0.9) 55%,
    rgba(var(--v-theme-hero-gradient-mid), 0.85) 100%
  );
}

.impact-score-page__sections {
  display: grid;
  gap: clamp(2.5rem, 4vw, 3.5rem);
}

.impact-score-page__section {
  padding: clamp(1.8rem, 2vw + 1rem, 2.4rem);
  background: rgb(var(--v-theme-surface-default));
  border-radius: 26px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.24);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}

.impact-score-page__block {
  display: grid;
  gap: 1rem;
}

.impact-score-page__list {
  display: grid;
  gap: 0.75rem;
  padding-left: 1.25rem;
}

.impact-score-page__example {
  margin: 0;
  display: grid;
  gap: 0.5rem;
  justify-items: start;
}
</style>
