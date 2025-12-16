<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { useLogoAsset } from '~~/app/composables/useThemedAsset'

const { t } = useI18n()

const logoSrc = useLogoAsset()
</script>

<template>
  <NuxtLink id="main-logo-link" to="/" class="logo-link">
    <img :src="logoSrc" :alt="t('siteIdentity.logoAlt')" class="main-logo" />
  </NuxtLink>
</template>

<style scoped lang="sass">
.logo-link
  display: inline-block
  text-decoration: none

.main-logo
  height: auto
  max-width: 100%
  display: block
  transition: opacity 0.2s ease

  &:hover
    opacity: 0.8
</style>
