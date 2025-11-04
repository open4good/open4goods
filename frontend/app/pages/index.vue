<script setup lang="ts">
import { computed, ref } from 'vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const router = useRouter()
const localePath = useLocalePath()
const requestURL = useRequestURL()

const searchQuery = ref('')

const heroImageSrc = '/images/home/hero-placeholder.svg'

const problemItems = computed(() => [
  {
    icon: 'mdi-sign-direction',
    text: String(t('home.problems.items.labelsOverload')),
  },
  {
    icon: 'mdi-scale-balance',
    text: String(t('home.problems.items.budgetVsEcology')),
  },
  {
    icon: 'mdi-tab-multiple',
    text: String(t('home.problems.items.tooManyTabs')),
  },
])

const solutionBenefits = computed(() => [
  {
    emoji: '⏱️',
    label: String(t('home.solution.benefits.time')),
  },
  {
    emoji: '💰',
    label: String(t('home.solution.benefits.savings')),
  },
  {
    emoji: '🌍',
    label: String(t('home.solution.benefits.planet')),
  },
  {
    emoji: '🛡️',
    label: String(t('home.solution.benefits.trust')),
  },
])

const featureCards = computed(() => [
  {
    icon: 'mdi-leaf-circle',
    title: String(t('home.features.cards.impactScore.title')),
    description: String(t('home.features.cards.impactScore.description')),
  },
  {
    icon: 'mdi-chart-line',
    title: String(t('home.features.cards.priceComparison.title')),
    description: String(t('home.features.cards.priceComparison.description')),
  },
  {
    icon: 'mdi-source-branch',
    title: String(t('home.features.cards.openIndependent.title')),
    description: String(t('home.features.cards.openIndependent.description')),
  },
  {
    icon: 'mdi-shield-off-outline',
    title: String(t('home.features.cards.noTracking.title')),
    description: String(t('home.features.cards.noTracking.description')),
  },
  {
    icon: 'mdi-database',
    title: String(t('home.features.cards.massiveBase.title')),
    description: String(t('home.features.cards.massiveBase.description')),
  },
])

const categoryCards = computed(() => [
  {
    icon: 'mdi-cellphone',
    title: String(t('home.categories.items.electronics.title')),
    description: String(t('home.categories.items.electronics.description')),
    href: localePath({ name: 'search', query: { q: 'smartphone' } }),
  },
  {
    icon: 'mdi-fridge',
    title: String(t('home.categories.items.appliances.title')),
    description: String(t('home.categories.items.appliances.description')),
    href: localePath({ name: 'search', query: { q: 'lave-linge' } }),
  },
])

const blogEntries = computed(() => [
  {
    title: String(t('home.blog.items.first.title')),
    date: String(t('home.blog.items.first.date')),
    excerpt: String(t('home.blog.items.first.excerpt')),
    href: '#',
  },
  {
    title: String(t('home.blog.items.second.title')),
    date: String(t('home.blog.items.second.date')),
    excerpt: String(t('home.blog.items.second.excerpt')),
    href: '#',
  },
  {
    title: String(t('home.blog.items.third.title')),
    date: String(t('home.blog.items.third.date')),
    excerpt: String(t('home.blog.items.third.excerpt')),
    href: '#',
  },
])

const objectionItems = computed(() => [
  {
    icon: 'mdi-lightning-bolt',
    question: String(t('home.objections.items.aiEnergy.question')),
    answer: String(t('home.objections.items.aiEnergy.answer')),
  },
  {
    icon: 'mdi-recycle',
    question: String(t('home.objections.items.reuse.question')),
    answer: String(t('home.objections.items.reuse.answer')),
  },
  {
    icon: 'mdi-scale-balance',
    question: String(t('home.objections.items.independence.question')),
    answer: String(t('home.objections.items.independence.answer')),
  },
])

const faqItems = computed(() => [
  {
    question: String(t('home.faq.items.free.question')),
    answer: String(t('home.faq.items.free.answer')),
  },
  {
    question: String(t('home.faq.items.account.question')),
    answer: String(t('home.faq.items.account.answer')),
  },
  {
    question: String(t('home.faq.items.categories.question')),
    answer: String(t('home.faq.items.categories.answer')),
  },
  {
    question: String(t('home.faq.items.impactScore.question')),
    answer: String(t('home.faq.items.impactScore.answer')),
  },
  {
    question: String(t('home.faq.items.dataFreshness.question')),
    answer: String(t('home.faq.items.dataFreshness.answer')),
  },
  {
    question: String(t('home.faq.items.suggestProduct.question')),
    answer: String(t('home.faq.items.suggestProduct.answer')),
  },
])

const faqJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: faqItems.value.map((item) => ({
    '@type': 'Question',
    name: item.question,
    acceptedAnswer: {
      '@type': 'Answer',
      text: item.answer,
    },
  })),
}))

const canonicalUrl = computed(
  () => new URL(resolveLocalizedRoutePath('index', locale.value), requestURL.origin).toString(),
)

const alternateLinks = computed(() =>
  availableLocales.map((availableLocale) => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(resolveLocalizedRoutePath('index', availableLocale), requestURL.origin).toString(),
  })),
)

const searchLandingUrl = computed(() => localePath({ name: 'search' }))
const ogImageUrl = computed(() => new URL('/nudger-icon-512x512.png', requestURL.origin).toString())

const handleSearchSubmit = () => {
  const trimmedQuery = searchQuery.value.trim()
  const target = localePath({
    name: 'search',
    query: trimmedQuery.length > 0 ? { q: trimmedQuery } : undefined,
  })

  router.push(target)
}

useSeoMeta({
  title: () => String(t('home.seo.title')),
  description: () => String(t('home.seo.description')),
  ogTitle: () => String(t('home.seo.title')),
  ogDescription: () => String(t('home.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => String(t('siteIdentity.siteName')),
  ogLocale: () => locale.value.replace('-', '_'),
  ogImageAlt: () => String(t('home.seo.imageAlt')),
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...alternateLinks.value,
  ],
  script: [
    {
      key: 'home-faq-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(faqJsonLd.value),
    },
  ],
}))
</script>

