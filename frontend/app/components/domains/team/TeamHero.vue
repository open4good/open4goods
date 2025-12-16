<script setup lang="ts">
import { useId } from 'vue'
import TextContent from '~/components/domains/content/TextContent.vue'

interface Props {
  title: string
  subtitle: string
  descriptionBlocId: string
}

const props = defineProps<Props>()

const headingId = useId()
</script>

<template>
  <v-sheet
    class="team-hero"
    data-test="team-hero"
    role="region"
    :aria-labelledby="headingId"
  >
    <v-container class="py-12 px-4 text-center mx-auto" max-width="xl">
      <div class="team-hero__wrapper">
        <h1 :id="headingId" class="team-hero__title">{{ props.title }}</h1>
        <p class="team-hero__subtitle">{{ props.subtitle }}</p>

        <div class="team-hero__content">
          <TextContent :bloc-id="props.descriptionBlocId" :ipsum-length="220" />
        </div>
      </div>
    </v-container>
  </v-sheet>
</template>

<style scoped lang="sass">
.team-hero
  position: relative
  overflow: hidden
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.95), rgba(var(--v-theme-hero-gradient-end), 0.9))
  color: rgb(var(--v-theme-hero-pill-on-dark))

  &__wrapper
    margin: 0 auto
    display: flex
    flex-direction: column
    gap: 1rem


  &__title
    font-size: clamp(2.1rem, 5vw, 3rem)
    font-weight: 700
    margin: 0

  &__subtitle
    font-size: clamp(1.2rem, 2.5vw, 1.5rem)
    margin: 0
    opacity: 0.85

  &__content :deep(.text-content)
    padding: 0

  &__content :deep(.text-content),
  &__content :deep(.xwiki-sandbox)
    color: rgb(var(--v-theme-hero-pill-on-dark))

  &__content :deep(.xwiki-sandbox)
    font-size: clamp(1rem, 1.6vw, 1.15rem)
    line-height: 1.6
</style>
