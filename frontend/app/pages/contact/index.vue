<template>
  <div class="contact-page">
    <PageHeader
      variant="hero-standard"
      :title="pageTitle"
      :subtitle="t('contact.hero.subtitle')"
      heading-level="h1"
      :heading-id="'contact-hero-heading'"
      :eyebrow="t('contact.hero.eyebrow')"
      background="image"
      background-image-asset-key="contactBackground"
      surface-variant="pulse"
      layout="2-columns"
      container="xl"
    >
      <template #description>
        <div v-if="!hasPrefill">
          <p class="mb-6">{{ t('contact.hero.description') }}</p>
          <ul class="contact-hero__highlights" role="list">
            <li
              v-for="item in heroHighlights"
              :key="item.text"
              class="contact-hero__highlight"
            >
              <v-icon
                :icon="item.icon"
                size="26"
                class="contact-hero__highlight-icon"
              />
              <span class="contact-hero__highlight-text">{{ item.text }}</span>
            </li>
          </ul>
        </div>
      </template>

      <template #media>
        <v-card
          v-if="!hasPrefill"
          elevation="10"
          rounded="xl"
          color="primary"
          theme="dark"
          aria-labelledby="contact-hero-card-heading"
          role="region"
        >
          <div class="contact-hero__card-overlay" aria-hidden="true" />
          <div class="contact-hero__card-inner">
            <div class="contact-hero__badge">
              <v-icon
                icon="mdi-message-badge-outline"
                size="34"
                aria-hidden="true"
              />
            </div>
            <h2 id="contact-hero-card-heading" class="contact-hero__card-title">
              {{ t('contact.hero.channels.title') }}
            </h2>
            <p class="contact-hero__card-description">
              {{ t('contact.hero.channels.subtitle') }}
            </p>
            <v-divider class="contact-hero__card-divider" />
            <v-list
              bg-color="transparent"
              density="comfortable"
              lines="one"
              role="list"
              class="pa-0"
            >
              <v-list-item
                v-for="channel in heroChannels"
                :key="channel.label"
                :title="channel.label"
                :subtitle="channel.description"
                class="contact-hero__card-item rounded-lg"
                role="listitem"
              >
                <template #prepend>
                  <v-avatar color="white" size="40">
                    <v-icon :icon="channel.icon" color="primary" size="24" />
                  </v-avatar>
                </template>
              </v-list-item>
            </v-list>
          </div>
        </v-card>
      </template>
    </PageHeader>

    <ContactDetailsSection
      v-if="!hasPrefill"
      :eyebrow="t('contact.details.eyebrow')"
      :title="t('contact.details.title')"
      :subtitle="t('contact.details.subtitle')"
      :items="contactDetailItems"
    />

    <ContactFormCard
      :site-key="siteKey"
      :submitting="submitting"
      :success="success"
      :error-message="formError"
      :initial-subject="initialSubject"
      :initial-message="initialMessage"
      @submit="handleFormSubmit"
      @reset-feedback="handleResetFeedback"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { FetchError } from 'ofetch'
import type { ContactResponseDto } from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import PageHeader from '~/components/shared/header/PageHeader.vue'
import type { ContactFormPayload } from '~/components/domains/contact/ContactFormCard.vue'
import type { ContactDetailItem } from '~/components/domains/contact/ContactDetailsSection.vue'

interface HeroHighlight {
  icon: string
  text: string
}

interface HeroContactChannel {
  icon: string
  label: string
  description: string
}

definePageMeta({
  ssr: true,
})

const { t, te, locale, availableLocales } = useI18n()
const route = useRoute()
const runtimeConfig = useRuntimeConfig()
const requestURL = useRequestURL()
const localePath = useLocalePath()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const siteKey = computed(() => runtimeConfig.public.hcaptchaSiteKey ?? '')
const submitting = ref(false)
const success = ref(false)
const formError = ref<string | null>(null)

