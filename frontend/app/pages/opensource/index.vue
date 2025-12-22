<template>
  <div class="opensource-page">
    <PageHeader
      variant="hero-standard"
      :title="t('opensource.hero.title')"
      :subtitle="t('opensource.hero.subtitle')"
      description-bloc-id="webpages:opensource:hero-description"
      background="surface-variant"
      surface-variant="prism"
      layout="2-columns"
      container="lg"
      :primary-cta="heroPrimaryCta"
      :cta-group-label="heroCtaGroupLabel"
      show-media
      media-type="card"
      :hero-card="heroInfoCard"
      heading-level="h1"
      schema-type="AboutPage"
      :og-image="ogImageUrl"
    />

    <v-container class="opensource-live" fluid>
      <v-row class="g-4" align="stretch">
        <v-col cols="12" md="4">
          <v-card class="opensource-version-card h-100" color="surface-glass">
            <v-card-title class="d-flex align-center justify-space-between">
              <div class="text-body-1 text-uppercase font-weight-medium">
                {{ t('opensource.live.version.title') }}
              </div>
              <v-chip color="primary" variant="tonal" size="small">
                {{ t('opensource.live.version.status') }}
              </v-chip>
            </v-card-title>
            <v-card-text>
              <div class="text-h4 font-weight-bold">
                {{ currentVersion }}
              </div>
              <div class="text-body-2 mt-2 text-medium-emphasis">
                {{ t('opensource.live.version.subtitle') }}
              </div>
              <v-divider class="my-4" />
              <div class="d-flex align-center ga-3">
                <v-icon
                  icon="mdi-source-repository"
                  color="primary"
                  size="32"
                />
                <span class="text-body-2">
                  {{ t('opensource.live.version.description') }}
                </span>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="8">
          <v-card class="opensource-reports-card h-100" color="surface-glass">
            <v-card-title class="d-flex flex-column align-start">
              <div class="text-overline text-uppercase">
                {{ t('opensource.live.eyebrow') }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ t('opensource.live.title') }}
              </div>
              <div class="text-body-2 text-medium-emphasis">
                {{ t('opensource.live.subtitle') }}
              </div>
            </v-card-title>

            <v-tabs
              v-model="activeReportTab"
              align-tabs="center"
              bg-color="primary"
              stacked
              grow
            >
              <v-tab
                v-for="report in liveReportTabs"
                :key="report.value"
                :value="report.value"
              >
                <v-icon :icon="report.icon" size="28" class="mb-1" />
                {{ report.label }}
              </v-tab>
            </v-tabs>

            <v-tabs-window v-model="activeReportTab">
              <v-tabs-window-item
                v-for="report in liveReportTabs"
                :key="report.value"
                :value="report.value"
              >
                <v-card flat>
                  <v-card-text>
                    <div class="text-body-2 text-medium-emphasis mb-3">
                      {{ report.description }}
                    </div>
                    <div class="opensource-report-frame">
                      <iframe
                        :src="report.src"
                        :title="report.label"
                        loading="lazy"
                        referrerpolicy="no-referrer"
                      />
                    </div>
                  </v-card-text>
                </v-card>
              </v-tabs-window-item>
            </v-tabs-window>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <OpensourcePillarsSection
      :eyebrow="t('opensource.pillars.eyebrow')"
      :title="t('opensource.pillars.title')"
      description-bloc-id="webpages:opensource:pillars-intro"
      :cards="pillarCards"
      :feedback-callout="feedbackCallout"
    />

    <OpensourceContributionSection
      :eyebrow="t('opensource.contribution.eyebrow')"
      :title="t('opensource.contribution.title')"
      description-bloc-id="webpages:opensource:contribute-intro"
      :steps="contributionSteps"
    />

    <OpensourceResourcesSection
      :eyebrow="t('opensource.resources.eyebrow')"
      :title="t('opensource.resources.title')"
      description-bloc-id="webpages:opensource:resources-intro"
      :resources="resourceLinks"
      :contact="contactCta"
      :opendata-callout="opendataCallout"
      :prompt-callout="promptCallout"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '~/components/shared/header/PageHeader.vue'
import OpensourcePillarsSection from '~/components/domains/opensource/OpensourcePillarsSection.vue'
import OpensourceContributionSection from '~/components/domains/opensource/OpensourceContributionSection.vue'
import OpensourceResourcesSection from '~/components/domains/opensource/OpensourceResourcesSection.vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

interface HeroCtaDisplay {
  label: string
  href: string
  ariaLabel: string
  icon?: string
  color?: string
  variant?: 'flat' | 'outlined' | 'tonal' | 'text' | 'plain'
  target?: string
  rel?: string
}

interface PillarCardDisplay {
  icon: string
  title: string
  descriptionBlocId: string
  action?: {
    label: string
    href: string
    ariaLabel: string
    target?: string
    rel?: string
  }
}

interface ContributionStepDisplay {
  title: string
  descriptionBlocId: string
  icon: string
}