<template>
  <div class="home-page">
    <section class="home-hero" aria-labelledby="home-hero-title">
      <v-container class="home-hero__container" max-width="lg">
        <div class="home-hero__layout">
          <div class="home-hero__content">
            <p class="home-hero__eyebrow">{{ t('home.hero.eyebrow') }}</p>
            <h1 id="home-hero-title" class="home-hero__title">
              {{ t('home.hero.title') }}
            </h1>
            <p class="home-hero__subtitle">{{ t('home.hero.subtitle') }}</p>

            <form class="home-hero__search" role="search" @submit.prevent="handleSearchSubmit">
              <v-text-field
                v-model="searchQuery"
                class="home-hero__search-input"
                variant="outlined"
                density="comfortable"
                :label="t('home.hero.search.label')"
                :placeholder="t('home.hero.search.placeholder')"
                :aria-label="t('home.hero.search.ariaLabel')"
                prepend-inner-icon="mdi-magnify"
                hide-details="auto"
              />
              <v-btn class="home-hero__search-button" type="submit" size="large" color="primary" elevation="2">
                {{ t('home.hero.search.cta') }}
              </v-btn>
            </form>

            <p class="home-hero__helper">
              <span aria-hidden="true">⚡</span>
              {{ t('home.hero.search.helper') }}
            </p>
          </div>

          <div class="home-hero__media" aria-hidden="true">
            <v-sheet rounded="xl" elevation="6" class="home-hero__media-sheet">
              <v-img :src="heroImageSrc" :alt="t('home.hero.imageAlt')" cover class="home-hero__image" />
            </v-sheet>
          </div>
        </div>
      </v-container>
    </section>

    <section class="home-section home-problems" aria-labelledby="home-problems-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-problems-title">{{ t('home.problems.title') }}</h2>
        </header>
        <v-row class="home-problems__grid" dense>
          <v-col v-for="item in problemItems" :key="item.text" cols="12" md="4">
            <v-card class="home-problems__card" variant="tonal">
              <v-icon class="home-problems__icon" :icon="item.icon" size="32" />
              <p class="home-problems__text">{{ item.text }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-solution" aria-labelledby="home-solution-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-solution-title">{{ t('home.solution.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.solution.description') }}</p>
        </header>
        <ul class="home-solution__list">
          <li v-for="item in solutionBenefits" :key="item.label" class="home-solution__item">
            <span class="home-solution__emoji" aria-hidden="true">{{ item.emoji }}</span>
            <span class="home-solution__label">{{ item.label }}</span>
          </li>
        </ul>
      </v-container>
    </section>

    <section class="home-section home-features" aria-labelledby="home-features-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-features-title">{{ t('home.features.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.features.subtitle') }}</p>
        </header>
        <v-row class="home-features__grid" dense>
          <v-col v-for="card in featureCards" :key="card.title" cols="12" sm="6" md="4">
            <v-card class="home-features__card" variant="outlined">
              <v-icon class="home-features__icon" :icon="card.icon" size="32" />
              <h3 class="home-features__card-title">{{ card.title }}</h3>
              <p class="home-features__card-description">{{ card.description }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-categories" aria-labelledby="home-categories-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-categories-title">{{ t('home.categories.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.categories.subtitle') }}</p>
        </header>
        <v-row class="home-categories__grid" dense>
          <v-col v-for="card in categoryCards" :key="card.title" cols="12" sm="6">
            <NuxtLink :to="card.href" class="home-categories__link">
              <v-card class="home-categories__card" variant="outlined">
                <div class="home-categories__card-header">
                  <v-icon class="home-categories__icon" :icon="card.icon" size="32" />
                  <h3 class="home-categories__card-title">{{ card.title }}</h3>
                </div>
                <p class="home-categories__card-description">{{ card.description }}</p>
                <span class="home-categories__cta">{{ t('home.categories.cta') }}</span>
              </v-card>
            </NuxtLink>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-trust" aria-labelledby="home-trust-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-trust-title">{{ t('home.trust.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.trust.subtitle') }}</p>
        </header>

        <div class="home-trust__logos" role="list">
          <div class="home-trust__logo" role="listitem">
            <v-img src="/images/ecosystem/ademe.png" :alt="t('home.trust.logos.ademe')" />
          </div>
        </div>

        <div class="home-trust__stats">
          <p class="home-trust__stat">{{ t('home.trust.stats.references') }}</p>
          <v-divider class="home-trust__divider" vertical />
          <p class="home-trust__stat">{{ t('home.trust.stats.updates') }}</p>
        </div>

        <div class="home-trust__blog">
          <div class="home-trust__blog-header">
            <h3 class="home-trust__blog-title">{{ t('home.blog.title') }}</h3>
            <NuxtLink class="home-trust__blog-link" :to="localePath('blog')">
              {{ t('home.blog.cta') }}
            </NuxtLink>
          </div>
          <v-row class="home-trust__blog-grid" dense>
            <v-col v-for="entry in blogEntries" :key="entry.title" cols="12" md="4">
              <v-card class="home-trust__blog-card" variant="tonal">
                <p class="home-trust__blog-date">{{ entry.date }}</p>
                <h4 class="home-trust__blog-card-title">{{ entry.title }}</h4>
                <p class="home-trust__blog-excerpt">{{ entry.excerpt }}</p>
                <NuxtLink class="home-trust__blog-read" :to="entry.href">
                  {{ t('home.blog.readMore') }}
                </NuxtLink>
              </v-card>
            </v-col>
          </v-row>
        </div>
      </v-container>
    </section>

    <section class="home-section home-objections" aria-labelledby="home-objections-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-objections-title">{{ t('home.objections.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.objections.subtitle') }}</p>
        </header>
        <v-row class="home-objections__grid" dense>
          <v-col v-for="item in objectionItems" :key="item.question" cols="12" md="4">
            <v-card class="home-objections__card" variant="outlined">
              <div class="home-objections__card-header">
                <v-icon class="home-objections__icon" :icon="item.icon" size="28" />
                <h3 class="home-objections__question">{{ item.question }}</h3>
              </div>
              <p class="home-objections__answer">{{ item.answer }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-faq" aria-labelledby="home-faq-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-faq-title">{{ t('home.faq.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.faq.subtitle') }}</p>
        </header>
        <v-expansion-panels class="home-faq__panels" multiple variant="accordion">
          <v-expansion-panel v-for="item in faqItems" :key="item.question">
            <v-expansion-panel-title class="home-faq__panel-title">
              {{ item.question }}
            </v-expansion-panel-title>
            <v-expansion-panel-text class="home-faq__panel-text">
              {{ item.answer }}
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-container>
    </section>

    <section class="home-cta" aria-labelledby="home-cta-title">
      <v-container class="home-cta__container" max-width="lg">
        <div class="home-cta__content">
          <h2 id="home-cta-title" class="home-cta__title">{{ t('home.cta.title') }}</h2>
          <p class="home-cta__subtitle">{{ t('home.cta.subtitle') }}</p>
          <div class="home-cta__actions">
            <form class="home-cta__form" role="search" @submit.prevent="handleSearchSubmit">
              <v-text-field
                v-model="searchQuery"
                class="home-cta__search-input"
                variant="outlined"
                density="comfortable"
                :label="t('home.hero.search.label')"
                :placeholder="t('home.hero.search.placeholder')"
                :aria-label="t('home.hero.search.ariaLabel')"
                prepend-inner-icon="mdi-magnify"
                hide-details="auto"
              />
              <v-btn class="home-cta__button" type="submit" size="large" color="primary" elevation="2">
                {{ t('home.cta.button') }}
              </v-btn>
            </form>
            <NuxtLink class="home-cta__link" :to="searchLandingUrl">
              {{ t('home.cta.altLink') }}
            </NuxtLink>
          </div>
        </div>
      </v-container>
    </section>
  </div>
</template>

<style scoped lang="sass">
.home-page
  display: flex
  flex-direction: column

.home-section
  padding-block: clamp(3rem, 6vw, 6rem)
  background: rgb(var(--v-theme-surface-default))

.home-section:nth-of-type(even)
  background: rgba(var(--v-theme-surface-muted), 0.6)

.home-section__container
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3.5rem)

.home-section__header
  max-width: 720px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

.home-hero
  padding-block: clamp(4rem, 10vw, 7rem)
  background: radial-gradient(circle at top left, rgba(var(--v-theme-hero-gradient-start), 0.28), transparent 55%),
    radial-gradient(circle at bottom right, rgba(var(--v-theme-hero-gradient-end), 0.25), transparent 60%),
    rgb(var(--v-theme-surface-default))

.home-hero__container
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3rem)

.home-hero__layout
  display: grid
  gap: clamp(2rem, 5vw, 3rem)

.home-hero__content
  display: flex
  flex-direction: column
  gap: 1.5rem

.home-hero__eyebrow
  font-weight: 600
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)
  margin: 0