const MAX_SUBJECT_LENGTH = 180
const MAX_MESSAGE_LENGTH = 1200
const MAX_TITLE_KEY_LENGTH = 160

const normalizePrefillValue = (value: unknown, maxLength: number) => {
  if (typeof value !== 'string') {
    return ''
  }

  const trimmed = value.trim()

  if (!trimmed) {
    return ''
  }

  return trimmed.slice(0, maxLength)
}

const initialSubject = computed(() =>
  normalizePrefillValue(route.query.subject, MAX_SUBJECT_LENGTH)
)
const initialMessage = computed(() =>
  normalizePrefillValue(route.query.message, MAX_MESSAGE_LENGTH)
)
const prefillTitleKey = computed(() =>
  normalizePrefillValue(route.query.titleKey, MAX_TITLE_KEY_LENGTH)
)
const titleOverride = ref<string | null>(null)
const pageTitle = computed(() =>
  titleOverride.value ? titleOverride.value : String(t('contact.hero.title'))
)
const hasPrefill = computed(() =>
  Boolean(initialSubject.value || initialMessage.value || prefillTitleKey.value)
)

const linkedinUrl = computed(() => String(t('siteIdentity.links.linkedin')))

const heroHighlights = computed<HeroHighlight[]>(() => [
  {
    icon: 'mdi-account-heart-outline',
    text: String(t('contact.hero.highlights.commitment')),
  },
  {
    icon: 'mdi-leaf-circle-outline',
    text: String(t('contact.hero.highlights.impact')),
  },
])

const heroChannels = computed<HeroContactChannel[]>(() => [
  {
    icon: 'mdi-email-fast-outline',
    label: String(t('contact.hero.channels.support.label')),
    description: String(t('contact.hero.channels.support.description')),
  },
  {
    icon: 'mdi-account-group-outline',
    label: String(t('contact.hero.channels.partnerships.label')),
    description: String(t('contact.hero.channels.partnerships.description')),
  },
  {
    icon: 'mdi-shield-check-outline',
    label: String(t('contact.hero.channels.trust.label')),
    description: String(t('contact.hero.channels.trust.description')),
  },
])

const contactDetailItems = computed<ContactDetailItem[]>(() => [
  {
    icon: 'mdi-chat-question-outline',
    title: String(t('contact.details.cards.support.title')),
    description: String(t('contact.details.cards.support.description')),
    links: [
      {
        label: String(t('contact.details.cards.support.cta')),
        href: '#contact-form',
        ariaLabel: String(t('contact.details.cards.support.ctaAria')),
      },
    ],
  },
  {
    icon: 'mdi-handshake-outline',
    title: String(t('contact.details.cards.partnerships.title')),
    description: String(t('contact.details.cards.partnerships.description')),
    links: [
      {
        label: String(t('contact.details.cards.partnerships.cta')),
        href: localePath('team'),
        ariaLabel: String(t('contact.details.cards.partnerships.ctaAria')),
      },
    ],
  },
  {
    icon: 'mdi-earth',
    title: String(t('contact.details.cards.community.title')),
    description: String(t('contact.details.cards.community.description')),
    links: [
      {
        label: String(t('contact.details.cards.community.cta')),
        href: linkedinUrl.value,
        ariaLabel: String(t('contact.details.cards.community.ctaAria')),
      },
    ],
  },
])

const applyTitleOverride = () => {
  if (!prefillTitleKey.value) {
    titleOverride.value = null
    return
  }
  if (!te(prefillTitleKey.value)) {
    titleOverride.value = null
    return
  }
  titleOverride.value = String(t(prefillTitleKey.value))
}

onMounted(() => {
  applyTitleOverride()
})

watch(
  prefillTitleKey,
  () => {
    if (!import.meta.client) {
      return
    }
    applyTitleOverride()
  },
  { immediate: false }
)

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('contact', locale.value),
    requestURL.origin
  ).toString()
)

