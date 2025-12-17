<template>
  <HeroSurface
    tag="section"
    class="contact-hero"
    aria-labelledby="contact-hero-heading"
    variant="pulse"
  >
    <v-container class="py-12">
      <v-row align="center" class="g-6" justify="space-between">
        <v-col cols="12" md="7" class="contact-hero__content">
          <p
            v-if="eyebrow"
            class="contact-hero__eyebrow"
            data-testid="contact-hero-eyebrow"
          >
            {{ eyebrow }}
          </p>

          <h1 id="contact-hero-heading" class="contact-hero__title">
            {{ title }}
          </h1>

          <p class="contact-hero__subtitle">
            {{ subtitle }}
          </p>

          <p class="contact-hero__description">
            {{ description }}
          </p>

          <ul class="contact-hero__highlights" role="list">
            <li
              v-for="item in highlights"
              :key="item.text"
              class="contact-hero__highlight"
            >
              <v-icon
                :icon="item.icon"
                size="26"
                class="contact-hero__highlight-icon"
              />
              <span class="contact-hero__highlight-text">
                {{ item.text }}
              </span>
            </li>
          </ul>
        </v-col>

        <v-col cols="12" md="5">
          <v-card
            class="contact-hero__card"
            elevation="10"
            rounded="xl"
            color="primary"
            theme="dark"
            aria-labelledby="contact-hero-card-heading"
            role="region"
          >
            <div class="contact-hero__card-overlay" aria-hidden="true" />
            <div class="contact-hero__card-inner">
              <div class="contact-hero__badge">
                <v-icon
                  icon="mdi-message-badge-outline"
                  size="34"
                  aria-hidden="true"
                />
              </div>
              <h2
                id="contact-hero-card-heading"
                class="contact-hero__card-title"
              >
                {{ channelsTitle }}
              </h2>
              <p class="contact-hero__card-description">
                {{ channelsSubtitle }}
              </p>
              <v-divider class="contact-hero__card-divider" />
              <v-list
                class="contact-hero__card-list"
                bg-color="transparent"
                density="comfortable"
                lines="one"
                role="list"
              >
                <v-list-item
                  v-for="channel in contactChannels"
                  :key="channel.label"
                  :title="channel.label"
                  :subtitle="channel.description"
                  class="contact-hero__card-item"
                  role="listitem"
                >
                  <template #prepend>
                    <v-avatar color="white" size="40">
                      <v-icon :icon="channel.icon" color="primary" size="24" />
                    </v-avatar>
                  </template>
                </v-list-item>
              </v-list>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </HeroSurface>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'

export interface HeroHighlight {
  icon: string
  text: string
}

export interface HeroContactChannel {
  icon: string
  label: string
  description: string
}

const props = defineProps<{
  eyebrow?: string
  title: string
  subtitle: string
  description: string
  highlights: HeroHighlight[]
  contactChannels: HeroContactChannel[]
  channelsTitle: string
  channelsSubtitle: string
}>()

const {
  eyebrow,
  title,
  subtitle,
  description,
  highlights,
  contactChannels,
  channelsTitle,
  channelsSubtitle,
} = toRefs(props)
</script>

<style scoped lang="sass">
.contact-hero
  position: relative
  color: rgb(255, 255, 255)
  overflow: hidden

  &__content
    position: relative
    z-index: 1

  &__eyebrow
    display: inline-flex
    align-items: center
    padding: 0.375rem 0.75rem
    border-radius: 999px
    background-color: rgba(var(--v-theme-hero-pill-on-dark), 0.16)
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    margin-bottom: 1.25rem
    font-size: 0.85rem

  &__title
    font-weight: 700
    font-size: clamp(2.5rem, 4vw, 3.4rem)
    margin-bottom: 0.75rem
    line-height: 1.1

  &__subtitle
    font-size: clamp(1.2rem, 2.2vw, 1.5rem)
    font-weight: 600
    margin-bottom: 0.75rem

  &__description
    max-width: 48rem
    font-size: 1.05rem
    line-height: 1.7
    opacity: 0.92
    margin-bottom: 1.75rem

  &__highlights
    display: grid
    gap: 0.75rem
    padding: 0
    margin: 0
    list-style: none

  &__highlight
    display: flex
    gap: 0.75rem
    align-items: flex-start

  &__highlight-icon
    flex-shrink: 0

  &__highlight-text
    font-size: 1.05rem
    line-height: 1.55

  &__card
    position: relative
    overflow: hidden

  &__card-overlay
    position: absolute
    inset: 0
    background: linear-gradient(180deg, rgba(var(--v-theme-hero-overlay-soft), 0.08) 0%, rgba(var(--v-theme-hero-overlay-soft), 0.02) 100%)
    pointer-events: none

  &__card-inner
    position: relative
    padding: clamp(1.75rem, 4vw, 2.6rem)
    display: flex
    flex-direction: column
    gap: 1.1rem

  &__badge
    display: inline-flex
    width: 3.5rem
    height: 3.5rem
    border-radius: 50%
    align-items: center
    justify-content: center
    background: rgba(var(--v-theme-hero-overlay-strong), 0.18)
    color: white

  &__card-title
    font-size: 1.35rem
    font-weight: 700
    margin: 0

  &__card-description
    font-size: 1rem
    opacity: 0.92
    margin: 0

  &__card-divider
    opacity: 0.2

  &__card-list
    padding: 0

  &__card-item
    border-radius: 12px
    transition: background-color 0.2s ease

    &:hover
      background-color: rgba(var(--v-theme-hero-overlay-soft), 0.08)

@media (max-width: 959px)
  .contact-hero
    text-align: center

    &__highlights
      justify-items: center

    &__highlight
      justify-content: center

    &__card
      margin-top: 2.5rem

    &__card-inner
      align-items: center
      text-align: center

    &__card-list
      width: 100%

    &__card-item
      width: 100%
      justify-content: center

    &__card-title
      font-size: 1.5rem
</style>
