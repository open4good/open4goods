<template>
  <div class="contact-page">
    <ContactHero
      :eyebrow="t('contact.hero.eyebrow')"
      :title="t('contact.hero.title')"
      :subtitle="t('contact.hero.subtitle')"
      :description="t('contact.hero.description')"
      :highlights="heroHighlights"
      :contact-channels="heroChannels"
      :channels-title="t('contact.hero.channels.title')"
      :channels-subtitle="t('contact.hero.channels.subtitle')"
    />

    <ContactDetailsSection
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
      @submit="handleFormSubmit"
      @reset-feedback="handleResetFeedback"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { FetchError } from 'ofetch'
import type { ContactResponseDto } from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type { ContactFormPayload } from '~/components/domains/contact/ContactFormCard.vue'
import type {
  HeroHighlight,
  HeroContactChannel,
} from '~/components/domains/contact/ContactHero.vue'
import type { ContactDetailItem } from '~/components/domains/contact/ContactDetailsSection.vue'

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const runtimeConfig = useRuntimeConfig()
const requestURL = useRequestURL()
const localePath = useLocalePath()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const siteKey = computed(() => runtimeConfig.public.hcaptchaSiteKey ?? '')
const submitting = ref(false)
const success = ref(false)
const formError = ref<string | null>(null)

const linkedinUrl = computed(() => String(t('siteIdentity.links.linkedin')))

const heroHighlights = computed<HeroHighlight[]>(() => [
  {
    icon: 'mdi-account-heart-outline',
    text: String(t('contact.hero.highlights.commitment')),
  },
  {
    icon: 'mdi-lightbulb-on-outline',
    text: String(t('contact.hero.highlights.expertise')),
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
      children: JSON.stringify(contactPageJsonLd.value),
    },
  ],
}))

</script>

<style scoped lang="sass">
.contact-page
  display: flex
  flex-direction: column
  gap: 0
</style>
