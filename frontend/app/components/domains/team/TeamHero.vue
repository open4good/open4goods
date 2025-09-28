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
    color="primary"
    data-test="team-hero"
    role="region"
    :aria-labelledby="headingId"
  >
    <v-container class="py-12 text-center">
      <div class="team-hero__wrapper">

        <h1 :id="headingId" class="team-hero__title text-white">{{ props.title }}</h1>
        <p class="team-hero__subtitle text-white">{{ props.subtitle }}</p>

        <div class="team-hero__content text-white text-body-1">
          <TextContent :bloc-id="props.descriptionBlocId" :ipsum-length="180" />
        </div>
      </div>
    </v-container>
  </v-sheet>
</template>

<style scoped lang="sass">
.team-hero
  position: relative
  overflow: hidden
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.95), rgba(67, 160, 71, 0.9))
  color: white

  &__wrapper
    max-width: 720px
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

  &__content :deep(.xwiki-sandbox)
    font-size: clamp(1rem, 1.6vw, 1.15rem)
    line-height: 1.6
</style>
