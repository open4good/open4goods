<template>
  <div class="impact-score-versions">
    <v-container class="py-12 py-md-16">
      <nav class="mb-4">
        <NuxtLink :to="localePath('/impact-score')">
          {{ t('impactScoreVersionsPage.backLink') }}
        </NuxtLink>
      </nav>
      <h1 class="text-h3 font-weight-bold mb-4">
        {{ t('impactScoreVersionsPage.title') }}
      </h1>
      <p class="text-body-1 mb-8">{{ t('impactScoreVersionsPage.intro') }}</p>

      <ol class="impact-score-versions__list">
        <li
          v-for="entry in IMPACT_SCORE_VERSIONS"
          :id="entry.id"
          :key="entry.id"
          class="impact-score-versions__entry"
        >
          <h2 class="text-h5 font-weight-bold mb-1">
            {{ t(`impactScoreVersionsPage.entries.${entry.id}.title`) }}
          </h2>
          <p class="text-body-2 text-medium-emphasis mb-1">
            <time v-if="entry.date" :datetime="entry.date">
              {{ formatDate(entry.date) }}
            </time>
            <span v-else>{{ t('impactScoreVersionsPage.noDate') }}</span>
            <span v-if="entry.status === 'planned'">
              · {{ t('impactScoreVersionsPage.planned') }}
            </span>
          </p>
          <p class="text-body-2 mb-3">
            <strong>{{ t('impactScoreVersionsPage.scopeLabel') }}</strong>
            {{ t(`impactScoreVersionsPage.entries.${entry.id}.scope`) }}
          </p>
          <ul class="impact-score-versions__changes">
            <li
              v-for="index in entry.changeCount"
              :key="`${entry.id}-${index}`"
              class="mb-2"
            >
              <strong>{{ t('impactScoreVersionsPage.changeLabel') }}</strong>
              {{
                t(
                  `impactScoreVersionsPage.entries.${entry.id}.changes.${index - 1}.change`
                )
              }}
              <br />
              <strong>{{ t('impactScoreVersionsPage.reasonLabel') }}</strong>
              {{
                t(
                  `impactScoreVersionsPage.entries.${entry.id}.changes.${index - 1}.reason`
                )
              }}
            </li>
          </ul>
        </li>
      </ol>

      <h2 class="text-h5 font-weight-bold mt-10 mb-2">
        {{ t('impactScoreVersionsPage.howToRead.title') }}
      </h2>
      <p class="text-body-1">
        {{ t('impactScoreVersionsPage.howToRead.body') }}
      </p>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  IMPACT_SCORE_CURRENT_METHOD_VERSION,
  IMPACT_SCORE_VERSIONS,
  IMPACT_SCORE_VERSIONS_LAST_MODIFIED,
} from '~~/shared/utils/impact-score-versions'
import { buildImpactScoreMethodJsonLd } from '~/utils/impact-score-jsonld'

const { t, locale } = useI18n()
const localePath = useLocalePath()
const canonicalUrl = useCanonicalUrl()

const formatDate = (iso: string) =>
  new Intl.DateTimeFormat(locale.value, { dateStyle: 'long' }).format(
    new Date(`${iso}T00:00:00Z`)
  )

useSeoMeta({
  title: () => String(t('impactScoreVersionsPage.seo.title')),
  description: () => String(t('impactScoreVersionsPage.seo.description')),
  ogTitle: () => String(t('impactScoreVersionsPage.seo.title')),
  ogDescription: () => String(t('impactScoreVersionsPage.seo.description')),
})

useHead(() => ({
  link: canonicalUrl.value
    ? [{ rel: 'canonical', href: canonicalUrl.value }]
    : [],
  script: [
    {
      key: 'impact-score-versions-jsonld',
      type: 'application/ld+json',
      innerHTML: JSON.stringify(
        buildImpactScoreMethodJsonLd({
          type: 'TechArticle',
          name: String(t('impactScoreVersionsPage.title')),
          description: String(t('impactScoreVersionsPage.seo.description')),
          url: canonicalUrl.value,
          dateModified: IMPACT_SCORE_VERSIONS_LAST_MODIFIED,
          version: IMPACT_SCORE_CURRENT_METHOD_VERSION,
          inLanguage: locale.value,
        })
      ),
    },
  ],
}))
</script>

<style scoped lang="sass">
.impact-score-versions__list
  list-style: none
  padding: 0
  display: grid
  gap: 2rem

.impact-score-versions__entry
  padding: 1.5rem
  background: rgb(var(--v-theme-surface-default))
  border-radius: 16px
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.24)

.impact-score-versions__changes
  padding-left: 1.2rem
</style>
