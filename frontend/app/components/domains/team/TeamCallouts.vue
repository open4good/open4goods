<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface Props {
  partnersBlocId: string
  partnersTitle: string
  partnersCta: string
  partnersLink: string
  contactTitle: string
  contactDescription: string
  contactCta: string
  contactLink: string
}

const props = defineProps<Props>()
</script>

<template>
  <section
    class="team-callouts"
    role="region"
    aria-labelledby="team-callouts-title"
  >
    <v-container class="py-12 px-4 mx-auto" max-width="xl">
      <v-row class="g-6" align="stretch">
        <v-col cols="12" lg="8">
          <v-card class="team-callouts__card" elevation="6" rounded="xl">
            <v-card-item>
              <h3 id="team-callouts-title" class="team-callouts__title">
                {{ props.partnersTitle }}
              </h3>
            </v-card-item>
            <v-card-text class="team-callouts__text">
              <TextContent
                :bloc-id="props.partnersBlocId"
                :ipsum-length="120"
              />
            </v-card-text>
            <v-card-actions>
              <v-btn
                :href="props.partnersLink"
                color="primary"
                variant="elevated"
                size="large"
              >
                {{ props.partnersCta }}
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>

        <v-col cols="12" lg="4">
          <v-card
            class="team-callouts__card team-callouts__card--accent"
            elevation="4"
            rounded="xl"
          >
            <v-card-item>
              <h3 class="team-callouts__title team-callouts__title--accent">
                {{ props.contactTitle }}
              </h3>
            </v-card-item>
            <v-card-text class="team-callouts__description">
              {{ props.contactDescription }}
            </v-card-text>
            <v-card-actions>
              <v-btn
                :href="props.contactLink"
                color="secondary"
                variant="tonal"
                size="large"
              >
                {{ props.contactCta }}
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.team-callouts
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-callout-start), 0.7), rgba(var(--v-theme-surface-callout-end), 0.75))

  &__card
    height: 100%
    display: flex
    flex-direction: column
    justify-content: space-between
    padding: clamp(1.5rem, 3vw, 2.5rem)
    gap: 1.5rem

  &__card--accent
    background: linear-gradient(135deg, rgba(var(--v-theme-accent-callout), 0.1), rgba(var(--v-theme-accent-callout), 0.2))
    color: rgb(var(--v-theme-text-on-accent))

  &__title
    font-size: clamp(1.6rem, 2.3vw, 2rem)
    font-weight: 700
    margin: 0

  &__title--accent
    color: inherit

  &__text :deep(.text-content)
    padding: 0

  &__text :deep(.xwiki-sandbox)
    font-size: 1.05rem
    line-height: 1.7

  &__description
    font-size: 1.05rem
    line-height: 1.7
</style>
