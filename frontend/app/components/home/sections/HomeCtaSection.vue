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
  },
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
        <v-col cols="12" lg="8">
          <div class="home-cta__content">
            <h2 id="home-cta-title" class="home-cta__title">{{ t('home.cta.title') }}</h2>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="home-cta__subtitle subtitle-text" v-html="t('home.cta.subtitle')" />
            <div class="home-cta__actions">
              <form class="home-cta__form" role="search" @submit.prevent="handleSubmit">
                <SearchSuggestField
                  :model-value="searchQueryValue"
                  class="home-cta__search-input"
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
                      class="home-cta__submit"
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
              <div class="home-cta__links">
                <v-btn
                  class="home-cta__browse"
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
  padding-block: clamp(3.5rem, 7vw, 5.5rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.8), rgba(var(--v-theme-surface-default), 0.95))

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-cta__content
  display: flex
  flex-direction: column
  gap: clamp(1.5rem, 4vw, 2.25rem)
  padding: clamp(2rem, 5vw, 3rem)
  border-radius: clamp(1.75rem, 4vw, 2.5rem)
  background: rgba(var(--v-theme-surface-default), 0.96)
  box-shadow: 0 24px 36px rgba(var(--v-theme-shadow-primary-600), 0.15)

.home-cta__title
  margin: 0
  font-size: clamp(1.8rem, 4vw, 2.4rem)
  font-weight: 700

.home-cta__subtitle
  margin: 0

.home-cta__actions
  display: flex
  flex-direction: column
  gap: clamp(1.25rem, 3vw, 1.75rem)

.home-cta__search-input
  border-radius: clamp(1.5rem, 4vw, 2rem)
  background: rgba(var(--v-theme-surface-default), 0.85)

.home-cta__submit
  box-shadow: none

.home-cta__links
  display: flex
  flex-wrap: wrap
  gap: 1rem

.home-cta__browse
  min-width: 240px

@media (max-width: 599px)
  .home-cta__content
    padding: clamp(1.5rem, 6vw, 2.25rem)
</style>
