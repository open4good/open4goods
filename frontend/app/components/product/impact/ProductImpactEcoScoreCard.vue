<template>
  <article v-if="score" class="impact-ecoscore">
    <header class="impact-ecoscore__header">
      <div class="impact-ecoscore__header-main">
        <span class="impact-ecoscore__eyebrow">{{ $t('product.impact.primaryScoreLabel') }}</span>
        <h3 class="impact-ecoscore__title">{{ score.label }}</h3>
        <p v-if="score.description" class="impact-ecoscore__description">{{ score.description }}</p>
      </div>
      <NuxtLink
        :to="methodologyHref"
        class="impact-ecoscore__cta"
        :aria-label="$t('product.impact.methodologyLinkAria')"
      >
        <span>{{ $t('product.impact.methodologyLink') }}</span>
        <v-icon icon="mdi-arrow-top-right" size="18" />
      </NuxtLink>
    </header>

    <div class="impact-ecoscore__score">
      <ImpactScore :score="normalizedScore" :max="5" size="large" show-value />
    </div>
  </article>
  <article v-else class="impact-ecoscore impact-ecoscore--empty">
    <span class="impact-ecoscore__placeholder">{{ $t('product.impact.noPrimaryScore') }}</span>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ScoreView } from './impact-types'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const props = defineProps<{
  score: ScoreView | null
  verticalHomeUrl?: string | null
}>()

const { locale } = useI18n()

const normalizedScore = computed(() => {
  const rawValue = Number.isFinite(props.score?.value)
    ? Number(props.score?.value)
    : Number.isFinite(props.score?.relativeValue)
      ? Number(props.score?.relativeValue)
      : 0

  return Math.max(0, Math.min(rawValue, 5))
})

const normalizedVerticalEcoscorePath = computed(() => {
  const raw = props.verticalHomeUrl?.trim()
  if (!raw) {
    return null
  }

  const sanitized = raw.replace(/^\/+/, '').replace(/\/+$/, '')
  if (!sanitized.length) {
    return null
  }

  return `/${sanitized}/ecoscore`
})

const methodologyHref = computed(() => {
  if (normalizedVerticalEcoscorePath.value) {
    return normalizedVerticalEcoscorePath.value
  }

  return resolveLocalizedRoutePath('impact-score', locale.value)
})
</script>

<style scoped>
.impact-ecoscore {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 2rem;
  border-radius: 26px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-100), 0.95), rgba(var(--v-theme-surface-glass-strong), 0.9));
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.12);
  min-height: 100%;
}

.impact-ecoscore__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.impact-ecoscore__header-main {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 38ch;
}

.impact-ecoscore__eyebrow {
  display: inline-flex;
  align-items: center;
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-soft), 0.85);
  margin-bottom: 0.25rem;
}

.impact-ecoscore__title {
  margin: 0;
  font-size: clamp(1.4rem, 2.5vw, 2rem);
  font-weight: 700;
}

.impact-ecoscore__description {
  margin: 0.5rem 0 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-size: 0.95rem;
}

.impact-ecoscore__score {
  display: flex;
  justify-content: flex-start;
}

.impact-ecoscore__cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.1rem;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-surface-default), 0.9);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.impact-ecoscore__cta:hover,
.impact-ecoscore__cta:focus-visible {
  transform: translateY(-2px);
  background: rgba(var(--v-theme-surface-default), 1);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.18);
}

.impact-ecoscore__cta:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.35);
}

.impact-ecoscore--empty {
  align-items: center;
  justify-content: center;
  text-align: center;
}

.impact-ecoscore__placeholder {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

@media (max-width: 768px) {
  .impact-ecoscore__header-main {
    max-width: 100%;
  }

  .impact-ecoscore__cta {
    width: 100%;
    justify-content: center;
  }
}
</style>
