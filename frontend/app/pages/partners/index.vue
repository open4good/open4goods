<template>
  <div class="partners-page">
    <PageHeader
      variant="hero-standard"
      :eyebrow="t('partners.hero.eyebrow')"
      :title="t('partners.hero.title')"
      :subtitle="t('partners.hero.subtitle')"
      :description-html="t('partners.hero.description')"
      description-bloc-id="webpages:partners:hero-overview"
      background="surface-variant"
      surface-variant="orbit"
      layout="2-columns"
      container="lg"
      show-media
      media-type="glow"
      heading-level="h1"
      schema-type="AboutPage"
      :og-image="ogImageUrl"
    />

    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="partners-page__loader"
      :aria-label="t('partners.loading')"
      role="progressbar"
    />

    <v-container v-if="error" class="py-6 px-4 mx-auto" max-width="xl">
      <v-alert
        type="error"
        variant="tonal"
        border="start"
        prominent
        class="mb-4"
        role="alert"
      >
        {{ t('partners.errors.loadFailed') }}
      </v-alert>
      <v-btn color="primary" variant="tonal" @click="refresh">
        {{ t('common.actions.retry') }}
      </v-btn>
    </v-container>

    <v-sheet
      tag="section"
      class="partners-strip partners-strip--default"
      color="transparent"
    >
      <PartnersAffiliationSection
        :title="t('partners.affiliation.title')"
        :subtitle="t('partners.affiliation.subtitle')"
        :partners="affiliationPartners"
        :search-label="t('partners.affiliation.search.label')"
        :search-placeholder="t('partners.affiliation.search.placeholder')"
        :empty-state-label="t('partners.affiliation.empty')"
        :carousel-aria-label="t('partners.affiliation.carouselAriaLabel')"
        :link-label="t('partners.affiliation.linkLabel')"
      />
    </v-sheet>

    <v-sheet
      tag="section"
      class="partners-strip partners-strip--muted"
      color="transparent"
    >
      <PartnersStaticCarouselSection
        :title="t('partners.ecosystem.title')"
        :subtitle="t('partners.ecosystem.subtitle')"
        :partners="ecosystemPartners"
        :carousel-aria-label="t('partners.ecosystem.carouselAriaLabel')"
        :empty-state-label="t('partners.ecosystem.empty')"
        :link-label="t('partners.ecosystem.linkLabel')"
        :fallback-description="t('partners.ecosystem.fallbackDescription')"
      />
    </v-sheet>

    <v-sheet
      tag="section"
      class="partners-strip partners-strip--alt"
      color="transparent"
    >
      <PartnersStaticCarouselSection
        tone="muted"
        :title="t('partners.mentors.title')"
        :subtitle="t('partners.mentors.subtitle')"
        :partners="mentorPartners"
        :carousel-aria-label="t('partners.mentors.carouselAriaLabel')"
        :empty-state-label="t('partners.mentors.empty')"
        :link-label="t('partners.mentors.linkLabel')"
        :fallback-description="t('partners.mentors.fallbackDescription')"
      />
    </v-sheet>

    <v-sheet
      tag="section"
      class="partners-strip partners-strip--cta"
      color="transparent"
    >
      <PartnersContactCta
        :title="t('partners.cta.title')"
        :description="t('partners.cta.description')"
        :cta-label="t('partners.cta.ctaLabel')"
        :cta-to="contactLink"
        :eyebrow="t('partners.cta.eyebrow')"
      />
    </v-sheet>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AffiliationPartnerDto,
  StaticPartnerDto,
} from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import PartnersAffiliationSection from '~/components/domains/partners/PartnersAffiliationSection.vue'
import PartnersStaticCarouselSection from '~/components/domains/partners/PartnersStaticCarouselSection.vue'
import PartnersContactCta from '~/components/domains/partners/PartnersContactCta.vue'
import PageHeader from '~/components/shared/header/PageHeader.vue'

interface PartnersPageData {
  affiliation: AffiliationPartnerDto[]
  ecosystem: StaticPartnerDto[]
  mentors: StaticPartnerDto[]
}

definePageMeta({
  name: 'partners',
  ssr: false,
})

const { t, locale } = useI18n()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const { data, pending, error, refresh } = await useAsyncData<PartnersPageData>(
  'partners-data',
  async () => {
    const [affiliation, ecosystem, mentors] = await Promise.all([
      $fetch<AffiliationPartnerDto[]>('/api/partners/affiliation', {
        headers: requestHeaders,
      }),
      $fetch<StaticPartnerDto[]>('/api/partners/ecosystem', {
        headers: requestHeaders,
      }),
      $fetch<StaticPartnerDto[]>('/api/partners/mentors', {
        headers: requestHeaders,
      }),
    ])

    return {
      affiliation,
      ecosystem,
      mentors,
    }
  },
  {
    server: false,
    lazy: false,
  }
)

const affiliationPartners = computed(() => data.value?.affiliation ?? [])
const ecosystemPartners = computed(() => data.value?.ecosystem ?? [])
const mentorPartners = computed(() => data.value?.mentors ?? [])

const contactLink = computed(() => localePath('contact'))

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('partners', locale.value),
    requestURL.origin
  ).toString()
)
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

// Note: SEO metadata (title, description, OG tags, canonical, JSON-LD)
// is now handled automatically by PageHeader component via useHeaderSeo composable
</script>

<style scoped lang="scss">
.partners-page {
  display: flex;
  flex-direction: column;
  gap: 0;

  &__loader {
    margin: 0;
  }
}

.partners-strip {
  padding: clamp(3.25rem, 6vw, 5.5rem) 0;
  background-color: rgb(var(--v-theme-surface-default));
}

.partners-strip--muted {
  background-color: rgb(var(--v-theme-surface-muted));
}

.partners-strip--alt {
  background: linear-gradient(
    140deg,
    rgba(var(--v-theme-surface-primary-080), 0.9),
    rgba(var(--v-theme-surface-default), 1)
  );
}

.partners-strip--cta {
  background-color: rgb(var(--v-theme-surface-ice-050));
}

.partners-hero {
  &__content {
    max-width: 780px;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
}
</style>
