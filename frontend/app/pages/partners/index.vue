<template>
  <div class="partners-page">
    <section class="partners-hero" :aria-labelledby="heroHeadingId">
      <v-container class="py-16 px-4" max-width="xl">
        <div >
          <p class="text-overline text-uppercase mb-2 text-neutral-soft">
            {{ t('partners.hero.eyebrow') }}
          </p>
          <h1 :id="heroHeadingId" class="text-h3 text-sm-h2 font-weight-bold mb-4">
            {{ t('partners.hero.title') }}
          </h1>
          <p class="text-body-1 text-lg-h6 partners-hero__lead">
            {{ t('partners.hero.subtitle') }}
          </p>
          <p class="text-body-2 text-neutral-secondary mb-0">
            {{ t('partners.hero.description') }}
          </p>
        </div>
      </v-container>
    </section>

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

    <PartnersAffiliationSection
      :title="t('partners.affiliation.title')"
      :subtitle="t('partners.affiliation.subtitle')"
      :eyebrow="t('partners.affiliation.eyebrow')"
      :partners="affiliationPartners"
      :search-label="t('partners.affiliation.search.label')"
      :search-placeholder="t('partners.affiliation.search.placeholder')"
      :empty-state-label="t('partners.affiliation.empty')"
      :carousel-aria-label="t('partners.affiliation.carouselAriaLabel')"
      :link-label="t('partners.affiliation.linkLabel')"
    />

    <PartnersStaticCarouselSection
      :title="t('partners.ecosystem.title')"
      :subtitle="t('partners.ecosystem.subtitle')"
      :eyebrow="t('partners.ecosystem.eyebrow')"
      :partners="ecosystemPartners"
      :carousel-aria-label="t('partners.ecosystem.carouselAriaLabel')"
      :empty-state-label="t('partners.ecosystem.empty')"
      :link-label="t('partners.ecosystem.linkLabel')"
      :fallback-description="t('partners.ecosystem.fallbackDescription')"
    />

    <PartnersStaticCarouselSection
      tone="muted"
      :title="t('partners.mentors.title')"
      :subtitle="t('partners.mentors.subtitle')"
      :eyebrow="t('partners.mentors.eyebrow')"
      :partners="mentorPartners"
      :carousel-aria-label="t('partners.mentors.carouselAriaLabel')"
      :empty-state-label="t('partners.mentors.empty')"
      :link-label="t('partners.mentors.linkLabel')"
      :fallback-description="t('partners.mentors.fallbackDescription')"
    />

    <PartnersContactCta
      :title="t('partners.cta.title')"
      :description="t('partners.cta.description')"
      :cta-label="t('partners.cta.ctaLabel')"
      :cta-to="contactLink"
      :eyebrow="t('partners.cta.eyebrow')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AffiliationPartnerDto, StaticPartnerDto } from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import PartnersAffiliationSection from '~/components/domains/partners/PartnersAffiliationSection.vue'
import PartnersStaticCarouselSection from '~/components/domains/partners/PartnersStaticCarouselSection.vue'
import PartnersContactCta from '~/components/domains/partners/PartnersContactCta.vue'

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
const heroHeadingId = useId()

const { data, pending, error, refresh } = await useAsyncData<PartnersPageData>(
  'partners-data',
  async () => {
    const [affiliation, ecosystem, mentors] = await Promise.all([
      $fetch<AffiliationPartnerDto[]>('/api/partners/affiliation'),
      $fetch<StaticPartnerDto[]>('/api/partners/ecosystem'),
      $fetch<StaticPartnerDto[]>('/api/partners/mentors'),
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
  },
)

const affiliationPartners = computed(() => data.value?.affiliation ?? [])
const ecosystemPartners = computed(() => data.value?.ecosystem ?? [])
const mentorPartners = computed(() => data.value?.mentors ?? [])

const contactLink = computed(() => localePath('contact'))

const canonicalUrl = computed(() =>
  new URL(resolveLocalizedRoutePath('partners', locale.value), requestURL.origin).toString(),
)
const ogImageUrl = computed(() => new URL('/nudger-icon-512x512.png', requestURL.origin).toString())

useSeoMeta({
  title: () => String(t('partners.seo.title')),
  description: () => String(t('partners.seo.description')),
  ogTitle: () => String(t('partners.seo.title')),
  ogDescription: () => String(t('partners.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
  ],
}))
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

.partners-hero {
  background: radial-gradient(
      circle at top left,
      rgba(var(--v-theme-hero-gradient-start), 0.25),
      transparent 45%
    ),
    radial-gradient(circle at bottom right, rgba(var(--v-theme-hero-gradient-end), 0.2), transparent 50%),
    rgba(var(--v-theme-surface-default), 1);

  &__content {
    max-width: 780px;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  &__lead {
    max-width: 640px;
  }
}
</style>
