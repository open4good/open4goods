<template>
  <section class="home-crawl-links" data-testid="home-crawl-links">
    <v-container fluid class="max_large mx-auto px-4 py-8">
      <h2 class="text-h5 mb-4">{{ t('home.crawlLinks.title') }}</h2>

      <v-row>
        <v-col v-if="guides.length" cols="12" md="6">
          <h3 class="text-subtitle-1 mb-2">
            {{ t('home.crawlLinks.guides') }}
          </h3>
          <ul class="home-crawl-links__list">
            <li v-for="guide in guides" :key="guide.href">
              <NuxtLink :to="guide.href">{{ guide.label }}</NuxtLink>
            </li>
          </ul>
        </v-col>

        <v-col v-if="products.length" cols="12" md="6">
          <h3 class="text-subtitle-1 mb-2">
            {{ t('home.crawlLinks.products') }}
          </h3>
          <ul class="home-crawl-links__list">
            <li v-for="item in products" :key="item.fullSlug">
              <NuxtLink :to="item.fullSlug">{{ productTitle(item) }}</NuxtLink>
            </li>
          </ul>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'
import { resolveProductTitle } from '~/utils/_product-title-resolver'
import { humanizeSlug } from '~/utils/_product-title'
import { pickRelatedProducts } from '~/utils/_related-links'
import { useVerticalsLinks } from '~/composables/categories/useVerticalsLinks'

const MAX_GUIDES = 8
const MAX_PRODUCTS = 6

const { t, locale } = useI18n()
const verticals = await useVerticalsLinks()

const { data: randomProducts } = await useAsyncData<ProductDto[]>(
  'home-crawl-products',
  async () => {
    try {
      return await $fetch<ProductDto[]>('/api/stats/random', {
        query: { num: MAX_PRODUCTS * 2, minOffersCount: 3 },
      })
    } catch (error) {
      console.error('[Home] Failed to load featured products.', error)
      return []
    }
  },
  { default: () => [] }
)

const guides = computed(() =>
  verticals.value
    .flatMap(vertical =>
      vertical.guides.map(guide => ({
        href: guide.href,
        label: `${vertical.title} : ${humanizeSlug(guide.slug, locale.value) || guide.slug}`,
      }))
    )
    .slice(0, MAX_GUIDES)
)

const products = computed(() =>
  pickRelatedProducts(randomProducts.value, null, MAX_PRODUCTS)
)

const productTitle = (item: ProductDto) =>
  resolveProductTitle(item, locale.value, { uppercaseBrand: true }) ||
  item.slug ||
  ''
</script>

<style scoped lang="sass">
.home-crawl-links__list
  list-style: none
  margin: 0
  padding: 0

  li
    padding-block: 4px
</style>
