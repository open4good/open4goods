<script lang="ts" setup>
import type { RouteLocationRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'

import { useFooterLogoAsset } from '~~/app/composables/useThemedAsset'
import LatestReleaseBadge from '~/components/domains/releases/LatestReleaseBadge.vue'

import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'

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
const blogPath = computed(() =>
  resolveLocalizedRoutePath('blog', currentLocale.value)
)
const feedbackPath = computed(() =>
  resolveLocalizedRoutePath('feedback', currentLocale.value)
)
const categoriesPath = computed(() =>
  resolveLocalizedRoutePath('categories', currentLocale.value)
)
const releasesPath = computed(() =>
  resolveLocalizedRoutePath('releases', currentLocale.value)
)
const currentYear = computed(() => new Date().getFullYear())
const linkedinUrl = computed(() => String(t('siteIdentity.links.linkedin')))

const highlightLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.highlightLinks.ecoscore'),
    to: resolveLocalizedRoutePath('/impact-score', currentLocale.value),
    icon: 'mdi-leaf-circle-outline',
  },
  {
    label: t('siteIdentity.footer.highlightLinks.allProducts'),
    to: categoriesPath.value,
    icon: 'mdi-package-variant-closed',
  },
])

const resourceLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.comparator.links.openData'),
    to: '/opendata',
    icon: 'mdi-database-outline',
  },
  {
    label: t('siteIdentity.footer.comparator.links.openSource'),
    to: '/opensource',
    icon: 'mdi-github',
  },
  {
    label: t('siteIdentity.footer.comparator.links.privacy'),
    to: resolveLocalizedRoutePath('data-privacy', currentLocale.value),
    icon: 'mdi-shield-lock-outline',
  },
  {
    label: t('siteIdentity.footer.comparator.links.legal'),
    to: resolveLocalizedRoutePath('legal-notice', currentLocale.value),
    icon: 'mdi-scale-balance',
  },
])

const communityLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.community.links.team'),
    to: resolveLocalizedRoutePath('team', currentLocale.value),
    icon: 'mdi-account-group-outline',
  },
  {
    label: t('siteIdentity.footer.community.links.partners'),
    to: resolveLocalizedRoutePath('partners', currentLocale.value),
    icon: 'mdi-handshake-outline',
  },
])