interface ResourceLinkDisplay {
  icon: string
  title: string
  descriptionBlocId: string
  href: string
  ariaLabel: string
  target?: string
  rel?: string
}

interface ContactCtaDisplay {
  title: string
  descriptionBlocId: string
  ctaLabel: string
  ctaHref: string
  ctaAriaLabel: string
}

interface HeroInfoCardDisplay {
  icon: string
  title: string
  bodyHtml?: string
  items: { icon?: string; text: string }[]
}

interface FeedbackCalloutDisplay {
  title: string
  description: string
  points: string[]
  ctaLabel: string
  ctaHref: string
  ctaAriaLabel: string
}

interface OpendataCalloutDisplay {
  title: string
  description: string
  ctaLabel: string
  ctaHref: string
  ctaAriaLabel: string
}

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const requestURL = useRequestURL()
const localePath = useLocalePath()
const currentVersion = 'v0.9.8'
const activeReportTab = ref('frontend-coverage')

const heroPrimaryCta = computed(() => ({
  label: String(t('opensource.hero.primaryCta.label')),
  ariaLabel: `${String(t('opensource.hero.primaryCta.label'))} : ${String(t('opensource.hero.primaryCta.ariaLabel'))}`,
  href: 'https://github.com/open4good/open4goods',
  icon: 'mdi-github',
  color: 'primary',
  variant: 'flat' as const,
  target: '_blank',
  rel: 'noopener',
}))

const heroCtaGroupLabel = computed(() =>
  String(t('opensource.hero.ctaGroupLabel'))
)

const heroInfoCard = computed<HeroInfoCardDisplay>(() => ({
  icon: 'mdi-source-branch',
  title: String(t('opensource.hero.infoCard.title')),
  bodyHtml: `<strong>${String(t('opensource.hero.infoCard.highlight'))}</strong> ${String(t('opensource.hero.infoCard.description'))}`,
  items: [
    {
      icon: 'mdi-checkbox-marked-circle-outline',
      text: String(t('opensource.hero.infoCard.items.openLicenses')),
    },
    {
      icon: 'mdi-checkbox-marked-circle-outline',
      text: String(t('opensource.hero.infoCard.items.collaborativeReviews')),
    },
    {
      icon: 'mdi-checkbox-marked-circle-outline',
      text: String(t('opensource.hero.infoCard.items.sharedGovernance')),
    },
  ],
}))

const pillarCards = computed<PillarCardDisplay[]>(() => [
  {
    icon: 'mdi-source-branch-sync',
    title: String(t('opensource.pillars.cards.transparency.title')),
    descriptionBlocId: 'webpages:opensource:pillars-transparency',
    action: {
      label: String(t('opensource.pillars.cards.transparency.cta')),
      ariaLabel: `${String(t('opensource.pillars.cards.transparency.cta'))} : ${String(t('opensource.pillars.cards.transparency.ariaLabel'))}`,
      href: 'https://github.com/open4good/open4goods',
      target: '_blank',
      rel: 'noopener',
    },
  },
  {
    icon: 'mdi-earth-arrow-right',
    title: String(t('opensource.pillars.cards.methodology.title')),
    descriptionBlocId: 'webpages:opensource:pillars-methodology',
    action: {
      label: String(t('opensource.pillars.cards.methodology.cta')),
      ariaLabel: `${String(t('opensource.pillars.cards.methodology.cta'))} : ${String(t('opensource.pillars.cards.methodology.ariaLabel'))}`,
      href: localePath('impact-score'),
    },
  },
  {
    icon: 'mdi-account-group-outline',
    title: String(t('opensource.pillars.cards.community.title')),
    descriptionBlocId: 'webpages:opensource:pillars-community',
    action: {
      label: String(t('opensource.pillars.cards.community.cta')),
      ariaLabel: `${String(t('opensource.pillars.cards.community.cta'))} : ${String(t('opensource.pillars.cards.community.ariaLabel'))}`,
      href: localePath('team'),
    },
  },
])

const contributionSteps = computed<ContributionStepDisplay[]>(() => [
  {
    title: String(t('opensource.contribution.steps.setup.title')),
    descriptionBlocId: 'webpages:opensource:contribute-setup',
    icon: 'mdi-console',
  },
  {
    title: String(t('opensource.contribution.steps.issues.title')),
    descriptionBlocId: 'webpages:opensource:contribute-issues',
    icon: 'mdi-lightbulb-on-outline',
  },
  {
    title: String(t('opensource.contribution.steps.share.title')),
    descriptionBlocId: 'webpages:opensource:contribute-share',
    icon: 'mdi-heart-outline',
  },
])

