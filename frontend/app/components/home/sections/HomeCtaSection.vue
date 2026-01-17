<script setup lang="ts">
import { computed, toRefs } from 'vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'
import { useSeasonalEventPack } from '~~/app/composables/useSeasonalEventPack'
import { useEventPackI18n } from '~/composables/useEventPackI18n'

type Emits = {
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}

const props = withDefaults(
  defineProps<{
    searchQuery: string
    categoriesLandingUrl: string
    minSuggestionQueryLength?: number
  }>(),
  {
    minSuggestionQueryLength: 2,
  }
)

const emit = defineEmits<Emits>()

const { t, te } = useI18n()

const { categoriesLandingUrl, minSuggestionQueryLength } = toRefs(props)

const searchQueryValue = computed(() => props.searchQuery)

const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)

const resolveSearchString = (path: string, fallbackKey: string) =>
  packI18n.resolveString(path, { fallbackKeys: [fallbackKey] }) ??
  (te(fallbackKey) ? String(t(fallbackKey)) : '')

const handleSubmit = () => {
  emit('submit')
}

const handleUpdate = (value: string) => {
  emit('update:searchQuery', value)
}

const handleCategorySelect = (value: CategorySuggestionItem) => {
  emit('select-category', value)
}

const handleProductSelect = (value: ProductSuggestionItem) => {
  emit('select-product', value)
}
</script>

<template>
  <section class="home-section home-cta" aria-labelledby="home-cta-title">
    <v-container fluid class="home-section__container">
      <v-row justify="center">
        <v-col cols="12" lg="6">
          <NudgerCard
            class="home-cta__content"
            background="rgba(var(--v-theme-surface-default), 0.96)"
            padding="clamp(2rem, 5vw, 3rem)"
            base-radius="clamp(1.75rem, 4vw, 2.5rem)"
          >
            <p id="home-cta-title" class="home-hero__subtitle">
              {{ t('home.cta.title') }}
            </p>
            <p class="home-section__subtitle text-center">
              {{ t('home.cta.subtitle') }}
            </p>
            <form
              class="home-hero__search"
              role="search"
              @submit.prevent="handleSubmit"
            >
              <SearchSuggestField
                :model-value="searchQueryValue"
                class="home-hero__search-input"
                :label="
                  resolveSearchString(
                    'hero.search.label',
                    'home.hero.search.label'
                  )
                "
                :placeholder="
                  resolveSearchString(
                    'hero.search.placeholder',
                    'home.hero.search.placeholder'
                  )
                "
                :aria-label="
                  resolveSearchString(
                    'hero.search.ariaLabel',
                    'home.hero.search.ariaLabel'
                  )
                "
                :min-chars="minSuggestionQueryLength"
                :enable-scan="true"
                :scan-mobile="true"
                :scan-desktop="false"
                :enable-voice="true"
                :voice-mobile="true"
                :voice-desktop="true"
                @update:model-value="handleUpdate"
                @submit="handleSubmit"
                @select-category="handleCategorySelect"
                @select-product="handleProductSelect"
              >
                <template #append-inner>
                  <v-btn
                    class="home-hero__search-submit nudger_degrade-defaut"
                    icon="mdi-arrow-right"
                    variant="flat"
                    color="primary"
                    size="small"
                    type="submit"
                    :aria-label="t('home.cta.searchSubmit')"
                  />
                </template>
              </SearchSuggestField>
            </form>
            <div class="home-cta__actions">
              <div class="home-cta__links">
                <v-btn
                  class="home-section__cta nudger_degrade-defaut mx-auto"
                  :to="categoriesLandingUrl"
                  color="primary"
                  variant="tonal"
                  size="large"
                >
                  {{ t('home.cta.browseTaxonomy') }}
                </v-btn>
              </div>
            </div>
          </NudgerCard>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-section__container
  padding-inline: 0

.home-cta__content
  display: flex
  flex-direction: column
  gap: clamp(0.875rem, 2vw, 1.25rem);
  box-shadow: 0 24px 36px rgba(var(--v-theme-shadow-primary-600), 0.15)

.home-hero__search
  display: flex
  flex-direction: column
  gap: 0.75rem
  width: 100%

.home-hero__search-input
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  background: rgba(var(--v-theme-surface-default), 0.92)
  box-shadow: 0 14px 22px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-hero__search-submit
  box-shadow: none
</style>
