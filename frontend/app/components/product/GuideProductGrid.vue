<template>
  <div class="guide-product-grid">
    <v-row v-if="pending" class="guide-product-grid__skeletons">
      <v-col
        v-for="index in topCount"
        :key="index"
        cols="12"
        sm="6"
        :md="topCount >= 4 ? 3 : 4"
      >
        <v-skeleton-loader type="image, article" class="rounded-xl" />
      </v-col>
    </v-row>

    <CategoryProductCardGrid
      v-else-if="products.length"
      :products="products"
      size="small"
    />

    <p v-else class="guide-product-grid__empty">
      {{ t('buyingGuide.widget.empty') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  ProductDto,
  ProductSearchResponseDto,
  SortRequestDto,
} from '~~/shared/api-client'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'
import { useGuideContext } from '~/composables/useGuideContext'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

const LISTING_INCLUDE = 'base,identity,names,attributes,resources,scores,offers'

const props = withDefaults(
  defineProps<{
    vertical?: string
    top?: string | number
    sort?: string
  }>(),
  {
    vertical: undefined,
    top: 3,
    sort: 'ecoscore',
  }
)

const { t } = useI18n()
const guideContext = useGuideContext()

const normalizedVertical = computed(() =>
  `${props.vertical ?? guideContext?.verticalId ?? ''}`.trim()
)
const topCount = computed(() => {
  const parsed = Number.parseInt(`${props.top}`, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 3
})

// `sort` accepts a known keyword (ecoscore) or a raw field mapping. Order can be
// suffixed with ":asc" / ":desc"; impact score defaults to descending.
const sortRequest = computed<SortRequestDto>(() => {
  const raw = (props.sort ?? 'ecoscore').trim()
  const [keyword, order] = raw.split(':')
  const field =
    !keyword || keyword === 'ecoscore' || keyword === 'impact'
      ? ECOSCORE_RELATIVE_FIELD
      : keyword

  return {
    sorts: [
      {
        field,
        order: order === 'asc' ? 'asc' : 'desc',
      },
    ],
  }
})

const { data, pending } = await useAsyncData<ProductDto[]>(
  () =>
    `guide-product-grid:${normalizedVertical.value}:${topCount.value}:${props.sort}`,
  async () => {
    if (!normalizedVertical.value) {
      return []
    }

    try {
      const response = await $fetch<ProductSearchResponseDto>(
        '/api/products/search',
        {
          method: 'POST',
          query: { include: LISTING_INCLUDE },
          body: {
            verticalId: normalizedVertical.value,
            pageNumber: 0,
            pageSize: topCount.value,
            sort: sortRequest.value,
          },
        }
      )

      return response.products?.data ?? []
    } catch (error) {
      console.error('Failed to load guide product grid', error)
      return []
    }
  },
  {
    watch: [normalizedVertical, topCount, () => props.sort],
  }
)

const products = computed(() => data.value ?? [])
</script>

<style scoped lang="sass">
.guide-product-grid
  margin: 1.5rem 0

  &__empty
    font-size: 0.85rem
    font-style: italic
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>
