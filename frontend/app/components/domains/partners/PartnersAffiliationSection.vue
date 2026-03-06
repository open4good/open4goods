<template>
  <section class="partners-affiliation" :aria-labelledby="headingId">
    <v-container class="partners-affiliation__container" max-width="xl">
      <header class="partners-affiliation__header d-flex flex-column gap-4">
        <div class="partners-affiliation__titles">
          <div class="d-flex flex-column gap-2">
            <h2 :id="headingId" class="text-h4 text-wrap font-weight-bold">
              {{ title }}
            </h2>
            <p class="text-body-1 text-neutral-secondary mb-0">
              {{ subtitle }}
            </p>
          </div>
        </div>

        <v-text-field
          v-model="searchTerm"
          :label="searchLabel"
          :placeholder="searchPlaceholder"
          variant="outlined"
          color="primary"
          density="comfortable"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="mt-6 partners-affiliation__search"
          :aria-label="searchLabel"
        />
      </header>

      <div v-if="slides.length > 0" class="partners-affiliation__carousel">
        <v-carousel
          v-model="activeSlide"
          :items="slides"
          :height="carouselHeight"
          :cycle="slides.length > 1"
          :show-arrows="slides.length > 1"
          :hide-delimiters="true"
          :hide-delimiter-background="true"
          :interval="12000"
          :pause-on-hover="true"
          :aria-label="carouselAriaLabel"
        >
          <v-carousel-item
            v-for="(slide, slideIndex) in slides"
            :key="`affiliation-slide-${slideIndex}`"
          >
            <div class="partners-affiliation__slide">
              <article
                v-for="partner in slide"
                :key="partner.id ?? partner.name"
                class="partners-affiliation__card"
              >
                <div class="partners-affiliation__hover-wrapper">
                  <v-hover v-slot="{ isHovering, props: hoverProps }">
                    <v-card
                      v-bind="hoverProps"
                      :class="[
                        'partners-affiliation__card-surface',
                        {
                          'partners-affiliation__card-surface--compact':
                            compact,
                        },
                      ]"
                      :elevation="isHovering ? 6 : 1"
                      rounded="lg"
                      :href="partner.affiliationLink ?? undefined"
                      :target="partner.affiliationLink ? '_blank' : undefined"
                      :rel="
                        partner.affiliationLink
                          ? 'noopener nofollow'
                          : undefined
                      "
                      :aria-label="linkAriaLabel(partner)"
                    >
                      <div class="partners-affiliation__card-media">
                        <v-img
                          :src="partner.logoUrl"
                          :alt="logoAlt(partner)"
                          :width="logoSize"
                          :height="logoSize"
                          class="partners-affiliation__logo"
                        />
                      </div>
                      <div class="partners-affiliation__card-content">
                        <h3
                          class="text-subtitle-1 font-weight-medium mb-1 text-center"
                        >
                          {{ partner.name }}
                        </h3>
                        <div
                          class="partners-affiliation__cta"
                          aria-hidden="true"
                        >
                          {{ linkLabel }}
                          <v-icon
                            icon="mdi-open-in-new"
                            size="16"
                            class="ms-1"
                          />
                        </div>
                      </div>
                    </v-card>
                  </v-hover>
                </div>
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
import type { AffiliationPartnerDto } from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle: string
    partners: AffiliationPartnerDto[]
    searchLabel: string
    searchPlaceholder: string
    emptyStateLabel: string
    carouselAriaLabel: string
    linkLabel: string
    compact?: boolean
  }>(),
  {
    compact: false,
  }
)

const display = useDisplay()
const headingId = useId()
const searchTerm = ref('')
const activeSlide = ref(0)

const normalizedPartners = computed(() => props.partners ?? [])

const filteredPartners = computed(() => {
  const query = searchTerm.value.trim().toLowerCase()

  if (!query) {
    return normalizedPartners.value
  }

  return normalizedPartners.value.filter(partner =>
    (partner.name ?? '').toLowerCase().includes(query)
  )
})

const itemsPerSlide = computed(() => {
  if (display.xlAndUp.value) {
    return 14
  }

  if (display.lgAndUp.value) {
    return 12
  }

  if (display.mdAndUp.value) {
    return 8
  }

  if (display.smAndUp.value) {
    return 6
  }

  return 4
})

const slides = computed(() => {
  const chunkSize = Math.max(1, itemsPerSlide.value)
  const source = filteredPartners.value
  const chunks: AffiliationPartnerDto[][] = []

  for (let index = 0; index < source.length; index += chunkSize) {
    chunks.push(source.slice(index, index + chunkSize))
  }

  return chunks
})

watch(slides, () => {
  activeSlide.value = 0
})

const carouselHeight = computed(() => {
  if (props.compact) return display.mdAndUp.value ? 400 : 480
  return display.mdAndUp.value ? 560 : 640
})

const logoSize = computed(() => {
  if (props.compact) return display.smAndUp.value ? 64 : 48
  return display.smAndUp.value ? 96 : 80
})

const logoAlt = (partner: AffiliationPartnerDto) =>
  partner.name ? `${partner.name} logo` : ''

const linkAriaLabel = (partner: AffiliationPartnerDto) => {
  const name = partner.name?.trim()

  return name ? `${props.linkLabel} – ${name}` : props.linkLabel
}
</script>

<style scoped lang="scss">
.partners-affiliation {
  color: rgb(var(--v-theme-text-neutral-strong));

  &__container {
    padding-inline: clamp(1.25rem, 3vw, 1.75rem);
  }

  &__header {
    max-width: 720px;
    margin: 0 auto 2rem;
    text-align: center;
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  &__search {
    width: min(100%, 560px);
    margin: 0 auto;
  }

  &__carousel {
    position: relative;
  }

  &__slide {
    display: grid;
    gap: 1.5rem;
    justify-content: center;
    align-items: stretch;
    grid-template-columns: repeat(2, 1fr);

    @media (min-width: 600px) {
      grid-template-columns: repeat(3, 1fr);
    }

    @media (min-width: 960px) {
      grid-template-columns: repeat(4, 1fr);
    }

    @media (min-width: 1280px) {
      grid-template-columns: repeat(6, 1fr);
    }

    @media (min-width: 1920px) {
      grid-template-columns: repeat(7, 1fr);
    }
  }

  &__card {
    display: flex;
  }

  &__hover-wrapper {
    display: contents;
  }

  &__card-surface {
    background: rgba(var(--v-theme-surface-default), 0.94);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
    padding: 1.25rem 1rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    transition:
      transform 200ms ease,
      box-shadow 200ms ease;
    cursor: pointer;
    color: inherit;
    text-decoration: none;

    &--compact {
      padding: 0.75rem 0.5rem;
      gap: 0.5rem;
    }

    &:hover {
      transform: translateY(-4px);
    }
  }

  &__card-media {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    min-height: 80px;

    .partners-affiliation__card-surface--compact & {
      min-height: 60px;
    }
  }

  &__logo {
    filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.08));
    padding: 0.5rem;
  }

  &__card-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 0.25rem;
  }

  &__cta {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.25rem;
    text-transform: none;
    font-weight: 600;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>
