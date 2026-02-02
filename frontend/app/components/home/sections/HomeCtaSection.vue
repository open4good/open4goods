<script setup lang="ts">
import { computed, toRefs } from 'vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'

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
    reveal?: boolean
  }>(),
  {
    minSuggestionQueryLength: 2,
  }
)

const emit = defineEmits<Emits>()

const { t, te, tm } = useI18n()

const { categoriesLandingUrl, minSuggestionQueryLength } = toRefs(props)

const searchQueryValue = computed(() => props.searchQuery)

const isVisible = computed(() => Boolean(props.reveal))

const resolveSearchString = (key: string) =>
  te(key) ? String(t(key)) : ''

const resolveSearchPlaceholder = () => {
  if (te('home.hero.search.placeholders')) {
    const value = tm('home.hero.search.placeholders')
    if (Array.isArray(value)) {
      return value.map(entry => String(entry))
    }
  }

  return resolveSearchString('home.hero.search.placeholder')
}

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
    <NudgerCard
      class="home-cta__content home-reveal-group"
      :class="{ 'is-ready': true, 'is-visible': isVisible }"
      background="rgba(var(--v-theme-surface-default), 0.96)"
      padding="clamp(2rem, 5vw, 3rem)"
      base-radius="clamp(1.75rem, 4vw, 2.5rem)"
    >
      <p
        id="home-cta-title"
        class="home-hero__subtitle home-reveal-item ma-0"
        :style="{ '--reveal-delay': '0ms' }"
      >
        {{ t('home.cta.title') }}
      </p>
      <form
        class="home-hero__search home-reveal-item d-flex flex-column ga-2 w-100"
        :style="{ '--reveal-delay': '200ms' }"
        role="search"
        @submit.prevent="handleSubmit"
      >
        <SearchSuggestField
          :model-value="searchQueryValue"
          class="home-hero__search-input"
          :label="resolveSearchString('home.hero.search.label')"
          :placeholder="resolveSearchPlaceholder()"
          :aria-label="resolveSearchString('home.hero.search.ariaLabel')"
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
              class="home-hero__search-submit"
              icon="mdi-arrow-right"
              variant="plain"
              rounded="0"
              size="small"
              type="submit"
              :aria-label="t('home.cta.searchSubmit')"
            />
          </template>
        </SearchSuggestField>
      </form>
      <div
        class="home-cta__actions home-reveal-item home-reveal-item--scale"
        :style="{ '--reveal-delay': '300ms' }"
      >
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
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: 0
  background: transparent

.home-section__container
  padding-inline: 0

.home-cta__content
  // d-flex flex-column ga-3 styles kept in SASS as base, supplemented by component
  display: flex
  flex-direction: column
  gap: clamp(0.875rem, 2vw, 1.25rem)
  box-shadow: 0 24px 36px rgba(var(--v-theme-shadow-primary-600), 0.15)

.home-hero__search
  // d-flex flex-column ga-2 w-100 now handled by utility classes
  // keeping width in sass
  width: 100%

.home-hero__search-input
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  background: rgba(var(--v-theme-surface-default), 0.92)
  box-shadow: 0 14px 22px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-hero__search-submit
  box-shadow: none
</style>
