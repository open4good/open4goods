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

    <div v-if="verticalCapsules.length" class="impact-score-methodology__capsules">
      <span class="impact-score-methodology__label">
        {{ t('impactScorePage.sections.methodology.verticalLabel') }}
      </span>
      <div class="impact-score-methodology__capsules-list">
        <v-chip
          v-for="vertical in verticalCapsules"
          :key="vertical.key"
          class="impact-score-methodology__chip"
          variant="tonal"
          rounded="pill"
          :to="vertical.link"
          link
          :aria-label="
            t('impactScorePage.sections.methodology.verticalCardAria', {
              vertical: vertical.label,
            })
          "
        >
          <span class="impact-score-methodology__chip-text">
            {{ vertical.label }}
          </span>
          <v-icon icon="mdi-arrow-top-right" size="16" />
        </v-chip>
      </div>
    </div>
    <p v-else class="impact-score-methodology__empty">
      {{ t('impactScorePage.sections.methodology.empty') }}
    </p>
  </section>
</template>

<script setup lang="ts">
import type { VerticalConfigDto } from '~~/shared/api-client'

const props = defineProps<{
  verticals: VerticalConfigDto[]
}>()

const { t } = useI18n()
const localePath = useLocalePath()

const normalizeVerticalLink = (vertical: VerticalConfigDto): string | null => {
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

  return localePath(`${normalized}/ecoscore`)
}

const verticalCapsules = computed(() =>
  [...props.verticals]
    .filter(vertical => vertical.enabled !== false)
    .map(vertical => ({
      key: vertical.id ?? vertical.verticalHomeUrl ?? vertical.verticalHomeTitle,
      label: vertical.verticalHomeTitle?.trim() ?? '',
      order: vertical.order ?? Number.MAX_SAFE_INTEGER,
      link: normalizeVerticalLink(vertical),
    }))
    .filter(
      (vertical): vertical is {
        key: string
        label: string
        order: number
        link: string
      } => Boolean(vertical.key && vertical.label && vertical.link)
    )
    .sort((a, b) => a.order - b.order)
)
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

.impact-score-methodology__capsules-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.impact-score-methodology__chip {
  background: rgba(var(--v-theme-surface-primary-080), 0.85);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
}

.impact-score-methodology__chip :deep(.v-icon) {
  margin-left: 0.35rem;
}

.impact-score-methodology__chip-text {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.impact-score-methodology__empty {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>
