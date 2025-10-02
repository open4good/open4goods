<template>
  <div class="opensource-page">
    <OpensourceHero
      :eyebrow="t('opensource.hero.eyebrow')"
      :title="t('opensource.hero.title')"
      :subtitle="t('opensource.hero.subtitle')"
      description-bloc-id="webpages:opensource:hero-description"
      :stats="heroStats"
      :ctas="heroCtas"
    />

    <OpensourcePillarsSection
      :eyebrow="t('opensource.pillars.eyebrow')"
      :title="t('opensource.pillars.title')"
      description-bloc-id="webpages:opensource:pillars-intro"
      :cards="pillarCards"
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
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import OpensourceHero from '~/components/domains/opensource/OpensourceHero.vue'
import OpensourcePillarsSection from '~/components/domains/opensource/OpensourcePillarsSection.vue'
import OpensourceContributionSection from '~/components/domains/opensource/OpensourceContributionSection.vue'
import OpensourceResourcesSection from '~/components/domains/opensource/OpensourceResourcesSection.vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

interface HeroStatDisplay {
  value: string
  label: string
}

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

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const requestURL = useRequestURL()
const localePath = useLocalePath()

const heroStats = computed<HeroStatDisplay[]>(() => [
  {
    value: String(t('opensource.hero.stats.open.value')),
    label: String(t('opensource.hero.stats.open.label')),
  },
  {
    value: String(t('opensource.hero.stats.ssr.value')),
    label: String(t('opensource.hero.stats.ssr.label')),
  },
  {
    value: String(t('opensource.hero.stats.community.value')),
    label: String(t('opensource.hero.stats.community.label')),
  },
])

const heroCtas = computed<HeroCtaDisplay[]>(() => [
  {
    label: String(t('opensource.hero.primaryCta.label')),
    ariaLabel: String(t('opensource.hero.primaryCta.ariaLabel')),
    href: 'https://github.com/open4good/open4goods',
    icon: 'mdi-github',
    color: 'primary',
    variant: 'flat',
    target: '_blank',
    rel: 'noopener',
  }
])

const pillarCards = computed<PillarCardDisplay[]>(() => [
  {
    icon: 'mdi-source-branch-sync',
    title: String(t('opensource.pillars.cards.transparency.title')),
    descriptionBlocId: 'webpages:opensource:pillars-transparency',
    action: {
      label: String(t('opensource.pillars.cards.transparency.cta')),
      ariaLabel: String(t('opensource.pillars.cards.transparency.ariaLabel')),
      href: 'https://github.com/open4good/open4goods',
      target: '_blank',
      rel: 'noopener',
    },
  },
  {
    icon: 'mdi-earth-check',
    title: String(t('opensource.pillars.cards.methodology.title')),
    descriptionBlocId: 'webpages:opensource:pillars-methodology',
    action: {
      label: String(t('opensource.pillars.cards.methodology.cta')),
      ariaLabel: String(t('opensource.pillars.cards.methodology.ariaLabel')),
      href: localePath('impact-score'),
    },
  },
  {
    icon: 'mdi-account-group-outline',
    title: String(t('opensource.pillars.cards.community.title')),
    descriptionBlocId: 'webpages:opensource:pillars-community',
    action: {
      label: String(t('opensource.pillars.cards.community.cta')),
      ariaLabel: String(t('opensource.pillars.cards.community.ariaLabel')),
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
    ariaLabel: String(t('opensource.resources.links.guide.ariaLabel')),
    target: '_blank',
    rel: 'noopener',
  },
  {
    icon: 'mdi-view-kanban',
    title: String(t('opensource.resources.links.issues.title')),
    descriptionBlocId: 'webpages:opensource:resources-issues',
    href: 'https://github.com/open4good/open4goods/issues',
    ariaLabel: String(t('opensource.resources.links.issues.ariaLabel')),
    target: '_blank',
    rel: 'noopener',
  },
  {
    icon: 'mdi-newspaper-variant-outline',
    title: String(t('opensource.resources.links.updates.title')),
    descriptionBlocId: 'webpages:opensource:resources-updates',
    href: localePath('blog'),
    ariaLabel: String(t('opensource.resources.links.updates.ariaLabel')),
  },
])

const contactCta = computed<ContactCtaDisplay>(() => ({
  title: String(t('opensource.resources.contact.title')),
  descriptionBlocId: 'webpages:opensource:community-callout',
  ctaLabel: String(t('opensource.resources.contact.cta.label')),
  ctaHref: localePath('contact'),
  ctaAriaLabel: String(t('opensource.resources.contact.cta.ariaLabel')),
}))

const canonicalUrl = computed(() =>
  new URL(resolveLocalizedRoutePath('opensource', locale.value), requestURL.origin).toString(),
)

const siteName = computed(() => String(t('siteIdentity.siteName')))
const ogLocale = computed(() => locale.value.replace('-', '_'))
const ogImageUrl = computed(() => new URL('/nudger-icon-512x512.png', requestURL.origin).toString())
const ogImageAlt = computed(() => String(t('opensource.seo.imageAlt')))

const alternateLinks = computed(() =>
  availableLocales.map((availableLocale) => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(resolveLocalizedRoutePath('opensource', availableLocale), requestURL.origin).toString(),
  })),
)

useSeoMeta({
  title: () => String(t('opensource.seo.title')),
  description: () => String(t('opensource.seo.description')),
  ogTitle: () => String(t('opensource.seo.title')),
  ogDescription: () => String(t('opensource.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => ogImageAlt.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...alternateLinks.value,
  ],
}))
</script>

<style scoped lang="sass">
.opensource-page
  display: flex
  flex-direction: column
</style>
