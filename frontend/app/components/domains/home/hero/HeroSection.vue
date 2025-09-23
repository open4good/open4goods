<script setup lang="ts">
const videoDemo1 = '/videos/video-concept1.mp4'

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
            {{ title || 'Titre par défaut' }}
          </template>
          <template #subtitle>
            {{ subtitle || 'Démo vidéo hero pour montrer une intégration propre dans une application Vue 3.' }}
          </template>
          <template #cta>
            <a :href="ctaUrl || '#'">
              {{ ctaText || 'Appel à l\'action' }}
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