const feedbackLinks = computed<FooterLink[]>(() => [
  {
    label: t('siteIdentity.footer.feedback.links.feedback'),
    to: feedbackPath.value,
    icon: 'mdi-message-text-outline',
  },
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

const footerLogo = useFooterLogoAsset()
</script>

<template>
  <v-container class="footer-container py-10" fluid>
    <h2 id="footer-heading" class="sr-only">
      {{ t('siteIdentity.footer.accessibleTitle') }}
    </h2>

    <v-row class="g-8 footer-upper">
      <v-col cols="12" md="3">
        <div class="footer-panel d-flex flex-column ga-4">
          <div class="footer-section-title text-subtitle-1 font-weight-medium">
            {{ t('siteIdentity.footer.feedback.title') }}
          </div>

          <div class="footer-action-title text-subtitle-1">
            <span>{{ t('siteIdentity.footer.feedback.cta') }}</span>
            <v-icon
              icon="mdi-arrow-right"
              size="18"
              class="ms-2"
              aria-hidden="true"
            />
          </div>

          <nav
            class="d-flex flex-column ga-1 mt-2"
            :aria-label="t('siteIdentity.footer.feedback.title')"
          >
            <ul class="footer-list">
              <li
                v-for="link in feedbackLinks"
                :key="String(link.to ?? link.href ?? link.label)"
                class="footer-list-item"
              >
                <NuxtLink
                  :to="link.to"
                  :href="link.href"
                  class="py-1 px-2 d-flex align-center"
                  :class="{ 'text-decoration-none': true }"
                  :target="link.target"
                  :rel="link.rel"
                >
                  <v-icon
                    v-if="link.icon"
                    :icon="link.icon"
                    size="18"
                    class="me-2"
                  />
                  <span class="text-body-2">{{ link.label }}</span>
                </NuxtLink>
              </li>
            </ul>
          </nav>
        </div>
      </v-col>

      <v-col cols="12" md="4">
        <div class="footer-panel d-flex flex-column ga-4">
          <div class="footer-action-title text-subtitle-1">
            <span>{{ t('siteIdentity.footer.highlightLinks.title') }}</span>
            <v-icon
              icon="mdi-arrow-right"
              size="18"
              class="ms-2"
              aria-hidden="true"
            />
          </div>
          <nav
            class="d-flex flex-column ga-1 mt-2"
            :aria-label="t('siteIdentity.footer.highlightLinks.title')"
          >
            <ul class="footer-list">
              <li
                v-for="link in highlightLinks"
                :key="String(link.to ?? link.href ?? link.label)"
                class="footer-list-item"
              >
                <NuxtLink
                  :to="link.to"
                  :href="link.href"
                  class="py-1 px-2 d-flex align-center"
                  :class="{ 'text-decoration-none': true }"
                >
                  <v-icon
                    v-if="link.icon"
                    :icon="link.icon"
                    size="18"
                    class="me-2"
                  />
                  <span class="text-body-2">{{ link.label }}</span>
                </NuxtLink>
              </li>
            </ul>
          </nav>

          <div class="footer-action-title text-subtitle-1">
            <span>{{ t('siteIdentity.footer.comparator.title') }}</span>
            <v-icon
              icon="mdi-arrow-right"
              size="18"
              class="ms-2"
              aria-hidden="true"
            />
          </div>
          <nav
            class="d-flex flex-column ga-1 mt-2"
            :aria-label="t('siteIdentity.footer.comparator.title')"
          >
            <ul class="footer-list">
              <li
                v-for="link in resourceLinks"
                :key="String(link.to ?? link.href ?? link.label)"
                class="footer-list-item"
              >
                <NuxtLink
                  :to="link.to"
                  :href="link.href"
                  class="py-1 px-2 d-flex align-center"
                  :class="{ 'text-decoration-none': true }"
                >
                  <v-icon
                    v-if="link.icon"
                    :icon="link.icon"
                    size="18"
                    class="me-2"
                  />
                  <span class="text-body-2">{{ link.label }}</span>
                </NuxtLink>
              </li>
            </ul>
          </nav>
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
            <template #prepend>
              <v-icon icon="mdi-post-outline" size="18" class="me-2" />
            </template>
            {{ t('siteIdentity.footer.blogLink') }}
          </v-btn>

          <div class="footer-section-title text-subtitle-1 font-weight-medium">
            {{ t('siteIdentity.footer.community.title') }}
          </div>
          <nav
            class="d-flex flex-column ga-1 mt-2"
            :aria-label="t('siteIdentity.footer.community.title')"
          >
            <ul class="footer-list">
              <li
                v-for="link in communityLinks"
                :key="String(link.to ?? link.href ?? link.label)"
                class="footer-list-item"
              >
                <NuxtLink
                  :to="link.to"
                  :href="link.href"
                  class="py-1 px-2 d-flex align-center"
                  :class="{ 'text-decoration-none': true }"
                >
                  <v-icon
                    v-if="link.icon"
                    :icon="link.icon"
                    size="18"
                    class="me-2"
                  />
                  <span class="text-body-2">{{ link.label }}</span>
                </NuxtLink>
              </li>
            </ul>
          </nav>
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
        <LatestReleaseBadge
          class="footer-latest-badge"
          dense
          :scroll-target="releasesPath"
        />
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

.footer-action-title {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: rgba(var(--v-theme-hero-overlay-strong), 0.98);
}

.footer-section-title {
  color: rgba(var(--v-theme-hero-overlay-strong), 0.88);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.footer-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-list-item {
  min-height: 32px;
  border-radius: 12px;
  transition: background-color 0.2s ease;
  color: inherit;
  text-decoration: none;
}

.footer-list-item > a {
  border-radius: 12px;
  color: inherit;
  text-decoration: none;
}

.footer-list-item:hover > a {
  background-color: rgba(var(--v-theme-hero-overlay-soft), 0.1);
  text-decoration: none;
}

.footer-bottom {
  position: relative;
  z-index: 1;
}

.footer-latest-badge {
  align-self: center;
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