.home-hero__title
  font-size: clamp(2rem, 5vw, 3.5rem)
  line-height: 1.1
  margin: 0

.home-hero__subtitle
  font-size: clamp(1.05rem, 2.5vw, 1.35rem)
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

.home-hero__search
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-hero__search-input :deep(.v-field)
  border-radius: 999px

.home-hero__search-input :deep(input)
  font-size: 1.05rem

.home-hero__search-button
  align-self: flex-start
  border-radius: 999px
  padding-inline: clamp(1.5rem, 4vw, 2.5rem)

.home-hero__helper
  display: flex
  align-items: center
  gap: 0.5rem
  font-size: 0.95rem
  color: rgb(var(--v-theme-text-neutral-soft))
  margin: 0

.home-hero__media
  display: flex
  justify-content: center

.home-hero__media-sheet
  padding: clamp(1rem, 3vw, 2rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.7)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)

.home-hero__image
  max-height: 320px
  border-radius: 1rem

.home-problems__grid
  row-gap: 1.5rem

.home-problems__card
  padding: 1.5rem
  display: flex
  flex-direction: column
  gap: 1rem
  height: 100%
  background: rgba(var(--v-theme-surface-primary-080), 0.6)

.home-problems__icon
  color: rgba(var(--v-theme-accent-primary-highlight), 0.9)

.home-problems__text
  margin: 0
  font-size: 1.05rem
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__list
  list-style: none
  display: grid
  gap: 1rem
  padding: 0
  margin: 0

.home-solution__item
  display: flex
  align-items: flex-start
  gap: 0.75rem
  padding: 1rem 1.25rem
  border-radius: 1rem
  background: rgba(var(--v-theme-surface-glass), 0.7)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)

.home-solution__emoji
  font-size: 1.5rem

.home-solution__label
  font-size: 1.05rem
  color: rgb(var(--v-theme-text-neutral-strong))

.home-features__grid
  row-gap: 1.5rem

.home-features__card
  padding: 1.75rem
  height: 100%
  display: flex
  flex-direction: column
  gap: 0.75rem
  border-radius: 1.25rem
  background: rgba(var(--v-theme-surface-glass-strong), 0.75)

.home-features__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)

.home-features__card-title
  font-size: 1.2rem
  margin: 0

.home-features__card-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-categories
  background: rgba(var(--v-theme-surface-primary-050), 0.8)

.home-categories__grid
  row-gap: 1.5rem

.home-categories__link
  text-decoration: none

.home-categories__card
  padding: 1.75rem
  height: 100%
  display: flex
  flex-direction: column
  gap: 0.75rem
  border-radius: 1.5rem
  transition: transform 0.25s ease, box-shadow 0.25s ease

