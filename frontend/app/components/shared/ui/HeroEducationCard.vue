<script setup lang="ts">
interface HeroEducationCardItem {
  icon?: string
  text: string
}

const props = withDefaults(
  defineProps<{
    icon: string
    title: string
    bodyHtml?: string
    items?: HeroEducationCardItem[]
  }>(),
  {
    bodyHtml: undefined,
    items: () => [],
  },
)
</script>

<template>
  <v-card class="hero-education-card" elevation="12" rounded="xl" aria-hidden="true">
    <div class="hero-education-card__header">
      <v-icon :icon="props.icon" class="hero-education-card__icon" size="64" />
      <h2 class="hero-education-card__title">{{ props.title }}</h2>
    </div>

    <!-- eslint-disable vue/no-v-html -->
    <p
      v-if="props.bodyHtml"
      class="hero-education-card__body"
      v-html="props.bodyHtml"
    />
    <!-- eslint-enable vue/no-v-html -->

    <v-divider v-if="props.items?.length" class="my-4" />

    <ul v-if="props.items?.length" class="hero-education-card__list">
      <li v-for="(item, index) in props.items" :key="index">
        <v-icon v-if="item.icon" :icon="item.icon" size="small" />
        <span>{{ item.text }}</span>
      </li>
    </ul>
  </v-card>
</template>

<style scoped lang="sass">
.hero-education-card
  background: rgba(var(--v-theme-surface-glass), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
  backdrop-filter: blur(18px)
  color: rgba(var(--v-theme-text-neutral-strong), 0.95)
  padding: clamp(1.5rem, 3vw, 2.5rem)

.hero-education-card__header
  display: flex
  align-items: center
  gap: 1rem
  margin-bottom: 1rem

.hero-education-card__icon
  color: rgba(var(--v-theme-accent-primary-highlight), 0.85)

.hero-education-card__title
  font-size: clamp(1.5rem, 4vw, 2rem)
  margin: 0
  line-height: 1.2

.hero-education-card__body
  font-size: 1rem
  margin: 0
  line-height: 1.6

.hero-education-card__list
  list-style: none
  padding: 0
  margin: 0
  display: flex
  flex-direction: column
  gap: 0.75rem

.hero-education-card__list li
  display: flex
  align-items: center
  gap: 0.75rem
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.hero-education-card__list li .v-icon
  color: rgba(var(--v-theme-accent-supporting), 0.9)
</style>
