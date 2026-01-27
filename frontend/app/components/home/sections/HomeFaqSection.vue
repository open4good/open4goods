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
  reveal?: boolean
}>()

const hasPanels = computed(() => props.items.length > 0)
const localePath = useLocalePath()
const impactScorePath = computed(() => localePath('/impact-score'))

const sanitize = (content: string) => DOMPurify.sanitize(content)
const isVisible = computed(() => Boolean(props.reveal))
</script>

<template>
  <section class="home-section home-faq" aria-labelledby="home-faq-title">
    <div
      class="home-faq__content home-reveal-group"
      :class="{ 'is-ready': true, 'is-visible': isVisible }"
    >
      <h2
        id="home-faq-title"
        class="home-hero__subtitle home-reveal-item ma-0"
        :style="{ '--reveal-delay': '0ms' }"
      >
        {{ $t('home.faq.title') }}
      </h2>
      <p
        class="home-section__subtitle home-reveal-item ma-0"
        :style="{ '--reveal-delay': '100ms' }"
      >
        {{ $t('home.faq.subtitle') }}
      </p>
      <v-expansion-panels
        v-if="hasPanels"
        class="home-faq__panels home-reveal-item"
        multiple
        variant="accordion"
        :style="{ '--reveal-delay': '200ms' }"
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
                class="home-faq__cta nudger_degrade-defaut"
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
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: 0
  background: transparent

.home-section__container
  padding-inline: 0

// .home-section__inner styles now handled by utility classes: d-flex flex-column ga-3 mx-auto + inline style

// .home-section__header styles now handled by utility classes: d-flex flex-column ga-2

.home-section__subtitle
  // margin now handled by utility class: ma-0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-faq__panels
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  overflow: hidden
  border: 1px solid rgb(var(--v-theme-primary))

.home-faq__panel-title
  font-weight: 600
  font-size: 1.05rem

.home-faq__panel-text
  //background: rgba(var(--v-theme-surface-primary-080), 0.4)
  background: rgb(var(--v-theme-surface-default))

.home-faq__text-content
  padding-block: 0.5rem
</style>
