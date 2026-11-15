<template>
  <div class="ranking-policy-page">
    <PageHeader
      variant="hero-standard"
      :eyebrow="t('rankingPolicy.hero.eyebrow')"
      :title="t('rankingPolicy.hero.title')"
      :subtitle="t('rankingPolicy.hero.subtitle')"
      layout="single-column"
      container="lg"
      heading-level="h1"
      :og-image="ogImageUrl"
    />

    <v-container class="ranking-policy-page__content" max-width="900">
      <p class="ranking-policy-page__intro">
        {{ t('rankingPolicy.intro') }}
      </p>
      <p
        class="ranking-policy-page__updated"
        data-testid="ranking-policy-updated"
      >
        <time :datetime="RANKING_POLICY_UPDATED_AT">
          {{ t('rankingPolicy.updatedAt', { date: formattedUpdatedAt }) }}
        </time>
      </p>

      <v-card
        class="ranking-policy-page__summary"
        variant="tonal"
        color="primary"
        rounded="lg"
      >
        <v-card-title class="ranking-policy-page__summary-title">
          <h2 class="text-h6">{{ t('rankingPolicy.summary.title') }}</h2>
        </v-card-title>
        <v-card-text>
          <ul class="ranking-policy-page__summary-list">
            <li v-for="key in SUMMARY_ITEM_KEYS" :key="key">
              {{ t(`rankingPolicy.summary.items.${key}`) }}
            </li>
          </ul>
        </v-card-text>
      </v-card>

      <section
        v-for="section in SECTIONS"
        :id="section.id"
        :key="section.id"
        class="ranking-policy-page__section"
        :aria-labelledby="`${section.id}-title`"
        :data-testid="`ranking-policy-section-${section.key}`"
      >
        <h2
          :id="`${section.id}-title`"
          class="ranking-policy-page__section-title"
        >
          {{ t(`rankingPolicy.sections.${section.key}.title`) }}
        </h2>
        <p
          v-for="paragraph in section.paragraphs"
          :key="paragraph"
          class="ranking-policy-page__paragraph"
        >
          {{ t(`rankingPolicy.sections.${section.key}.${paragraph}`) }}
        </p>
        <NuxtLink
          v-if="section.key === 'defaultSort'"
          class="ranking-policy-page__link"
          :to="impactScorePath"
        >
          {{ t('rankingPolicy.sections.defaultSort.methodLink') }}
        </NuxtLink>
      </section>

      <p class="ranking-policy-page__contact">
        {{ t('rankingPolicy.contact.text') }}
        <NuxtLink class="ranking-policy-page__link" :to="contactPath">
          {{ t('rankingPolicy.contact.link') }}
        </NuxtLink>
      </p>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import PageHeader from '~/components/shared/header/PageHeader.vue'
import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'

/**
 * "Classement et référencement" page required from a comparison site by
 * article D111-6 of the French consumer code. The text describes the ranking
 * code as it is: update RANKING_POLICY_UPDATED_AT and the i18n texts whenever a
 * default sort, an offer order or the merchant listing rules change (GOU-1995).
 */
definePageMeta({
  name: 'ranking-and-listing',
})

const RANKING_POLICY_UPDATED_AT = '2026-11-09'

const SUMMARY_ITEM_KEYS = [
  'defaultSort',
  'offers',
  'remuneration',
  'noPaidPlacement',
] as const

const SECTIONS = [
  {
    key: 'referencing',
    id: 'referencing',
    paragraphs: ['p1', 'p2', 'p3'],
  },
  {
    key: 'remuneration',
    id: 'remuneration',
    paragraphs: ['p1', 'p2', 'p3'],
  },
  {
    key: 'defaultSort',
    id: 'default-sort',
    paragraphs: ['p1', 'p2', 'p3'],
  },
  {
    key: 'otherSorts',
    id: 'other-sorts',
    paragraphs: ['p1', 'p2', 'p3'],
  },
  { key: 'search', id: 'search', paragraphs: ['p1', 'p2'] },
  { key: 'offers', id: 'offers', paragraphs: ['p1', 'p2'] },
  { key: 'alternatives', id: 'alternatives', paragraphs: ['p1', 'p2'] },
] as const

const { t, locale } = useI18n()
const requestURL = useRequestURL()

const currentLocale = computed(() => normalizeLocale(locale.value))

// The server renders in UTC: format the date in UTC to avoid hydration drift.
const formattedUpdatedAt = computed(() =>
  new Intl.DateTimeFormat(currentLocale.value, {
    dateStyle: 'long',
    timeZone: 'UTC',
  }).format(new Date(`${RANKING_POLICY_UPDATED_AT}T00:00:00Z`))
)

const impactScorePath = computed(() =>
  resolveLocalizedRoutePath('/impact-score', currentLocale.value)
)
const contactPath = computed(() =>
  resolveLocalizedRoutePath('contact', currentLocale.value)
)

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('ranking-and-listing', currentLocale.value),
    requestURL.origin
  ).toString()
)

const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(t('rankingPolicy.seo.title')),
  description: () => String(t('rankingPolicy.seo.description')),
  ogTitle: () => String(t('rankingPolicy.seo.title')),
  ogDescription: () => String(t('rankingPolicy.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
}))
</script>

<style scoped lang="sass">
.ranking-policy-page
  display: flex
  flex-direction: column

.ranking-policy-page__content
  padding-top: 32px
  padding-bottom: 56px

.ranking-policy-page__intro
  margin-bottom: 8px

.ranking-policy-page__updated
  margin-bottom: 24px
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.875rem

.ranking-policy-page__summary
  margin-bottom: 32px

.ranking-policy-page__summary-list
  padding-left: 1.25rem

  li + li
    margin-top: 6px

.ranking-policy-page__section
  margin-bottom: 32px

.ranking-policy-page__section-title
  margin-bottom: 12px
  font-size: 1.375rem

.ranking-policy-page__paragraph
  margin-bottom: 12px
  line-height: 1.6

.ranking-policy-page__link
  color: rgb(var(--v-theme-primary))
  font-weight: 600
</style>
