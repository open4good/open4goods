<script lang="ts" setup>
import { useI18n } from 'vue-i18n'

import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'

type FooterLink = {
  label: string
  href: string
  target?: string
  rel?: string
}

const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))
const blogPath = computed(() =>
  resolveLocalizedRoutePath('blog', currentLocale.value)
)

const currentYear = computed(() => new Date().getFullYear())
const linkedinUrl = computed(() => String(t('siteIdentity.links.linkedin')))

const highlightLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.highlightLinks.ecoscore'),
    href: resolveLocalizedRoutePath('ecoscore', currentLocale.value),
  },
])

const comparatorLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.comparator.links.openData'),
    href: '/opendata',
  },
  {
    label: t('siteIdentity.footer.comparator.links.openSource'),
    href: '/opensource',
  },
  {
    label: t('siteIdentity.footer.comparator.links.privacy'),
    href: '/politique-confidentialite',
  },
  {
    label: t('siteIdentity.footer.comparator.links.legal'),
    href: '/mentions-legales',
  },
])

const communityLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.community.links.team'),
    href: resolveLocalizedRoutePath('team', currentLocale.value),
  },
  {
    label: t('siteIdentity.footer.community.links.partners'),
    href: '/partenaires',
  },
])

const feedbackLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.feedback.links.idea'),
    href: '/feedback/idea',
  },
  {
    label: t('siteIdentity.footer.feedback.links.issue'),
    href: '/feedback/issue',
  },
  {
    label: t('siteIdentity.footer.feedback.links.contact'),
    href: resolveLocalizedRoutePath('contact', currentLocale.value),
  },
  {
    label: t('siteIdentity.footer.feedback.links.linkedin'),
    href: linkedinUrl.value,
    target: '_blank',
    rel: 'nofollow noopener',
  },
])
</script>

<template>
  <v-container class="footer-container py-10" fluid>
    <h2 id="footer-heading" class="sr-only">
      {{ t('siteIdentity.footer.accessibleTitle') }}
    </h2>

    <v-row class="g-8">
      <v-col cols="12" md="4" class="d-flex flex-column ga-4">
        <p class="footer-mission text-body-1 mb-0">
          {{ t('siteIdentity.footer.mission') }}
        </p>

        <v-btn
          :href="blogPath"
          variant="text"
          append-icon="mdi-arrow-right"
          class="footer-link-btn text-white px-0"
        >
          {{ t('siteIdentity.footer.blogLink') }}
        </v-btn>
      </v-col>

      <v-col cols="12" md="4">
        <div class="d-flex flex-column ga-2">
          <v-btn
            v-for="link in highlightLinks"
            :key="link.href"
            :href="link.href"
            variant="text"
            append-icon="mdi-arrow-right"
            class="footer-link-btn text-white px-0"
          >
            {{ link.label }}
          </v-btn>
        </div>

        <div class="text-subtitle-1 font-weight-medium mt-6">
          {{ t('siteIdentity.footer.comparator.title') }}
        </div>
        <v-list
          density="compact"
          bg-color="transparent"
          class="footer-list pa-0 mt-2"
        >
          <v-list-item
            v-for="link in comparatorLinks"
            :key="link.href"
            :href="link.href"
            class="footer-list-item px-0"
          >
            <template #title>
              <span class="text-body-2">{{ link.label }}</span>
            </template>
          </v-list-item>
        </v-list>
      </v-col>

      <v-col cols="12" md="4">
        <div>
          <div class="text-subtitle-1 font-weight-medium">
            {{ t('siteIdentity.footer.community.title') }}
          </div>
          <v-list
            density="compact"
            bg-color="transparent"
            class="footer-list pa-0 mt-2"
          >
            <v-list-item
              v-for="link in communityLinks"
              :key="link.href"
              :href="link.href"
              class="footer-list-item px-0"
            >
              <template #title>
                <span class="text-body-2">{{ link.label }}</span>
              </template>
            </v-list-item>
          </v-list>
        </div>

        <div class="text-subtitle-1 font-weight-medium mt-6">
          {{ t('siteIdentity.footer.feedback.title') }}
        </div>
        <v-list
          density="compact"
          bg-color="transparent"
          class="footer-list pa-0 mt-2"
        >
          <v-list-item
            v-for="link in feedbackLinks"
            :key="link.href"
            :href="link.href"
            class="footer-list-item px-0"
            :target="link.target"
            :rel="link.rel"
          >
            <template #title>
              <span class="text-body-2">{{ link.label }}</span>
            </template>
          </v-list-item>
        </v-list>
      </v-col>
    </v-row>

    <v-divider class="my-8" opacity="0.2" color="white" />

    <v-row class="justify-center text-center">
      <v-col cols="12" class="d-flex flex-column align-center ga-4">
        <NuxtLink to="/" class="footer-logo-link d-inline-flex">
          <v-img
            src="@/assets/images/nudger-logo-orange.svg"
            :alt="t('siteIdentity.footer.logoAlt')"
            height="40"
            class="footer-logo"
            cover
          />
        </NuxtLink>
        <p class="mb-0 text-body-2">
          {{ t('siteIdentity.footer.copyright', { year: currentYear }) }}
        </p>
      </v-col>
    </v-row>
  </v-container>
</template>

<style lang="postcss" scoped>
.footer-container {
  max-width: 1200px;
}

.footer-mission {
  line-height: 1.6;
}

.footer-link-btn {
  justify-content: flex-start;
  text-transform: none;
  font-weight: 600;
}

.footer-list :deep(.v-list-item-title) {
  color: inherit;
}

.footer-list-item {
  min-height: 32px;
}

.footer-logo {
  max-width: 160px;
}

.footer-logo-link {
  transition: opacity 0.2s ease;
}

.footer-logo-link:hover {
  opacity: 0.85;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
