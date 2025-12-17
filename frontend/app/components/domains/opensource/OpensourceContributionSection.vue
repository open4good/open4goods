<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface ContributionStep {
  title: string
  descriptionBlocId: string
  icon: string
}

defineProps<{
  eyebrow: string
  title: string
  descriptionBlocId: string
  steps: ContributionStep[]
}>()
</script>

<template>
  <section
    class="opensource-contribution"
    aria-labelledby="opensource-contribution-title"
  >
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
        <h2 id="opensource-contribution-title" class="section-title">
          {{ title }}
        </h2>
        <TextContent :bloc-id="descriptionBlocId" :ipsum-length="200" />
      </div>

      <div class="steps-grid" role="list">
        <article
          v-for="(step, index) in steps"
          :key="step.title"
          class="step-card"
          role="listitem"
        >
          <header class="step-header">
            <div class="step-index" aria-hidden="true">{{ index + 1 }}</div>
            <div class="step-icon">
              <v-icon :icon="step.icon" size="28" aria-hidden="true" />
            </div>
          </header>
          <h3 class="step-title">{{ step.title }}</h3>
          <TextContent :bloc-id="step.descriptionBlocId" :ipsum-length="150" />
        </article>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opensource-contribution
  background: rgba(var(--v-theme-surface-alt), 1)

.section-header
  max-width: 720px
  margin: 0 auto 3rem
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

.steps-grid
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr))
  gap: 1.5rem

.step-card
  background: rgba(var(--v-theme-surface-default), 1)
  border-radius: 24px
  padding: clamp(1.75rem, 3vw, 2.25rem)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  box-shadow: 0 18px 36px rgba(var(--v-theme-shadow-primary-600), 0.08)
  display: flex
  flex-direction: column
  gap: 1rem

.step-header
  display: flex
  align-items: center
  gap: 1rem

.step-index
  width: 44px
  height: 44px
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-100), 1)
  display: grid
  place-items: center
  font-weight: 700
  font-size: 1.1rem
  color: rgba(var(--v-theme-text-on-accent), 0.9)

.step-icon
  width: 44px
  height: 44px
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-080), 0.9)
  display: grid
  place-items: center
  color: rgba(var(--v-theme-accent-primary-highlight), 1)

.step-title
  font-size: 1.25rem
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)
</style>
