<template>
  <div class="impact-score-page">
    <ImpactScoreHero />

    <v-container class="impact-score-page__sections py-12 py-md-16">
      <ImpactScoreExamples class="impact-score-page__section" />
      <ImpactScoreMethodology
        class="impact-score-page__section"
        :verticals="verticals"
      />
      <ImpactScorePrinciples class="impact-score-page__section" />
      <ImpactScoreSummary class="impact-score-page__section" />
      <ImpactScoreDetails class="impact-score-page__section" />
    </v-container>
  </div>
</template>

<script setup lang="ts">
import type { VerticalConfigDto } from '~~/shared/api-client'
import ImpactScoreDetails from '~/components/impact-score/ImpactScoreDetails.vue'
import ImpactScoreExamples from '~/components/impact-score/ImpactScoreExamples.vue'
import ImpactScoreHero from '~/components/impact-score/ImpactScoreHero.vue'
import ImpactScoreMethodology from '~/components/impact-score/ImpactScoreMethodology.vue'
import ImpactScorePrinciples from '~/components/impact-score/ImpactScorePrinciples.vue'
import ImpactScoreSummary from '~/components/impact-score/ImpactScoreSummary.vue'

const { t } = useI18n()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

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
</style>