const siteName = computed(() => String(t('siteIdentity.siteName')))
const ogLocale = computed(() => locale.value.replace('-', '_'))
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)
const ogImageAlt = computed(() => String(t('contact.seo.imageAlt')))
const alternateLinks = computed(() =>
  availableLocales.map(availableLocale => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(
      resolveLocalizedRoutePath('contact', availableLocale),
      requestURL.origin
    ).toString(),
  }))
)

const fallbackErrorMessage = computed(() =>
  String(t('contact.form.feedback.genericError'))
)

const extractErrorMessage = (error: unknown): string => {
  if (error instanceof FetchError) {
    const data = error.data as {
      statusMessage?: string
      message?: string
    } | null

    if (data?.statusMessage) {
      return data.statusMessage
    }

    if (data?.message) {
      return data.message
    }

    if (error.message) {
      return error.message
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  if (typeof error === 'string' && error.length > 0) {
    return error
  }

  return fallbackErrorMessage.value
}

const handleResetFeedback = () => {
  success.value = false
  formError.value = null
}

const handleFormSubmit = async (payload: ContactFormPayload) => {
  submitting.value = true
  success.value = false
  formError.value = null

  try {
    const response = await $fetch<ContactResponseDto>('/api/contact', {
      method: 'POST',
      headers: {
        ...requestHeaders,
        'content-type': 'application/json',
      },
      body: payload,
    })

    const isSuccess = response?.success ?? true
    success.value = isSuccess

    if (!isSuccess) {
      formError.value = fallbackErrorMessage.value
    }
  } catch (error) {
    console.error('Contact submission failed', error)
    success.value = false
    formError.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

const contactPageJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'ContactPage',
  name: String(t('contact.seo.title')),
  description: String(t('contact.seo.description')),
  url: canonicalUrl.value,
  mainEntity: {
    '@type': 'Organization',
    name: siteName.value,
    url: new URL('/', requestURL.origin).toString(),
    logo: ogImageUrl.value,
    sameAs: [linkedinUrl.value].filter(Boolean),
  },
}))

useSeoMeta({
  title: () => String(t('contact.seo.title')),
  description: () => String(t('contact.seo.description')),
  ogTitle: () => String(t('contact.seo.title')),
  ogDescription: () => String(t('contact.seo.description')),
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
  script: [
    {
      key: 'contact-page-jsonld',
      type: 'application/ld+json',
      innerHTML: JSON.stringify(contactPageJsonLd.value),
    },
  ],
}))
</script>

<style scoped lang="sass">
.contact-page
  display: flex
  flex-direction: column
  gap: 0

.contact-hero__highlights
  display: grid
  gap: 0.75rem
  padding: 0
  margin: 0
  list-style: none

.contact-hero__highlight
  display: flex
  gap: 0.75rem
  align-items: flex-start

.contact-hero__highlight-icon
  flex-shrink: 0

.contact-hero__highlight-text
  font-size: 1.05rem
  line-height: 1.55

.contact-hero__card-overlay
  position: absolute
  inset: 0
  background: linear-gradient(180deg, rgba(var(--v-theme-hero-overlay-soft), 0.08) 0%, rgba(var(--v-theme-hero-overlay-soft), 0.02) 100%)
  pointer-events: none

.contact-hero__card-inner
  position: relative
  padding: clamp(1.75rem, 4vw, 2.6rem)
  display: flex
  flex-direction: column
  gap: 1.1rem

.contact-hero__badge
  display: inline-flex
  width: 3.5rem
  height: 3.5rem
  border-radius: 50%
  align-items: center
  justify-content: center
  background: rgba(var(--v-theme-hero-overlay-strong), 0.18)
  color: white

.contact-hero__card-title
  font-size: 1.35rem
  font-weight: 700
  margin: 0

.contact-hero__card-description
  font-size: 1rem
  opacity: 0.92
  margin: 0

.contact-hero__card-divider
  opacity: 0.2

.contact-hero__card-item
  transition: background-color 0.2s ease

  &:hover
    background-color: rgba(var(--v-theme-hero-overlay-soft), 0.08)
</style>
