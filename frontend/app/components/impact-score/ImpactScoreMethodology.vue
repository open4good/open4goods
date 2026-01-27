<template>
  <section class="impact-score-methodology">
    <header class="impact-score-methodology__header">
      <v-chip
        class="impact-score-methodology__eyebrow"
        color="accent-primary-highlight"
        variant="tonal"
      >
        {{ t('impactScorePage.sections.methodology.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-methodology__title">
        {{ t('impactScorePage.sections.methodology.title') }}
      </h2>
      <p class="impact-score-methodology__intro">
        {{ t('impactScorePage.sections.methodology.intro') }}
      </p>
    </header>

    <div
      v-if="verticalCapsules.length"
      class="impact-score-methodology__capsules"
    >
      <span class="impact-score-methodology__label">
        {{ t('impactScorePage.sections.methodology.verticalLabel') }}
      </span>
      <ResponsiveCarousel
        class="impact-score-methodology__carousel"
        :items="verticalCapsules"
        :aria-label="
          t('impactScorePage.sections.methodology.verticalCarouselAria')
        "
        :breakpoints="{ xs: 1, sm: 2, md: 3, lg: 3, xl: 3 }"
      >
        <template #item="{ item }">
          <v-card
            class="impact-score-methodology__card"
            elevation="0"
            rounded="xl"
            border
            role="link"
            tabindex="0"
            :aria-label="
              t('impactScorePage.sections.methodology.verticalCardAria', {
                vertical: item.label,
              })
            "
            @click="navigateToEcoscore(item.ecoscoreLink)"
            @keydown.enter.prevent="navigateToEcoscore(item.ecoscoreLink)"
            @keydown.space.prevent="navigateToEcoscore(item.ecoscoreLink)"
          >
            <div class="impact-score-methodology__card-media">
              <v-img
                v-if="item.image"
                :src="item.image"
                :alt="
                  t('impactScorePage.sections.methodology.verticalImageAlt', {
                    vertical: item.label,
                  })
                "
                contain
                class="impact-score-methodology__card-image"
              />
              <div
                v-else
                class="impact-score-methodology__card-image impact-score-methodology__card-image--empty"
                aria-hidden="true"
              >
                <v-icon icon="mdi-image-outline" size="40" />
              </div>
            </div>

            <div class="impact-score-methodology__card-body">
              <p class="impact-score-methodology__card-label">
                {{ t('impactScorePage.sections.methodology.verticalLabel') }}
              </p>
              <h3 class="impact-score-methodology__card-title">
                {{ item.label }}
              </h3>
              <v-btn
                class="impact-score-methodology__card-cta"
                :aria-label="
                  t('impactScorePage.sections.methodology.verticalCtaAria', {
                    vertical: item.label,
                  })
                "
                variant="text"
                color="primary"
                type="button"
                @click.stop.prevent="navigateToCategory(item.categoryLink)"
              >
                <span>{{
                  t('impactScorePage.sections.methodology.verticalCta')
                }}</span>
                <v-icon icon="mdi-arrow-top-right" size="16" />
              </v-btn>
            </div>
          </v-card>
        </template>
      </ResponsiveCarousel>
    </div>
    <p v-else class="impact-score-methodology__empty">
      {{ t('impactScorePage.sections.methodology.empty') }}
    </p>
  </section>
</template>

<script setup lang="ts">
import ResponsiveCarousel from '~/components/shared/ui/ResponsiveCarousel.vue'
import type { VerticalConfigDto } from '~~/shared/api-client'

const props = defineProps<{
  verticals: VerticalConfigDto[]
}>()

const { t } = useI18n()
const localePath = useLocalePath()
const router = useRouter()

const normalizeVerticalBase = (vertical: VerticalConfigDto): string | null => {
  if (!vertical.verticalHomeUrl) {
    return null
  }

  const trimmed = vertical.verticalHomeUrl.trim()

  if (!trimmed) {
    return null
  }

  const withSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  const normalized = withSlash.endsWith('/')
    ? withSlash.slice(0, -1)
    : withSlash

  return localePath(normalized)
}

const verticalCapsules = computed(() =>
  [...props.verticals]
    .filter(vertical => vertical.enabled !== false)
    .map(vertical => ({
      key:
        vertical.id ?? vertical.verticalHomeUrl ?? vertical.verticalHomeTitle,
      label: vertical.verticalHomeTitle?.trim() ?? '',
      order: vertical.order ?? Number.MAX_SAFE_INTEGER,
      categoryLink: normalizeVerticalBase(vertical),
      ecoscoreLink: (() => {
        const base = normalizeVerticalBase(vertical)
        return base ? `${base}/ecoscore` : null
      })(),
      image:
        vertical.imageMedium ??
        vertical.imageLarge ??
        vertical.imageSmall ??
        null,
    }))
    .filter(
      (
        vertical
      ): vertical is {
        key: string
        label: string
        order: number
        categoryLink: string
        ecoscoreLink: string
        image: string | null
      } =>
        Boolean(
          vertical.key &&
          vertical.label &&
          vertical.categoryLink &&
          vertical.ecoscoreLink
        )
    )
    .sort((a, b) => a.order - b.order)
)

const navigateToEcoscore = (link: string) => {
  router.push(link)
}
</script>

<style scoped>
.impact-score-methodology {
  display: grid;
  gap: 1.5rem;
}

.impact-score-methodology__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-methodology__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-methodology__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-methodology__intro {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-methodology__capsules {
  display: grid;
  gap: 0.9rem;
}

.impact-score-methodology__label {
  font-size: 0.85rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-methodology__card {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: rgba(var(--v-theme-surface-default), 0.98);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.6);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;
  cursor: pointer;
}

.impact-score-methodology__card:focus-visible {
  outline: 2px solid rgba(var(--v-theme-accent-primary-highlight), 0.8);
  outline-offset: 4px;
}

.impact-score-methodology__card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px -24px rgba(var(--v-theme-shadow-primary-600), 0.4);
}

.impact-score-methodology__card-media {
  padding: 1.1rem 1.1rem 0;
}

.impact-score-methodology__card-image {
  height: 160px;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-primary-080), 0.7);
}

.impact-score-methodology__card-image--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgb(var(--v-theme-text-neutral-soft));
}

.impact-score-methodology__card-body {
  display: grid;
  gap: 0.5rem;
  padding: 1.1rem;
}

.impact-score-methodology__card-label {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-methodology__card-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-methodology__card-cta {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 0.35rem;
  font-weight: 600;
  text-transform: none;
  padding-inline: 0.25rem;
  align-self: flex-start;
  text-align: left;
}

.impact-score-methodology__card-cta :deep(.v-icon) {
  margin-left: 0.15rem;
}

.impact-score-methodology__empty {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-methodology__carousel :deep(.responsive-carousel__slide) {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}
</style>
