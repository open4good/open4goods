<template>
  <v-row dense class="category-hero-actions" align="stretch">
    <v-col
      v-if="hasEcoscore"
      cols="12"
      md="6"
      class="category-hero-actions__col"
    >
      <CategoryEcoscoreCard
        class="category-hero-actions__ecoscore"
        :vertical-home-url="verticalHomeUrl"
        :category-name="categoryName"
      />
    </v-col>

    <v-col v-if="hasNudge" cols="12" md="6" class="category-hero-actions__col">
      <HeroActionCard class="category-hero-actions__nudge-card">
        <div class="category-hero-actions__nudge">
          <p class="category-hero-actions__eyebrow">
            {{ t('category.hero.nudge.eyebrow') }}
          </p>
          <v-tooltip :text="t('category.hero.nudge.subtitle')">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                color="primary"
                variant="flat"
                prepend-icon="mdi-robot-love"
                class="category-hero-actions__cta"
                @click="emit('open-nudge')"
              >
                {{ t('category.hero.nudge.cta') }}
              </v-btn>
            </template>
          </v-tooltip>
        </div>
      </HeroActionCard>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import CategoryEcoscoreCard from '~/components/category/CategoryEcoscoreCard.vue'
import HeroActionCard from '~/components/shared/HeroActionCard.vue'

const props = withDefaults(
  defineProps<{
    verticalHomeUrl?: string | null
    categoryName?: string | null
    showNudge?: boolean
    showEcoscore?: boolean
  }>(),
  {
    verticalHomeUrl: null,
    categoryName: null,
    showNudge: true,
    showEcoscore: true,
  }
)

const emit = defineEmits<{
  (event: 'open-nudge'): void
}>()

const { t } = useI18n()

const hasEcoscore = computed(
  () => props.showEcoscore && Boolean(props.verticalHomeUrl)
)
const hasNudge = computed(() => props.showNudge)
</script>

<style scoped lang="sass">
.category-hero-actions
  margin: 0


  &__col
    display: flex

  &__ecoscore,
  &__nudge-card
    width: 100%
    height: 100%
    min-height: 160px

  &__nudge-card
    // Ensure the card content is flex to center the nudge content if needed, or just let it flow.
    // HeroActionCard slots might not facilitate centering by default, checking its implementation...
    // It has a content div.
    display: flex
    flex-direction: column
    justify-content: center

  &__nudge
    display: flex
    flex-direction: column
    align-items: flex-start
    gap: 0.5rem
    width: 100%

  &__eyebrow
    margin: 0
    font-size: 0.85rem
    letter-spacing: 0.08em
    text-transform: uppercase
    // Ensure good contrast on the card background (ActionCard often has dark/complex backgrounds)
    color: rgba(255, 255, 255, 0.9)
    text-shadow: 0 1px 2px rgba(0,0,0,0.3)

  &__cta
    box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.18)
</style>