.home-categories__card:hover
  transform: translateY(-4px)
  box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.18)

.home-categories__card-header
  display: flex
  align-items: center
  gap: 0.75rem

.home-categories__cta
  font-weight: 600
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)

.home-trust__logos
  display: flex
  flex-wrap: wrap
  gap: 1.5rem
  align-items: center

.home-trust__logo
  padding: 0.75rem 1.5rem
  border-radius: 999px
  background: rgba(var(--v-theme-surface-glass-strong), 0.8)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  :deep(img)
    max-height: 48px
    width: auto
    display: block

.home-trust__stats
  display: flex
  flex-wrap: wrap
  align-items: center
  gap: 1rem
  padding: 1.5rem
  border-radius: 1.25rem
  background: rgba(var(--v-theme-surface-glass), 0.7)
  margin-top: 2rem

.home-trust__stat
  margin: 0
  font-weight: 600

.home-trust__divider
  height: 32px

.home-trust__blog
  margin-top: clamp(2rem, 5vw, 3rem)
  display: flex
  flex-direction: column
  gap: 1.5rem

.home-trust__blog-header
  display: flex
  flex-direction: column
  gap: 0.5rem

.home-trust__blog-title
  margin: 0

.home-trust__blog-link
  font-weight: 600
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)
  text-decoration: none

.home-trust__blog-grid
  row-gap: 1.5rem

.home-trust__blog-card
  padding: 1.5rem
  height: 100%
  border-radius: 1.25rem
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-trust__blog-date
  margin: 0
  font-size: 0.95rem
  color: rgb(var(--v-theme-text-neutral-soft))

.home-trust__blog-card-title
  margin: 0
  font-size: 1.1rem

.home-trust__blog-excerpt
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  flex-grow: 1

.home-trust__blog-read
  font-weight: 600
  text-decoration: none
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)

.home-objections__grid
  row-gap: 1.5rem

.home-objections__card
  padding: 1.5rem
  height: 100%
  display: flex
  flex-direction: column
  gap: 0.75rem
  border-radius: 1.25rem
  background: rgba(var(--v-theme-surface-glass-strong), 0.8)

.home-objections__card-header
  display: flex
  gap: 0.75rem
  align-items: center

.home-objections__question
  margin: 0
  font-size: 1.05rem

.home-objections__answer
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-faq__panels
  border-radius: 1.25rem
  background: rgba(var(--v-theme-surface-glass-strong), 0.7)
  padding: 0.5rem

.home-faq__panel-title
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.home-faq__panel-text
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-cta
  padding-block: clamp(3rem, 8vw, 5rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.16), rgba(var(--v-theme-hero-gradient-end), 0.18))

.home-cta__container
  display: flex
  justify-content: center

.home-cta__content
  background: rgba(var(--v-theme-surface-default), 0.9)
  border-radius: clamp(1.5rem, 4vw, 2rem)
  padding: clamp(2rem, 5vw, 3rem)
  box-shadow: 0 24px 40px rgba(var(--v-theme-shadow-primary-600), 0.12)
  display: flex
  flex-direction: column
  gap: 1.5rem
  max-width: 720px
  width: 100%
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)

.home-cta__title
  margin: 0
  text-align: center
  font-size: clamp(1.8rem, 4vw, 2.5rem)

.home-cta__subtitle
  margin: 0
  text-align: center
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-cta__actions
  display: flex
  flex-direction: column
  gap: 1rem
  align-items: center

.home-cta__form
  width: 100%
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-cta__search-input :deep(.v-field)
  border-radius: 999px

.home-cta__button
  border-radius: 999px
  align-self: center
  padding-inline: clamp(1.75rem, 5vw, 2.5rem)

.home-cta__link
  text-decoration: none
  font-weight: 600
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)

@media (min-width: 960px)
  .home-hero__layout
    grid-template-columns: repeat(2, minmax(0, 1fr))
    align-items: center

  .home-hero__search
    flex-direction: row
    align-items: center

  .home-hero__search-button
    margin-inline-start: 0.5rem

  .home-hero__helper
    font-size: 1rem

  .home-solution__list
    grid-template-columns: repeat(2, minmax(0, 1fr))

  .home-trust__blog-header
    flex-direction: row
    justify-content: space-between
    align-items: baseline

  .home-cta__actions
    flex-direction: row
    justify-content: center
    gap: 1.5rem

  .home-cta__form
    flex-direction: row
    align-items: center
    max-width: 500px

  .home-cta__button
    margin: 0
</style>
