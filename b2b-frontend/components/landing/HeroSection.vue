<template>
  <section id="product" class="hero">
    <LandingGlowBackdrop />
    <LandingRevealBlock>
      <v-container class="hero__container">
        <div class="hero__content">
          <div class="hero__copy">
            <v-chip size="small" variant="flat" color="primary" class="hero__badge">
              {{ t('landing.hero.badge') }}
            </v-chip>

            <h1 class="hero__title">
              <span class="hero__title-prefix">{{ t('landing.hero.titlePrefix') }}</span>
              <span class="hero__title-word">
                <component :is="activeTitleTransition" mode="out-in">
                  <span :key="activeVariant?.text || t('landing.hero.fallbackTitleWord')" class="hero__title-emphasis">
                    {{ activeVariant?.text || t('landing.hero.fallbackTitleWord') }}
                  </span>
                </component>
              </span>
            </h1>

            <p class="hero__subtitle">
              {{ t('landing.hero.subtitle') }}
            </p>

            <div class="hero-audience d-flex ga-2 flex-wrap mt-6">
              <v-chip size="small" variant="tonal">{{ t('landing.hero.audiences.individuals') }}</v-chip>
              <v-chip size="small" variant="tonal">{{ t('landing.hero.audiences.enterprises') }}</v-chip>
              <v-chip size="small" variant="tonal">{{ t('landing.hero.audiences.clouders') }}</v-chip>
            </div>

            <v-row class="hero__signals mt-4" density="comfortable">
              <v-col v-for="signal in signals" :key="signal.key" cols="12" sm="4">
                <v-sheet class="hero-signal" rounded="lg" color="transparent" border>
                  <div class="hero-signal__value">{{ t(`landing.hero.signals.${signal.key}.value`) }}</div>
                  <div class="hero-signal__label">{{ t(`landing.hero.signals.${signal.key}.label`) }}</div>
                </v-sheet>
              </v-col>
            </v-row>

            <div class="hero__actions d-flex ga-3 flex-wrap mt-8">
              <v-btn to="/offres/entreprises" color="white" variant="flat" size="large">
                {{ t('landing.hero.enterpriseCta') }}
              </v-btn>
              <v-btn :to="downloadTo" variant="outlined" size="large">
                {{ t('landing.hero.providerCta') }}
              </v-btn>
              <v-btn to="/docs" variant="text" size="large">
                {{ t('landing.hero.secondaryCta') }}
              </v-btn>
            </div>
          </div>

          <v-sheet class="hero__surface" rounded="xl" color="transparent" border>
            <div class="hero__surface-head">
              <div>
                <div class="hero__surface-eyebrow">{{ t('landing.hero.surfaceEyebrow') }}</div>
                <div class="hero__surface-title">{{ t('landing.hero.surfaceTitle') }}</div>
              </div>
              <v-chip size="small" variant="tonal" color="primary">
                {{ t('landing.hero.surfaceBadge') }}
              </v-chip>
            </div>

            <LandingHeroNetworkGraph />

            <v-row class="mt-4" density="comfortable">
              <v-col cols="12" md="6">
                <v-sheet class="hero-track hero-track--enterprise" rounded="lg" color="transparent">
                  <div class="hero-track__label">{{ t('landing.hero.tracks.enterprise.label') }}</div>
                  <div class="hero-track__title">{{ t('landing.hero.tracks.enterprise.title') }}</div>
                  <p class="hero-track__text">{{ t('landing.hero.tracks.enterprise.text') }}</p>
                </v-sheet>
              </v-col>
              <v-col cols="12" md="6">
                <v-sheet class="hero-track hero-track--provider" rounded="lg" color="transparent">
                  <div class="hero-track__label">{{ t('landing.hero.tracks.provider.label') }}</div>
                  <div class="hero-track__title">{{ t('landing.hero.tracks.provider.title') }}</div>
                  <p class="hero-track__text">{{ t('landing.hero.tracks.provider.text') }}</p>
                </v-sheet>
              </v-col>
            </v-row>
          </v-sheet>
        </div>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  VFadeTransition,
  VSlideXTransition,
  VSlideYTransition,
  VScaleTransition,
} from 'vuetify/components'

interface HeroTitleVariant {
  text: string
  effect?: string
}

const { t, tm, rt } = useI18n()
const { downloadTo } = useLandingNav()
const { prefersReducedMotion } = useReducedMotion()
const activeVariantIndex = ref(0)
let variantTimer: ReturnType<typeof setInterval> | undefined

const signals = [
  { key: 'compatibility' },
  { key: 'security' },
  { key: 'activation' }
]

