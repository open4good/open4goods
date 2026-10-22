<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  OPENDATA_EXPLORE_CATEGORIES,
  type OpenDataExploreCategoryProducts,
} from '~~/shared/utils/opendata-explore'
import type { OpenDataLinkPage } from '~/composables/useAnalytics'

const props = defineProps<{
  page: OpenDataLinkPage
}>()

const { t, locale } = useI18n()
const localePath = useLocalePath()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
const { trackOpenDataOutboundClick } = useAnalytics()

const { data } = await useAsyncData<OpenDataExploreCategoryProducts[]>(
  'opendata-explore-products',
  async () => {
    try {
      return await $fetch<OpenDataExploreCategoryProducts[]>(
        '/api/opendata/explore',
        { headers: requestHeaders }
      )
    } catch (error) {
      // The block is optional: an API failure must never break the page.
      console.error('Failed to load open data explore products', error)
      return []
    }
  }
)

const language = computed<'fr' | 'en'>(() =>
  locale.value.startsWith('fr') ? 'fr' : 'en'
)

const categories = computed(() =>
  OPENDATA_EXPLORE_CATEGORIES.map(category => {
    const products =
      data.value?.find(entry => entry.id === category.id)?.products ?? []

    return {
      id: category.id,
      label: String(t(`opendata.explore.categories.${category.id}`)),
      href: localePath(`/${category.slugs[language.value]}`),
      products,
    }
  })
)
</script>

<template>
  <section class="opendata-explore" aria-labelledby="opendata-explore-heading">
    <v-container max-width="lg">
      <h2 id="opendata-explore-heading" class="opendata-explore__title">
        {{ t('opendata.explore.title') }}
      </h2>
      <p class="opendata-explore__subtitle">
        {{ t('opendata.explore.subtitle') }}
      </p>

      <v-row>
        <v-col
          v-for="category in categories"
          :key="category.id"
          cols="12"
          md="6"
          lg="4"
        >
          <v-card
            class="opendata-explore__card"
            variant="tonal"
            rounded="xl"
            :data-category="category.id"
          >
            <v-card-item>
              <h3 class="opendata-explore__category">
                <NuxtLink
                  :to="category.href"
                  class="opendata-explore__category-link"
                  :aria-label="
                    t('opendata.explore.categoryAriaLabel', {
                      category: category.label,
                    })
                  "
                  @click="
                    trackOpenDataOutboundClick({
                      block: 'explore-products',
                      target: 'category',
                      page: props.page,
                      category: category.id,
                    })
                  "
                >
                  {{ category.label }}
                </NuxtLink>
              </h3>
            </v-card-item>
            <v-card-text v-if="category.products.length">
              <ul class="opendata-explore__products">
                <li
                  v-for="product in category.products"
                  :key="product.gtin"
                  class="opendata-explore__product"
                >
                  <NuxtLink
                    :to="product.href"
                    class="opendata-explore__product-link"
                    @click="
                      trackOpenDataOutboundClick({
                        block: 'explore-products',
                        target: 'product',
                        page: props.page,
                        category: category.id,
                      })
                    "
                  >
                    {{ product.name }}
                  </NuxtLink>
                  <span class="opendata-explore__gtin">
                    {{ t('opendata.explore.gtinLabel') }}
                    <code>{{ product.gtin }}</code>
                  </span>
                </li>
              </ul>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-explore
  padding: clamp(2rem, 5vw, 3.5rem) 0

.opendata-explore__title
  margin: 0 0 0.5rem
  font-size: clamp(1.75rem, 3vw, 2.2rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-explore__subtitle
  margin: 0 0 1.5rem
  color: rgba(var(--v-theme-text-neutral-secondary), 1)

.opendata-explore__card
  height: 100%

.opendata-explore__category
  margin: 0
  font-size: 1.25rem

.opendata-explore__category-link
  color: rgb(var(--v-theme-primary))
  font-weight: 700

.opendata-explore__products
  list-style: none
  margin: 0
  padding: 0
  display: flex
  flex-direction: column
  gap: 0.75rem

.opendata-explore__product
  display: flex
  flex-direction: column
  gap: 0.15rem

.opendata-explore__product-link
  color: rgba(var(--v-theme-text-neutral-strong), 1)
  text-decoration: underline

.opendata-explore__gtin
  font-size: 0.85rem
  color: rgba(var(--v-theme-text-neutral-secondary), 1)
</style>
