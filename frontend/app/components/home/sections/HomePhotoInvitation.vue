<script setup lang="ts">
import { computed } from 'vue'
import { useLocalePath } from '#i18n'

const props = withDefaults(
  defineProps<{
    imageSrc?: string
    imageAlt?: string
  }>(),
  {
    imageSrc: '',
    imageAlt: '',
  }
)

const { t } = useI18n()
const localePath = useLocalePath()

const title = computed(() => t('home.photoInvitation.title'))
const ariaLabel = computed(() => t('home.photoInvitation.ariaLabel'))
const contactSubject = computed(() => t('home.photoInvitation.contact.subject'))
const contactMessage = computed(() => t('home.photoInvitation.contact.message'))
const resolvedAlt = computed(
  () => props.imageAlt || t('home.photoInvitation.imageAlt')
)

const contactLink = computed(() => ({
  path: localePath('contact'),
  query: {
    subject: contactSubject.value,
    message: contactMessage.value,
  },
}))
</script>

<template>
  <NuxtLink
    :to="contactLink"
    class="home-photo-invitation"
    :aria-label="ariaLabel"
  >
    <v-card
      class="home-photo-invitation__card"
      variant="tonal"
      rounded="xl"
      elevation="0"
    >
      <v-img
        v-if="props.imageSrc"
        :src="props.imageSrc"
        :alt="resolvedAlt"
        class="home-photo-invitation__image"
        cover
        height="220"
        width="320"
        loading="lazy"
      />
      <div class="home-photo-invitation__content">
        <p class="home-photo-invitation__title">{{ title }}</p>
      </div>
    </v-card>
  </NuxtLink>
</template>

<style scoped lang="sass">
.home-photo-invitation
  text-decoration: none
  color: inherit
  width: min(100%, 360px)
  display: block

.home-photo-invitation__card
  background: rgba(var(--v-theme-surface-glass), 0.8)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
  transition: transform 0.2s ease, box-shadow 0.2s ease

  &:hover
    transform: translateY(-4px)
    box-shadow: 0 18px 32px rgba(var(--v-theme-shadow-primary-600), 0.18)

.home-photo-invitation__image
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.home-photo-invitation__content
  padding: 1rem 1.25rem 1.25rem
  text-align: center

.home-photo-invitation__title
  margin: 0
  font-size: 1.05rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))
</style>
