<template>
  <v-card
    v-if="hasContent"
    class="product-related-links"
    variant="flat"
    data-testid="product-related-links"
  >
    <v-card-title class="text-h6">
      {{ t('product.relatedLinks.title') }}
    </v-card-title>
    <v-card-text>
      <nav
        class="d-flex flex-wrap ga-2 mb-4"
        :aria-label="t('product.relatedLinks.navLabel')"
      >
        <v-btn
          v-if="subCategory"
          :to="subCategory.href"
          variant="tonal"
          color="primary"
          size="small"
          data-testid="product-subcategory-link"
        >
          {{ subCategory.title }}
        </v-btn>
        <v-btn
          v-if="verticalHomeUrl"
          :to="verticalHomeUrl"
          variant="text"
          size="small"
        >
          {{
            t('product.relatedLinks.allOfVertical', { vertical: verticalTitle })
          }}
        </v-btn>
      </nav>

      <template v-if="products.length">
        <h3 class="text-subtitle-1 mb-2">
          {{ productsHeading }}
        </h3>
        <ul class="product-related-links__list">
          <li v-for="item in products" :key="item.fullSlug">
            <NuxtLink :to="item.fullSlug">
              {{ productTitle(item) }}
            </NuxtLink>
          </li>
        </ul>
      </template>

      <template v-if="guideLinks.length">
        <h3 class="text-subtitle-1 mt-4 mb-2">
          {{ t('product.relatedLinks.guides') }}
        </h3>
        <ul class="product-related-links__list">
          <li v-for="guide in guideLinks" :key="guide.href">
            <NuxtLink :to="guide.href">{{ guide.label }}</NuxtLink>
          </li>
        </ul>
      </template>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'
import { resolveProductTitle } from '~/utils/_product-title-resolver'
import { humanizeSlug } from '~/utils/_product-title'

const props = defineProps<{
  products: ProductDto[]
  subCategory: { title: string; href: string } | null
  verticalHomeUrl: string
  verticalTitle: string
  guides: string[]
}>()

const { t, locale } = useI18n()

const productsHeading = computed(() =>
  props.subCategory
    ? t('product.relatedLinks.sameSubCategory', {
        subCategory: props.subCategory.title,
      })
    : t('product.relatedLinks.sameVertical', { vertical: props.verticalTitle })
)

const productTitle = (item: ProductDto) =>
  resolveProductTitle(item, locale.value, { uppercaseBrand: true }) ||
  item.slug ||
  ''

const guideLinks = computed(() =>
  props.verticalHomeUrl
    ? props.guides.map(slug => ({
        label: humanizeSlug(slug, locale.value) || slug,
        href: `${props.verticalHomeUrl.replace(/\/+$/, '')}/${slug}`,
      }))
    : []
)

const hasContent = computed(
  () =>
    props.products.length > 0 ||
    guideLinks.value.length > 0 ||
    Boolean(props.subCategory)
)
</script>

<style scoped lang="sass">
.product-related-links__list
  columns: 2 260px
  list-style: none
  margin: 0
  padding: 0

  li
    padding-block: 4px
    break-inside: avoid
</style>
