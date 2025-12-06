<template>
  <section
    :class="['partners-static', `partners-static--${toneClass}`]"
    :aria-labelledby="headingId"
  >
    <v-container class="partners-static__container" max-width="xl">
      <header class="partners-static__header">
        <h2 :id="headingId" class="text-h4 text-wrap font-weight-bold mb-3">
          {{ title }}
        </h2>
        <p class="text-body-1 text-neutral-secondary mb-0">
          {{ subtitle }}
        </p>
      </header>

      <div v-if="slides.length > 0" class="partners-static__carousel">
        <v-carousel
          v-model="activeSlide"
          :cycle="shouldShowControls"
          :interval="shouldShowControls ? 6500 : undefined"
          :pause-on-hover="true"
          :hide-delimiters="true"
          :hide-delimiter-background="true"
          :show-arrows="shouldShowControls"
          :aria-label="carouselAriaLabel"
          :height="display.mdAndUp.value ? 420 : 460"
        >
          <v-carousel-item
            v-for="(slide, slideIndex) in slides"
            :key="`static-slide-${slideIndex}`"
          >
            <div class="partners-static__slide">
              <article
                v-for="partner in slide"
                :key="partner.name ?? partner.blocId ?? slideIndex"
                class="partners-static__card"
              >
                <v-card
                  class="partners-static__card-surface"
                  elevation="0"
                  rounded="xl"
                >
                  <div
                    v-if="partner.imageUrl"
                    class="partners-static__card-media"
                  >
                    <div class="partners-static__logo-frame">
                      <v-img
                        :src="partner.imageUrl"
                        :alt="imageAlt(partner)"
                        contain
                        class="partners-static__image"
                      />
                    </div>
                  </div>
                  <div class="partners-static__card-body">
                    <h3 class="text-h6 font-weight-semibold mb-3">
                      {{ partner.name }}
                    </h3>
                    <TextContent
                      v-if="partner.blocId"
                      :bloc-id="partner.blocId"
                      :ipsum-length="160"
                    />
                    <p v-else class="text-body-2 text-neutral-secondary">
                      {{ fallbackDescription }}
                    </p>
                  </div>
                  <div class="partners-static__card-footer">
                    <v-btn
                      v-if="partner.url"
                      :href="partner.url"
                      target="_blank"
                      rel="noopener nofollow"
                      color="primary"
                      variant="tonal"
                      class="partners-static__cta"
                    >
                      {{ linkLabel }}
                      <v-icon icon="mdi-open-in-new" size="16" class="ms-1" />
                    </v-btn>
                  </div>
                </v-card>
              </article>
            </div>
          </v-carousel-item>
        </v-carousel>
      </div>

      <v-alert
        v-else
        type="info"
        variant="tonal"
        border="start"
        class="mt-8"
        :aria-live="'polite'"
      >
        {{ emptyStateLabel }}
      </v-alert>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import { useDisplay } from 'vuetify'
import TextContent from '~/components/domains/content/TextContent.vue'
import type { StaticPartnerDto } from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle: string
    partners: StaticPartnerDto[]
    carouselAriaLabel: string
    emptyStateLabel: string
    linkLabel: string
    fallbackDescription: string
    tone?: 'default' | 'muted'
  }>(),
  {
    tone: 'default',
  }
)

const display = useDisplay()
const headingId = useId()
const activeSlide = ref(0)

const normalizedPartners = computed(() => props.partners ?? [])

const itemsPerSlide = computed(() => {
  if (display.mdAndUp.value) {
    return 3
  }

  if (display.smAndUp.value) {
    return 2
  }

  return 1
})

const slides = computed(() => {
  const chunkSize = Math.max(1, itemsPerSlide.value)
  const source = normalizedPartners.value
  const chunks: StaticPartnerDto[][] = []

  for (let index = 0; index < source.length; index += chunkSize) {
    chunks.push(source.slice(index, index + chunkSize))
  }

  return chunks
})

watch(slides, () => {
  activeSlide.value = 0
})

const toneClass = computed(() => props.tone)

const shouldShowControls = computed(() => slides.value.length > 1)

const imageAlt = (partner: StaticPartnerDto) =>
  partner.name ? `${partner.name} visual` : ''
</script>

<style scoped lang="scss">
.partners-static {
  color: rgb(var(--v-theme-text-neutral-strong));

  &__container {
    padding-inline: clamp(1.25rem, 3vw, 1.75rem);
  }

  &__header {
    text-align: center;
    max-width: 720px;
    margin: 0 auto 2.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  &__carousel {
    position: relative;
  }

  &__slide {
    display: grid;
    gap: 1.5rem;
    align-items: stretch;

    @media (min-width: 960px) {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    @media (min-width: 600px) and (max-width: 959px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 599px) {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
  }

  &__card {
    display: flex;
  }

  &__card-surface {
    background: rgba(var(--v-theme-surface-default), 0.96);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
  }

  &__card-media {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1.25rem 1.5rem;
    background: rgba(var(--v-theme-surface-muted), 0.75);
    border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
  }

  &__logo-frame {
    width: 100%;
    aspect-ratio: 3 / 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__image {
    width: 100%;
    height: 100%;

    :deep(.v-img__img) {
      object-fit: contain;
      object-position: center;
    }
  }

  &__card-body {
    padding: 1.25rem 1.5rem 1rem;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  &__card-footer {
    padding: 0 1.5rem 1.5rem;
    display: flex;
    justify-content: flex-end;
  }

  &__cta {
    text-transform: none;
    font-weight: 600;
  }

  &--muted {
    .partners-static__card-surface {
      background: rgba(var(--v-theme-surface-alt), 0.96);
    }

    .partners-static__card-media {
      background: rgba(var(--v-theme-surface-muted), 0.9);
    }
  }
}
</style>
