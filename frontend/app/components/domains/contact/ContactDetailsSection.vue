<template>
  <section class="contact-details" aria-labelledby="contact-details-heading">
    <v-container class="py-12">
      <div class="contact-details__header">
        <p v-if="eyebrow" class="contact-details__eyebrow">{{ eyebrow }}</p>
        <h2 id="contact-details-heading" class="contact-details__title">
          {{ title }}
        </h2>
        <p class="contact-details__subtitle">
          {{ subtitle }}
        </p>
      </div>

      <v-row class="contact-details__cards" align="stretch" justify="center" role="list">
        <v-col
          v-for="item in items"
          :key="item.title"
          cols="12"
          md="4"
          class="d-flex"
          role="listitem"
        >
          <v-card
            elevation="4"
            rounded="xl"
            class="contact-details__card"
            variant="outlined"
          >
            <div class="contact-details__icon-wrapper">
              <v-avatar size="56" class="contact-details__icon">
                <v-icon :icon="item.icon" size="30" color="primary" />
              </v-avatar>
            </div>
            <div class="contact-details__content">
              <h3 class="contact-details__card-title">
                {{ item.title }}
              </h3>
              <p class="contact-details__card-description">
                {{ item.description }}
              </p>
              <div v-if="item.links?.length" class="contact-details__links">
                <v-btn
                  v-for="link in item.links"
                  :key="link.label"
                  :href="link.href"
                  :title="link.label"
                  :aria-label="link.ariaLabel || link.label"
                  target="_blank"
                  rel="noopener"
                  variant="tonal"
                  color="primary"
                  prepend-icon="mdi-open-in-new"
                >
                  {{ link.label }}
                </v-btn>
              </div>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
export interface ContactDetailLink {
  label: string
  href: string
  ariaLabel?: string
}

export interface ContactDetailItem {
  icon: string
  title: string
  description: string
  links?: ContactDetailLink[]
}

withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    subtitle: string
    items: ContactDetailItem[]
  }>(),
  {
    eyebrow: undefined,
  },
)
</script>

<style scoped lang="sass">
.contact-details
  background: linear-gradient(180deg, #ffffff 0%, rgba(245, 250, 255, 0.95) 100%)

  &__header
    text-align: center
    max-width: 52rem
    margin: 0 auto 3rem auto
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__eyebrow
    display: inline-flex
    align-self: center
    padding: 0.4rem 1rem
    border-radius: 999px
    background: rgba(25, 118, 210, 0.1)
    color: rgb(var(--v-theme-primary))
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.82rem

  &__title
    font-size: clamp(2rem, 3vw, 2.6rem)
    font-weight: 700
    margin: 0

  &__subtitle
    font-size: 1.05rem
    color: rgba(16, 24, 40, 0.7)
    margin: 0

  &__cards
    margin-top: 1.5rem

  &__card
    display: flex
    flex-direction: column
    gap: 1rem
    padding: 2rem
    background-color: rgba(255, 255, 255, 0.92)
    backdrop-filter: blur(6px)
    border: 1px solid rgba(25, 118, 210, 0.1)
    transition: transform 0.2s ease, box-shadow 0.2s ease

    &:hover
      transform: translateY(-6px)
      box-shadow: 0 18px 40px -24px rgba(25, 118, 210, 0.6)

  &__icon-wrapper
    display: flex
    justify-content: flex-start

  &__icon
    background-color: rgba(25, 118, 210, 0.12)

  &__content
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__card-title
    font-size: 1.35rem
    font-weight: 600
    margin: 0

  &__card-description
    color: rgba(16, 24, 40, 0.75)
    font-size: 1rem
    line-height: 1.6
    margin: 0

  &__links
    display: flex
    flex-wrap: wrap
    gap: 0.75rem

@media (max-width: 959px)
  .contact-details
    &__card
      text-align: center

    &__icon-wrapper
      justify-content: center

    &__links
      justify-content: center
