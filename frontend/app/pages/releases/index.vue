<template>
  <div class="releases-page">
    <PageHeader
      :eyebrow="t('releases.hero.eyebrow')"
      :title="t('releases.hero.title')"
      background="image"
      background-image-asset-key="releasesBackground"
      layout="2-columns-right-media"
      container="fluid"
      show-media
      media-type="card"
      :hero-card="educationCard"
    >
      <template #subtitle>
        <p class="page-header__subtitle">
          {{ t('releases.hero.subtitle') }}
        </p>
        <LatestReleaseBadge
          class="releases-page__latest-chip"
          :scroll-target="faqAnchor"
        />
      </template>
    </PageHeader>

    <v-container class="releases-page__content">
      <div :id="faqAnchorId" class="releases-page__anchor" aria-hidden="true" />
      <h2 class="releases-page__section-title">
        {{ t('releases.faq.title') }}
      </h2>
      <p class="releases-page__section-subtitle">
        {{ t('releases.faq.subtitle') }}
      </p>
      <ReleaseAccordion
        :releases="releases"
        :loading="pending"
        :error="error"
        @retry="refresh"
      />
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import LatestReleaseBadge from '~/components/domains/releases/LatestReleaseBadge.vue'
import PageHeader from '~/components/shared/header/PageHeader.vue'
import ReleaseAccordion from '~/components/domains/releases/ReleaseAccordion.vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

definePageMeta({
  name: 'releases',
  ssr: false,
})

const { t, locale } = useI18n()
const requestURL = useRequestURL()

const faqAnchorId = 'releases-faq'
const faqAnchor = `#${faqAnchorId}`

const { releases, pending, error, refresh, latestRelease } =
  await useReleaseNotes()

const releasesCount = computed(() => releases.value.length)
const latestReleaseName = computed(
  () => latestRelease.value?.name ?? t('releases.empty')
)

const educationCard = computed(() => ({
  icon: 'mdi-text-box-multiple-outline',
  title: String(t('releases.summary.title')),
  bodyHtml: String(t('releases.summary.body')),
  items: [
    {
      icon: 'mdi-rocket-launch',
      text: String(
        t('releases.summary.latestItem', { name: latestReleaseName.value })
      ),
    },
    {
      icon: 'mdi-format-list-bulleted-square',
      text: String(
        t('releases.summary.countItem', { count: releasesCount.value })
      ),
    },
  ],
}))

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('releases', locale.value),
    requestURL.origin
  ).toString()
)

const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(t('releases.seo.title')),
  description: () => String(t('releases.seo.description')),
  ogTitle: () => String(t('releases.seo.title')),
  ogDescription: () => String(t('releases.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
}))
</script>

<style scoped lang="sass">
.releases-page
  display: flex
  flex-direction: column
  gap: 32px

.releases-page__content
  padding-bottom: 56px

.releases-page__anchor
  position: relative
  top: -80px
  height: 1px

.releases-page__section-title
  margin-bottom: 8px

.releases-page__section-subtitle
  margin-top: 0
  margin-bottom: 24px
  color: rgba(var(--v-theme-on-surface), 0.72)

.releases-page__latest-chip
  margin-top: 4px
</style>
