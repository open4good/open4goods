<template>
  <section
    class="tv-size-calculator"
    :aria-labelledby="titleId"
    data-test="tv-size-calculator"
  >
    <h3 :id="titleId" class="tv-size-calculator__title">
      {{ t('blog.tvSizeCalculator.title') }}
    </h3>

    <div class="tv-size-calculator__form">
      <div class="tv-size-calculator__field">
        <label :for="distanceId">
          {{ t('blog.tvSizeCalculator.distanceLabel') }}
        </label>
        <input
          :id="distanceId"
          v-model="distanceInput"
          class="tv-size-calculator__input"
          type="number"
          inputmode="decimal"
          step="0.1"
          :min="TV_DISTANCE_MIN_M"
          :max="TV_DISTANCE_MAX_M"
          :aria-invalid="distanceInvalid"
          :aria-describedby="hintId"
          data-test="tv-size-distance"
        />
        <p
          :id="hintId"
          class="tv-size-calculator__hint"
          :class="{ 'tv-size-calculator__hint--error': distanceInvalid }"
        >
          {{
            t('blog.tvSizeCalculator.distanceHint', {
              min: TV_DISTANCE_MIN_M,
              max: TV_DISTANCE_MAX_M,
            })
          }}
        </p>
      </div>

      <fieldset class="tv-size-calculator__field tv-size-calculator__radios">
        <legend>{{ t('blog.tvSizeCalculator.resolutionLabel') }}</legend>
        <label v-for="option in RESOLUTIONS" :key="option">
          <input
            v-model="resolution"
            type="radio"
            :name="radioName"
            :value="option"
            :data-test="`tv-size-resolution-${option}`"
          />
          {{ t(`blog.tvSizeCalculator.resolution.${option}`) }}
        </label>
      </fieldset>
    </div>

    <div
      class="tv-size-calculator__result"
      aria-live="polite"
      aria-atomic="true"
      data-test="tv-size-result"
    >
      <p class="tv-size-calculator__range">
        {{
          t('blog.tvSizeCalculator.range', {
            minInches: result.minInches,
            maxInches: result.maxInches,
            minCm: formatNumber(result.minCm),
            maxCm: formatNumber(result.maxCm),
          })
        }}
      </p>
      <p class="tv-size-calculator__recommended">
        <strong>{{
          t('blog.tvSizeCalculator.recommended', {
            inches: result.recommendedInches,
          })
        }}</strong>
        <span v-if="result.beyond">
          {{ t(`blog.tvSizeCalculator.beyond.${result.beyond}`) }}
        </span>
      </p>
      <p class="tv-size-calculator__dimensions">
        {{
          t('blog.tvSizeCalculator.dimensions', {
            width: formatNumber(result.widthCm),
            height: formatNumber(result.heightCm),
          })
        }}
      </p>
    </div>

    <NuxtLink
      v-if="linkUrl"
      :to="linkUrl"
      class="tv-size-calculator__link"
      data-test="tv-size-link"
      @click="onLinkClick"
    >
      {{ t('blog.tvSizeCalculator.link') }}
    </NuxtLink>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAnalytics } from '~/composables/useAnalytics'
import { useCategories } from '~/composables/categories/useCategories'
import {
  computeTvSize,
  TV_DEFAULT_RESOLUTION,
  TV_DISTANCE_DEFAULT_M,
  TV_DISTANCE_MAX_M,
  TV_DISTANCE_MIN_M,
  TV_SIZE_SUBCATEGORY,
  type TvResolution,
} from '~/utils/tv-size'

/**
 * Viewing distance → TV size calculator (GOU-2713), used in a blog article through
 * `::tv-size-calculator{default-distance="3" default-resolution="4k"}`
 * (see shared/utils/blog-product-widget.ts). The default result is computed during setup, so it is
 * in the SSR HTML; the inputs recompute it reactively on the client.
 */
const props = withDefaults(
  defineProps<{
    defaultDistance?: number
    defaultResolution?: TvResolution
  }>(),
  {
    defaultDistance: TV_DISTANCE_DEFAULT_M,
    defaultResolution: TV_DEFAULT_RESOLUTION,
  }
)