const resourceLinks = computed<ResourceLinkDisplay[]>(() => [
  {
    icon: 'mdi-book-open-page-variant',
    title: String(t('opensource.resources.links.guide.title')),
    descriptionBlocId: 'webpages:opensource:resources-guide',
    href: 'https://github.com/open4good/open4goods#readme',
    ariaLabel: `${String(t('opensource.resources.links.guide.title'))} : ${String(t('opensource.resources.links.guide.ariaLabel'))}`,
    target: '_blank',
    rel: 'noopener',
  },
  {
    icon: 'mdi-table-check',
    title: String(t('opensource.resources.links.issues.title')),
    descriptionBlocId: 'webpages:opensource:resources-issues',
    href: 'https://github.com/open4good/open4goods/issues',
    ariaLabel: `${String(t('opensource.resources.links.issues.title'))} : ${String(t('opensource.resources.links.issues.ariaLabel'))}`,
    target: '_blank',
    rel: 'noopener',
  },
  {
    icon: 'mdi-newspaper-variant-outline',
    title: String(t('opensource.resources.links.updates.title')),
    descriptionBlocId: 'webpages:opensource:resources-updates',
    href: localePath('blog'),
    ariaLabel: `${String(t('opensource.resources.links.updates.title'))} : ${String(t('opensource.resources.links.updates.ariaLabel'))}`,
  },
])

const contactCta = computed<ContactCtaDisplay>(() => ({
  title: String(t('opensource.resources.contact.title')),
  descriptionBlocId: 'webpages:opensource:community-callout',
  ctaLabel: String(t('opensource.resources.contact.cta.label')),
  ctaHref: localePath('contact'),
  ctaAriaLabel: `${String(t('opensource.resources.contact.cta.label'))} : ${String(t('opensource.resources.contact.cta.ariaLabel'))}`,
}))

const feedbackCallout = computed<FeedbackCalloutDisplay>(() => ({
  title: String(t('opensource.resources.feedback.title')),
  description: String(t('opensource.resources.feedback.description')),
  points: [
    String(t('opensource.resources.feedback.points.bugs')),
    String(t('opensource.resources.feedback.points.features')),
    String(t('opensource.resources.feedback.points.votes')),
  ],
  ctaLabel: String(t('opensource.resources.feedback.cta.label')),
  ctaHref: localePath('feedback'),
  ctaAriaLabel: `${String(t('opensource.resources.feedback.cta.label'))} : ${String(t('opensource.resources.feedback.cta.ariaLabel'))}`,
}))

const opendataCallout = computed<OpendataCalloutDisplay>(() => ({
  title: String(t('opensource.resources.opendata.title')),
  description: String(t('opensource.resources.opendata.description')),
  ctaLabel: String(t('opensource.resources.opendata.cta.label')),
  ctaHref: localePath('opendata'),
  ctaAriaLabel: `${String(t('opensource.resources.opendata.cta.label'))} : ${String(t('opensource.resources.opendata.cta.ariaLabel'))}`,
}))

const promptCallout = computed(() => ({
  title: String(t('opensource.resources.prompt.title')),
  description: String(t('opensource.resources.prompt.description')),
  ctaLabel: String(t('opensource.resources.prompt.cta.label')),
  ctaHref: localePath('prompt') + '?template=feature-request',
  ctaAriaLabel: String(t('opensource.resources.prompt.cta.ariaLabel')),
}))

const liveReportTabs = computed(() => [
  {
    value: 'frontend-coverage',
    icon: 'mdi-vuejs',
    label: String(t('opensource.live.tabs.frontend.title')),
    description: String(t('opensource.live.tabs.frontend.description')),
    src: '/reports/test-coverage/frontend/index.html',
  },
  {
    value: 'backend-coverage',
    icon: 'mdi-alpha-b-box-outline',
    label: String(t('opensource.live.tabs.backend.title')),
    description: String(t('opensource.live.tabs.backend.description')),
    src: '/reports/test-coverage/backend/index.html',
  },
  {
    value: 'maven-site',
    icon: 'mdi-cog-outline',
    label: String(t('opensource.live.tabs.maven.title')),
    description: String(t('opensource.live.tabs.maven.description')),
    src: '/public/reports/maven-site/index.html',
  },
])

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('opensource', locale.value),
    requestURL.origin
  ).toString()
)

const siteName = computed(() => String(t('siteIdentity.siteName')))
const ogLocale = computed(() => locale.value.replace('-', '_'))
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)
const ogImageAlt = computed(() => String(t('opensource.seo.imageAlt')))

const alternateLinks = computed(() =>
  availableLocales.map(availableLocale => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(
      resolveLocalizedRoutePath('opensource', availableLocale),
      requestURL.origin
    ).toString(),
  }))
)

// Note: SEO metadata (title, description, OG tags, canonical, JSON-LD)
// is now handled automatically by PageHeader component via useHeaderSeo composable

// Keep alternate links in head (not handled by PageHeader)
useHead(() => ({
  link: [...alternateLinks.value],
}))
</script>

<style scoped lang="sass">
.opensource-page
  display: flex
  flex-direction: column

.opensource-live
  padding-top: 24px

.opensource-version-card
  .v-card-title
    gap: 8px

.opensource-report-frame
  position: relative
  border-radius: 12px
  overflow: hidden
  min-height: 420px
  background: rgba(var(--v-theme-surface-muted), 0.24)

  iframe
    border: 0
    width: 100%
    height: 100%
</style>
