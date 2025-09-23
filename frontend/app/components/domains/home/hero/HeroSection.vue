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
  <v-container fluid>
    <v-row>
      <v-col cols="3">
        <the-main-logo />
      </v-col>
      <v-col cols="9">
        <the-hero-menu @toggle-drawer="$emit('toggle-drawer')" />
      </v-col>
    </v-row>
  </v-container>
  <v-container fluid class="fill-height align-start pt-0">
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

  <The-items-selector />

  <Bloc3Hz />
</template>

<style scoped lang="sass"></style>
