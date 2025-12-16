<script setup lang="ts">
import { useAnalytics } from '~/composables/useAnalytics'

interface DownloadHighlight {
  icon?: string
  text: string
}

interface DownloadCta {
  label: string
  ariaLabel: string
  href: string
  variant?: 'flat' | 'outlined' | 'tonal' | 'text' | 'plain'
  color?: string
  target?: string
  rel?: string
  disabled?: boolean
}

interface DownloadOption {
  id: string
  title: string
  description: string
  highlights: DownloadHighlight[]
  badge?: string
  cta?: DownloadCta
}

defineProps<{
  title: string
  subtitle?: string
  options: DownloadOption[]
}>()

const { trackOpenDataDownload } = useAnalytics()

const handleDownloadClick = (option: DownloadOption) => {
  if (!option.cta) {
    return
  }

  trackOpenDataDownload({
    datasetId: option.id,
    method: option.cta.label,
    href: option.cta.href,
  })
}
</script>

<template>
  <section class="dataset-download" aria-labelledby="dataset-download-heading">
    <v-container max-width="lg">
      <div class="dataset-download__header">
        <h2 id="dataset-download-heading" class="dataset-download__title">
          {{ title }}
        </h2>
        <p v-if="subtitle" class="dataset-download__subtitle">{{ subtitle }}</p>
      </div>

      <v-row class="g-6" align="stretch" justify="center">
        <v-col
          v-for="option in options"
          :key="option.id"
          cols="12"
          md="6"
          class="d-flex justify-center"
        >
          <v-card class="dataset-download__card" rounded="xl" elevation="4">
            <div v-if="option.badge" class="dataset-download__badge">
              {{ option.badge }}
            </div>
            <h3 class="dataset-download__card-title">{{ option.title }}</h3>
            <p class="dataset-download__card-description">
              {{ option.description }}
            </p>

            <ul class="dataset-download__highlights">
              <li v-for="highlight in option.highlights" :key="highlight.text">
                <v-icon
                  v-if="highlight.icon"
                  :icon="highlight.icon"
                  size="small"
                />
                <span>{{ highlight.text }}</span>
              </li>
            </ul>

            <v-btn
              v-if="option.cta"
              :href="option.cta.href"
              :aria-label="option.cta.ariaLabel"
              :variant="option.cta.variant ?? 'flat'"
              :color="option.cta.color ?? 'primary'"
              :target="option.cta.target"
              :rel="option.cta.rel"
              size="large"
              append-icon="mdi-arrow-down"
              :disabled="option.cta.disabled"
              class="dataset-download__cta"
              @click="handleDownloadClick(option)"
            >
              {{ option.cta.label }}
            </v-btn>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.dataset-download
  padding: clamp(3rem, 6vw, 5rem) 0
  background: rgba(var(--v-theme-surface-default), 1)

.dataset-download__header
  text-align: center
  max-width: 720px
  margin: 0 auto clamp(2rem, 5vw, 3rem)

.dataset-download__title
  margin: 0
  font-size: clamp(2rem, 4vw, 2.75rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.dataset-download__subtitle
  margin-top: 1rem
  margin-bottom: 0
  font-size: 1.05rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.dataset-download__card
  position: relative
  display: flex
  flex-direction: column
  gap: 1.25rem
  padding: clamp(1.75rem, 4vw, 2.75rem)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  background: rgba(var(--v-theme-surface-primary-050), 0.9)
  width: 100%

.dataset-download__badge
  align-self: flex-start
  padding: 0.35rem 0.75rem
  border-radius: 999px
  font-size: 0.75rem
  font-weight: 600
  letter-spacing: 0.05em
  text-transform: uppercase
  background: rgba(var(--v-theme-primary), 0.12)
  color: rgba(var(--v-theme-primary), 1)

.dataset-download__card-title
  margin: 0
  font-size: 1.5rem
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.dataset-download__card-description
  margin: 0
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.dataset-download__highlights
  list-style: none
  display: flex
  flex-direction: column
  gap: 0.6rem
  margin: 0
  padding: 0

.dataset-download__highlights li
  display: flex
  align-items: center
  gap: 0.5rem
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.dataset-download__cta
  margin-top: auto

@media (max-width: 959px)
  .dataset-download__card
    height: 100%

  .dataset-download__cta
    width: 100%
</style>