const titleVariants = computed<HeroTitleVariant[]>(() => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const rawVariants = tm('landing.hero.titleVariants') as any
  if (!rawVariants || typeof rawVariants !== 'object') {
    return []
  }

  const variantsArray = Array.isArray(rawVariants) ? rawVariants : Object.values(rawVariants)

  return variantsArray
    .map((variant) => {
      if (!variant || typeof variant !== 'object') return null

      const candidate = variant as Record<string, unknown>
      const textRaw = candidate.text
      if (!textRaw) return null

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const text = typeof textRaw === 'string' ? textRaw : rt(textRaw as any)

      return {
        text,
        effect: typeof candidate.effect === 'string' ? candidate.effect : undefined
      } as HeroTitleVariant
    })
    .filter((v): v is HeroTitleVariant => v !== null)
})

const activeVariant = computed(() => titleVariants.value[activeVariantIndex.value] ?? null)

const effectTransitionMap: Record<string, unknown> = {
  fade: VFadeTransition,
  'slide-x': VSlideXTransition,
  'slide-y': VSlideYTransition,
  scale: VScaleTransition
}

const activeTitleTransition = computed(() => {
  const effect = activeVariant.value?.effect ?? 'fade'
  return effectTransitionMap[effect] ?? effectTransitionMap.fade
})

onMounted(() => {
  if (titleVariants.value.length <= 1 || prefersReducedMotion.value) {
    return
  }

  variantTimer = setInterval(() => {
    activeVariantIndex.value = (activeVariantIndex.value + 1) % titleVariants.value.length
  }, 2200)
})

onBeforeUnmount(() => {
  if (variantTimer) {
    clearInterval(variantTimer)
  }
})
</script>

<style scoped lang="scss">
.hero {
  padding-top: 140px;
  padding-bottom: 96px;
  position: relative;
}

.hero__container {
  position: relative;
  z-index: 1;
}

.hero__content {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 32px;
  align-items: center;
}

.hero__copy {
  max-width: 640px;
}

.hero__badge {
  margin-bottom: 18px;
}

.hero__title {
  font-size: clamp(2rem, 5vw, 4rem);
  line-height: 1.05;
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.hero__title-word {
  min-height: 1.2em;
}

.hero__title-emphasis {
  background: linear-gradient(90deg, rgb(var(--v-theme-primary)), rgba(var(--v-theme-primary), 0.7));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  display: inline-block;
}

.hero__subtitle {
  max-width: 58ch;
  margin: 0;
  color: var(--inf-token-color-text-secondary);
  font-size: 1.0625rem;
}

.hero__signals {
  max-width: 680px;
}

.hero-signal {
  padding: 14px 16px;
  min-height: 100%;
  border-color: var(--inf-token-color-line-subtle);
  background:
    linear-gradient(180deg, rgba(var(--v-theme-surface), 0.56), rgba(var(--v-theme-surface), 0.28));
}

.hero-signal__value {
  font-size: 1rem;
  font-weight: 700;
}

.hero-signal__label {
  margin-top: 4px;
  color: var(--inf-token-color-text-secondary);
  font-size: 0.8125rem;
}

.hero__surface {
  padding: 24px;
  border-color: var(--inf-token-color-line-subtle);
  background:
    linear-gradient(180deg, rgba(var(--v-theme-surface), 0.72), rgba(var(--v-theme-surface), 0.42));
  backdrop-filter: blur(12px);
}

.hero__surface-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hero__surface-eyebrow {
  color: var(--inf-token-color-text-secondary);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.hero__surface-title {
  margin-top: 6px;
  font-size: 1.125rem;
  font-weight: 600;
}

.hero-track {
  min-height: 100%;
  padding: 16px;
  border: 1px solid var(--inf-token-color-line-subtle);
}

.hero-track--enterprise {
  background: rgba(var(--v-theme-primary), 0.08);
}

.hero-track--provider {
  background: rgba(var(--v-theme-info), 0.08);
}

.hero-track__label {
  color: var(--inf-token-color-text-secondary);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.hero-track__title {
  margin-top: 8px;
  font-size: 1rem;
  font-weight: 600;
}

.hero-track__text {
  margin-top: 8px;
  color: var(--inf-token-color-text-secondary);
  font-size: 0.9375rem;
}

.hero-audience :deep(.v-chip) {
  border: 1px solid var(--inf-token-color-line-subtle);
}

@media (max-width: 959px) {
  .hero {
    padding-top: 124px;
    padding-bottom: 72px;
  }

  .hero__content {
    grid-template-columns: 1fr;
  }

  .hero__title {
    align-items: flex-start;
  }
}
</style>