const RESOLUTIONS: TvResolution[] = ['4k', 'full-hd']
const TRACK_DEBOUNCE_MS = 800
const TV_VERTICAL = 'tv'

const { t, locale } = useI18n()
const { trackEvent } = useAnalytics()
const { fetchCategoryById } = useCategories()

const uid = useId()
const titleId = `tv-size-title-${uid}`
const distanceId = `tv-size-distance-${uid}`
const hintId = `tv-size-hint-${uid}`
const radioName = `tv-size-resolution-${uid}`

const distanceInput = ref<number | string>(props.defaultDistance)
const resolution = ref<TvResolution>(props.defaultResolution)

const distanceValue = computed(() => Number(distanceInput.value))
const distanceInvalid = computed(
  () =>
    !Number.isFinite(distanceValue.value) ||
    distanceValue.value < TV_DISTANCE_MIN_M ||
    distanceValue.value > TV_DISTANCE_MAX_M
)
const result = computed(() =>
  computeTvSize(distanceValue.value, resolution.value)
)

const formatNumber = (value: number) =>
  new Intl.NumberFormat(locale.value, { maximumFractionDigits: 1 }).format(
    value
  )

// Same source as GuideProductGrid: the category detail gives `verticalHomeUrl` and the sub-category slugs.
const { data: category } = await useAsyncData(
  'tv-size-calculator:category',
  async () => {
    try {
      return await fetchCategoryById(TV_VERTICAL)
    } catch (error) {
      console.error('Failed to load the TV category for the calculator', error)
      return null
    }
  }
)

const linkUrl = computed(() => {
  const subcategoryId = TV_SIZE_SUBCATEGORY[result.value.recommendedInches]
  const slug = category.value?.subCategories
    ?.find(candidate => candidate.id === subcategoryId)
    ?.slug?.trim()
  if (!slug) {
    return null
  }
  const home = category.value?.verticalHomeUrl?.replace(/^\/+|\/+$/g, '') ?? ''
  return `/${home ? `${home}/` : ''}${slug.replace(/^\/+/, '')}`
})

const track = (action: 'calcul' | 'clic_sous_categorie') =>
  trackEvent('calculateur_tv', {
    props: {
      action,
      taille: result.value.recommendedInches,
      resolution: result.value.resolution,
    },
  })

// Not immediate: nothing is sent at render, only after a change by the user.
let debounceTimer: ReturnType<typeof setTimeout> | undefined
watch([distanceInput, resolution], () => {
  clearTimeout(debounceTimer)
  if (distanceInvalid.value) {
    return
  }
  debounceTimer = setTimeout(() => track('calcul'), TRACK_DEBOUNCE_MS)
})
onBeforeUnmount(() => clearTimeout(debounceTimer))

const onLinkClick = () => track('clic_sous_categorie')
</script>

<style scoped lang="sass">
.tv-size-calculator
  margin: 1.5rem 0
  padding: 1rem 1.25rem
  border: 1px solid rgba(var(--v-theme-on-surface), 0.2)
  border-radius: 12px
  background: rgb(var(--v-theme-surface))
  color: rgb(var(--v-theme-on-surface))

  &__title
    margin: 0 0 0.75rem

  &__form
    display: flex
    flex-wrap: wrap
    gap: 1rem 2rem

  &__field
    display: flex
    flex-direction: column
    gap: 0.25rem
    border: 0
    padding: 0
    margin: 0

  &__input
    min-height: 44px
    width: 8rem
    padding: 0 0.75rem
    border: 1px solid rgba(var(--v-theme-on-surface), 0.5)
    border-radius: 8px
    background: transparent
    color: inherit
    font: inherit

    &[aria-invalid="true"]
      border-color: rgb(var(--v-theme-error))

  &__hint
    margin: 0
    font-size: 0.875rem
    opacity: 0.8

    &--error
      color: rgb(var(--v-theme-error))
      opacity: 1

  &__radios label
    display: flex
    align-items: center
    gap: 0.5rem
    min-height: 44px

  &__result
    margin-top: 1rem

    p
      margin: 0.25rem 0

  &__link
    display: inline-block
    margin-top: 0.5rem
    font-weight: 600
</style>
