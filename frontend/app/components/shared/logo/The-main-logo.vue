<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { useLogoAsset } from '~~/app/composables/useThemedAsset'

const { t } = useI18n()

const logoSrc = useLogoAsset()
</script>

<template>
  <NuxtLink id="main-logo-link" to="/" class="logo-link" :aria-label="t('siteIdentity.logoAlt')">
    <img :src="logoSrc" :alt="t('siteIdentity.logoAlt')" class="main-logo" loading="lazy" />
    <div class="logo-text">
      <span class="logo-title">{{ t('siteIdentity.title') }}</span>
      <span class="logo-subtitle">{{ t('siteIdentity.subtitle') }}</span>
    </div>
  </NuxtLink>
</template>

<style scoped lang="sass">
.logo-link
  display: inline-flex
  align-items: center
  gap: 0.5rem
  text-decoration: none
  color: inherit
  padding: 0.25rem 0.5rem
  border-radius: 0.75rem
  transition: background-color 0.2s ease, transform 0.2s ease

  &:hover,
  &:focus-visible
    background-color: rgba(var(--v-theme-surface-primary-080), 0.7)
    transform: translateY(-1px)

.main-logo
  height: 2.5rem
  width: 2.5rem
  min-width: 2.5rem
  display: block
  transition: opacity 0.2s ease

  &:hover
    opacity: 0.8

.logo-text
  display: flex
  flex-direction: column
  justify-content: center
  line-height: 1.1

.logo-title
  font-weight: 700
  font-size: 1rem
  letter-spacing: 0.01em

.logo-subtitle
  font-size: 0.8rem
  color: rgb(var(--v-theme-text-neutral-secondary))

@media (min-width: 960px)
  .logo-link
    gap: 0.75rem

  .main-logo
    height: 2.75rem
    width: 2.75rem

  .logo-title
    font-size: 1.1rem

  .logo-subtitle
    font-size: 0.9rem
</style>
