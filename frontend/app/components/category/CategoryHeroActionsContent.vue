<template>
  <v-row dense class="category-hero-actions" align="stretch">
    <v-col
      v-if="hasEcoscore"
      cols="12"
      :md="columnMd"
      class="category-hero-actions__col"
    >
      <CategoryEcoscoreCard
        class="category-hero-actions__ecoscore"
        :vertical-home-url="verticalHomeUrl"
        :category-name="categoryName"
      />
    </v-col>

    <v-col
      v-if="hasNudge"
      cols="12"
      :md="columnMd"
      class="category-hero-actions__col"
    >
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
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import CategoryEcoscoreCard from '~/components/category/CategoryEcoscoreCard.vue'

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
const columnMd = computed(() =>
  hasEcoscore.value && hasNudge.value ? 6 : 12
)
</script>

<style scoped lang="sass">
.category-hero-actions
  margin: 0

  &__col
    display: flex

  &__ecoscore
    width: 100%

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
    color: rgba(var(--v-theme-accent-supporting), 0.9)

  &__cta
    box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.18)
</style>
