<script lang="ts" setup>
import type { RouteLocationRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'

import { normalizeLocale, resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

type FooterLink = {
  label: string
  to?: RouteLocationRaw
  href?: string
  target?: string
  rel?: string
  icon?: string
}

const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))
const blogPath = computed(() => resolveLocalizedRoutePath('blog', currentLocale.value))
const feedbackPath = computed(() => resolveLocalizedRoutePath('feedback', currentLocale.value))
const categoriesPath = computed(() => resolveLocalizedRoutePath('categories', currentLocale.value))

const currentYear = computed(() => new Date().getFullYear())
const linkedinUrl = computed(() => String(t('siteIdentity.links.linkedin')))

const highlightLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.highlightLinks.ecoscore'),
    to: resolveLocalizedRoutePath('/impact-score', currentLocale.value),
  },
  {
    label: t('siteIdentity.footer.highlightLinks.allProducts'),
    to: categoriesPath.value,
  },
])

const resourceLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.comparator.links.openData'),
    to: '/opendata',
  },
  {
    label: t('siteIdentity.footer.comparator.links.openSource'),
    to: '/opensource',
  },
  {
    label: t('siteIdentity.footer.comparator.links.privacy'),
    to: resolveLocalizedRoutePath('data-privacy', currentLocale.value),
  },
  {
    label: t('siteIdentity.footer.comparator.links.legal'),
    to: resolveLocalizedRoutePath('legal-notice', currentLocale.value),
  },
])

const communityLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.community.links.team'),
    to: resolveLocalizedRoutePath('team', currentLocale.value),
  },
  {
    label: t('siteIdentity.footer.community.links.partners'),
    to: resolveLocalizedRoutePath('partners', currentLocale.value),
  },
])

const feedbackLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.feedback.links.contact'),
    to: resolveLocalizedRoutePath('contact', currentLocale.value),
    icon: 'mdi-email-outline',
  },
  {
    label: t('siteIdentity.footer.feedback.links.linkedin'),
    href: linkedinUrl.value,
    target: '_blank',
    rel: 'noopener nofollow',
    icon: 'mdi-linkedin',
  },
])

const footerLogo = new URL('../../../assets/images/nudger-logo-orange.svg', import.meta.url).href

</script>

