<script setup lang="ts">
import { computed, toRefs } from 'vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'

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

const { t } = useI18n()

const { categoriesLandingUrl, minSuggestionQueryLength } = toRefs(props)

const searchQueryValue = computed(() => props.searchQuery)

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
          <div class="home-cta__content card__nudger">
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
                :label="t('home.hero.search.label')"
                :placeholder="t('home.hero.search.placeholder')"
                :aria-label="t('home.hero.search.ariaLabel')"
                :min-chars="minSuggestionQueryLength"
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
          </div>
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
  padding: clamp(2rem, 5vw, 3rem)
  border-radius: clamp(1.75rem, 4vw, 2.5rem)
  background: rgba(var(--v-theme-surface-default), 0.96)
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
