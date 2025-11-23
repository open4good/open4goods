<script setup lang="ts">
interface FaqItem {
  id: string
  question: string
  answer: string
}

defineProps<{
  title: string
  subtitle?: string
  items: FaqItem[]
}>()
</script>

<template>
  <section class="opendata-faq" aria-labelledby="opendata-faq-heading">
    <v-container max-width="lg">
      <div class="opendata-faq__header">
        <h2 id="opendata-faq-heading" class="opendata-faq__title">{{ title }}</h2>
        <p v-if="subtitle" class="opendata-faq__subtitle">{{ subtitle }}</p>
      </div>

      <v-expansion-panels variant="accordion" class="opendata-faq__panels">
        <v-expansion-panel v-for="item in items" :key="item.id" eager>
          <v-expansion-panel-title expand-icon="mdi-chevron-down">
            <h3 class="opendata-faq__question">{{ item.question }}</h3>
          </v-expansion-panel-title>
          <v-expansion-panel-text>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="opendata-faq__answer" v-html="item.answer" />
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-faq
  padding: clamp(3rem, 6vw, 5rem) 0
  background: rgba(var(--v-theme-surface-muted), 1)

.opendata-faq__header
  text-align: center
  max-width: 720px
  margin: 0 auto clamp(2rem, 5vw, 3rem)

.opendata-faq__title
  margin: 0
  font-size: clamp(2rem, 3.5vw, 2.5rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-faq__subtitle
  margin-top: 1rem
  margin-bottom: 0
  font-size: 1.05rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-faq__panels
  border-radius: 20px
  overflow: hidden
  background: rgba(var(--v-theme-surface-default), 1)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.opendata-faq__question
  font-size: 1.05rem
  font-weight: 600
  color: rgba(var(--v-theme-text-neutral-strong), 0.95)

.opendata-faq__answer :deep(p)
  margin: 0 0 1rem
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-faq__answer :deep(p:last-child)
  margin-bottom: 0

.opendata-faq__answer :deep(ul)
  margin: 0 0 1rem 1.25rem
  padding: 0
  list-style: disc
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-faq__answer :deep(li)
  margin-bottom: 0.5rem
  font-size: 0.95rem

.opendata-faq__answer :deep(li:last-child)
  margin-bottom: 0

.v-expansion-panel-text
  background: rgba(var(--v-theme-surface-default), 1)
</style>

