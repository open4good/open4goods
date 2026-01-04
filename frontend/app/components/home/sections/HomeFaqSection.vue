<script setup lang="ts">
import { computed } from 'vue'
import { useLocalePath } from '#i18n'
import DOMPurify from 'isomorphic-dompurify'
import HomeContactCard from '~/components/home/HomeContactCard.vue'

type FaqItem = {
  question: string
  answer: string
  blocId: string
  isImpactScore?: boolean
  isContact?: boolean
  ctaLabel?: string
  ctaAria?: string
}

const props = defineProps<{
  items: FaqItem[]
}>()

const hasPanels = computed(() => props.items.length > 0)
const localePath = useLocalePath()
const impactScorePath = computed(() => localePath('/impact-score'))

const sanitize = (content: string) => DOMPurify.sanitize(content)
</script>

<template>
  <section class="home-section home-faq" aria-labelledby="home-faq-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <h2 id="home-faq-title" class="home-hero__subtitle">
          {{ $t('home.faq.title') }}
        </h2>
        <p class="home-section__subtitle text-center">
          {{ $t('home.faq.subtitle') }}
        </p>
        <v-expansion-panels
          v-if="hasPanels"
          class="home-faq__panels"
          multiple
          variant="accordion"
        >
          <v-expansion-panel v-for="panel in props.items" :key="panel.blocId">
            <v-expansion-panel-title>
              <h3 class="home-faq__panel-title">{{ panel.question }}</h3>
            </v-expansion-panel-title>
            <v-expansion-panel-text class="home-faq__panel-text">
              <!-- eslint-disable vue/no-v-html -->
              <div
                v-if="!panel.isContact"
                class="home-faq__answer"
                v-html="sanitize(panel.answer)"
              />
              <!-- eslint-enable vue/no-v-html -->
              <HomeContactCard v-else />
              <div
                v-if="panel.isImpactScore && panel.ctaLabel"
                class="home-faq__cta-wrapper"
              >
                <v-btn
                  class="home-faq__cta"
                  color="primary"
                  variant="tonal"
                  :to="impactScorePath"
                  :aria-label="panel.ctaAria || panel.ctaLabel"
                >
                  {{ panel.ctaLabel }}
                </v-btn>
              </div>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-section__container
  padding-inline: 0

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(0.875rem, 2vw, 1.5rem)

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
  border: 1px solid rgb(var(--v-theme-primary))

.home-faq__panel-title
  font-weight: 600
  font-size: 1.05rem

.home-faq__panel-text
  background: rgb(var(--v-theme-surface-default))

.home-faq__answer
  padding-block: 0.5rem
  text-align: left
  margin-left: 1.25rem
  gap: 0.375rem
  color: rgb(var(--v-theme-text-neutral-secondary))

  ul
    padding-left: 2.25rem
    margin: 0

.home-faq__cta-wrapper
  margin-top: 0.5rem

.home-faq__cta
  width: fit-content
  font-weight: 600
</style>
