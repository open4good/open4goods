<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const videoDemo1 = '/videos/video-concept1.mp4'
const { t } = useI18n()

defineProps<{
  title?: string
  subtitle?: string
  ctaText?: string
  ctaUrl?: string
}>()

defineEmits<{
  'toggle-drawer': []
}>()
</script>

<template>
  <v-container fluid class="align-start container-video">
    <v-row class="fill-height">
      <v-col cols="12">
        <The-hero-video :video-src="videoDemo1">
          <template #title>
            {{ title || t('siteIdentity.hero.defaultTitle') }}
          </template>
          <template #subtitle>
            {{ subtitle || t('siteIdentity.hero.defaultSubtitle') }}
          </template>
          <template #cta>
            <a :href="ctaUrl || '#'">
              {{ ctaText || t('siteIdentity.hero.defaultCtaText') }}
            </a>
          </template>
        </The-hero-video>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="sass">
.container-video
  margin-top: 14vh
</style>
