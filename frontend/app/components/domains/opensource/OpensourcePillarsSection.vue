<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface PillarCardAction {
  label: string
  href: string
  ariaLabel: string
  target?: string
  rel?: string
}

interface PillarCard {
  icon: string
  title: string
  descriptionBlocId: string
  action?: PillarCardAction
}

defineProps<{
  eyebrow: string
  title: string
  descriptionBlocId: string
  cards: PillarCard[]
}>()
</script>

<template>
  <section class="opensource-pillars" aria-labelledby="opensource-pillars-title">
    <v-container class="py-14">
      <div class="section-header">
        <v-chip
          v-if="eyebrow"
          label
          size="small"
          color="accent-supporting"
          class="section-chip"
        >
          {{ eyebrow }}
        </v-chip>

        <h2 id="opensource-pillars-title" class="section-title">{{ title }}</h2>
        <TextContent :bloc-id="descriptionBlocId" :ipsum-length="200" />
      </div>

      <v-row class="mt-6" align="stretch" dense>
        <v-col v-for="card in cards" :key="card.title" cols="12" md="4" class="d-flex">
          <v-card class="pillar-card" rounded="xl" elevation="6">
            <div class="pillar-icon">
              <v-icon :icon="card.icon" size="40" aria-hidden="true" />
            </div>
            <div class="pillar-content">
              <h3 class="pillar-title">{{ card.title }}</h3>
              <TextContent :bloc-id="card.descriptionBlocId" :ipsum-length="160" />
            </div>
            <div v-if="card.action" class="pillar-action">
              <v-btn
                :href="card.action.href"
                variant="text"
                color="primary"
                :aria-label="card.action.ariaLabel"
                :target="card.action.target"
                :rel="card.action.rel"
                append-icon="mdi-arrow-top-right"
              >
                {{ card.action.label }}
              </v-btn>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opensource-pillars
  background: rgba(var(--v-theme-surface-muted), 1)

.section-header
  max-width: 760px
  margin: 0 auto
  text-align: center
  display: flex
  flex-direction: column
  gap: 1rem

.section-chip
  align-self: center
  font-weight: 600
  letter-spacing: 0.08em

.section-title
  font-size: clamp(2rem, 3.5vw, 2.75rem)
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.pillar-card
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.5rem, 3vw, 2rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.95)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  box-shadow: 0 24px 48px rgba(var(--v-theme-shadow-primary-600), 0.1)

.pillar-icon
  width: 56px
  height: 56px
  border-radius: 16px
  background: rgba(var(--v-theme-surface-primary-080), 0.9)
  display: grid
  place-items: center
  color: rgba(var(--v-theme-accent-primary-highlight), 1)

.pillar-content
  flex: 1
  display: flex
  flex-direction: column
  gap: 0.75rem

.pillar-title
  font-size: 1.3rem
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.pillar-action
  margin-top: auto
</style>