<template>
  <v-container class="footer-container py-10" fluid>
    <h2 id="footer-heading" class="sr-only">
      {{ t('siteIdentity.footer.accessibleTitle') }}
    </h2>

    <v-row class="g-8 footer-upper">
      <v-col cols="12" md="4">
        <div class="footer-panel d-flex flex-column ga-4">
          <div class="footer-section-title text-subtitle-1 font-weight-medium">
            {{ t('siteIdentity.footer.feedback.title') }}
          </div>

          <v-btn
            :to="feedbackPath"
            variant="text"
            append-icon="mdi-arrow-right"
            color="hero-overlay-strong"
            class="footer-link-btn px-0"
          >
            {{ t('siteIdentity.footer.feedback.cta') }}
          </v-btn>

          <v-list density="compact" bg-color="transparent" class="footer-list pa-0">
            <v-list-item
              v-for="link in feedbackLinks"
              :key="String(link.to ?? link.href ?? link.label)"
              :to="link.to"
              :href="link.href"
              class="footer-list-item px-0"
              :target="link.target"
              :rel="link.rel"
            >
              <template #prepend>
                <v-icon v-if="link.icon" :icon="link.icon" size="18" class="me-2" />
              </template>
              <template #title>
                <span class="text-body-2">{{ link.label }}</span>
              </template>
            </v-list-item>
          </v-list>
        </div>
      </v-col>

      <v-col cols="12" md="4">
        <div class="footer-panel d-flex flex-column ga-4">
          <div class="d-flex flex-column ga-2">
            <v-btn
              v-for="link in highlightLinks"
              :key="String(link.to ?? link.href ?? link.label)"
              :to="link.to"
              :href="link.href"
              variant="text"
              append-icon="mdi-arrow-right"
              color="hero-overlay-strong"
              class="footer-link-btn px-0"
            >
              {{ link.label }}
            </v-btn>
          </div>

          <div class="footer-section-title text-subtitle-1 font-weight-medium">
            {{ t('siteIdentity.footer.comparator.title') }}
          </div>
          <v-list density="compact" bg-color="transparent" class="footer-list pa-0 mt-2">
            <v-list-item
              v-for="link in resourceLinks"
              :key="String(link.to ?? link.href ?? link.label)"
              :to="link.to"
              :href="link.href"
              class="footer-list-item px-0"
            >
              <template #title>
                <span class="text-body-2">{{ link.label }}</span>
              </template>
            </v-list-item>
          </v-list>
        </div>
      </v-col>

      <v-col cols="12" md="4">
        <div class="footer-panel d-flex flex-column ga-4">
          <p class="footer-mission text-body-1 mb-0">
            {{ t('siteIdentity.footer.mission') }}
          </p>

          <v-btn
            :to="blogPath"
            variant="text"
            append-icon="mdi-arrow-right"
            color="hero-overlay-strong"
            class="footer-link-btn px-0"
          >
            {{ t('siteIdentity.footer.blogLink') }}
          </v-btn>

          <div class="footer-section-title text-subtitle-1 font-weight-medium">
            {{ t('siteIdentity.footer.community.title') }}
          </div>
          <v-list density="compact" bg-color="transparent" class="footer-list pa-0 mt-2">
            <v-list-item
              v-for="link in communityLinks"
              :key="String(link.to ?? link.href ?? link.label)"
              :to="link.to"
              :href="link.href"
              class="footer-list-item px-0"
            >
              <template #title>
                <span class="text-body-2">{{ link.label }}</span>
              </template>
            </v-list-item>
          </v-list>
        </div>
      </v-col>
    </v-row>

    <v-divider class="my-8 footer-divider" color="hero-overlay-strong" />

    <v-row class="justify-center text-center footer-bottom">
      <v-col cols="12" class="d-flex flex-column align-center ga-4">
        <NuxtLink to="/" class="footer-logo-link d-inline-flex">
          <v-img
            :src="footerLogo"
            :alt="t('siteIdentity.footer.logoAlt')"
            height="40"
            class="footer-logo"
            cover
          />
        </NuxtLink>
        <p class="footer-meta mb-0 text-body-2">
          {{ t('siteIdentity.footer.copyright', { year: currentYear }) }}
        </p>
      </v-col>
    </v-row>
  </v-container>
</template>

<style lang="postcss" scoped>
  .footer-container {
    max-width: none;
    width: 100%;
    padding-inline: clamp(24px, 6vw, 96px);
    color: rgb(var(--v-theme-hero-overlay-strong));
  }

  .footer-upper {
    position: relative;
    z-index: 1;
  }

  .footer-panel {
    padding: 24px;
    border-radius: 20px;
    background: rgba(var(--v-theme-hero-overlay-soft), 0.08);
    border: 1px solid rgba(var(--v-theme-hero-overlay-soft), 0.16);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    box-shadow: 0 18px 42px -24px rgba(var(--v-theme-shadow-primary-600), 0.35);
    height: 100%;
  }

  .footer-mission {
    line-height: 1.6;
    color: rgba(var(--v-theme-hero-overlay-strong), 0.96);
  }

  .footer-link-btn {
    justify-content: flex-start;
    text-transform: none;
    font-weight: 600;
    letter-spacing: 0.01em;
  }

  .footer-link-btn :deep(.v-btn__append) {
    opacity: 0.9;
  }

  .footer-section-title {
    color: rgba(var(--v-theme-hero-overlay-strong), 0.88);
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }

  .footer-list :deep(.v-list-item-title) {
    color: rgba(var(--v-theme-hero-overlay-strong), 0.82);
  }

  .footer-list-item {
    min-height: 32px;
    border-radius: 12px;
    transition: background-color 0.2s ease;
  }

  .footer-list-item:hover {
    background-color: rgba(var(--v-theme-hero-overlay-soft), 0.1);
  }

  .footer-divider {
    opacity: 0.24 !important;
  }

  .footer-bottom {
    position: relative;
    z-index: 1;
  }

  .footer-logo {
    max-width: 160px;
    filter: drop-shadow(0 8px 18px rgba(var(--v-theme-shadow-primary-600), 0.45));
  }

  .footer-logo-link {
    transition: opacity 0.2s ease;
  }

  .footer-logo-link:hover {
    opacity: 0.85;
  }

  .footer-meta {
    color: rgba(var(--v-theme-hero-overlay-strong), 0.72);
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
