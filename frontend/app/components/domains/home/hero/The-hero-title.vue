<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="hero-title-container">
    <span class="hero-title-eyebrow">
      {{ t('siteIdentity.hero.mainSubtitle') }}
    </span>
    <h1 class="hero-title-text">
      {{ t('siteIdentity.hero.mainTitle') }}
    </h1>
    <div class="pt-2">
      <TheSearchBar />
    </div>
  </div>
</template>

<style scoped lang="sass">
.hero-title-container
  position: absolute
  padding: 20px
  top: 50%
  left: 34%
  width: 66%
  transform: translate(-50%, -50%)
  z-index: 10
  text-align: left


.hero-title-text
    color: white

.hero-title-eyebrow
    display: block
    font-size: 1.125rem
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    color: rgba(255, 255, 255, 0.82)

.hero-title-text
    font-size: clamp(1.5rem, 6vw, 4rem)
    font-weight: 700
</style>
