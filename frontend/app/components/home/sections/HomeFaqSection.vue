<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

type FaqItem = {
  question: string
  answer: string
  blocId: string
}

const props = defineProps<{
  items: FaqItem[]
}>()

const { t } = useI18n()
</script>

<template>
  <section class="home-section home-faq" aria-labelledby="home-faq-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <header class="home-section__header">
          <h2 id="home-faq-title">{{ t('home.faq.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.faq.subtitle') }}</p>
        </header>
        <v-expansion-panels class="home-faq__panels" multiple variant="accordion">
          <v-expansion-panel v-for="panel in props.items" :key="panel.blocId">
            <v-expansion-panel-title >
              <h3 class="home-faq__panel-title">{{ panel.question }}</h3>
            </v-expansion-panel-title>
            <v-expansion-panel-text class="home-faq__panel-text">
              <TextContent
                class="home-faq__text-content"
                :bloc-id="panel.blocId"
                :fallback-text="panel.answer"
                :ipsum-length="panel.answer.length"
              />
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(3rem, 6vw, 5rem)
  background: rgba(var(--v-theme-surface-default), 0.98)

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3rem)

.home-section__header
  max-width: 760px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-faq__panels
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  overflow: hidden
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)

.home-faq__panel-title
  font-weight: 600
  font-size: 1.05rem

.home-faq__panel-text
  background: rgba(var(--v-theme-surface-primary-080), 0.4)

.home-faq__text-content
  padding-block: 0.5rem
</style>
