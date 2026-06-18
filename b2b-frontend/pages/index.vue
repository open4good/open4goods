<template>
  <div>
    <section class="py-18 pt-16">
      <v-container>
        <v-row align="center" class="ga-8 ga-md-0">
          <v-col cols="12" md="6">
            <p class="text-overline text-primary mb-3">{{ t('home.hero.eyebrow') }}</p>
            <h1 class="text-h3 text-md-h2 font-weight-bold mb-4">{{ t('home.hero.title') }}</h1>
            <p class="text-body-1 text-medium-emphasis mb-6" style="max-width: 620px;">
              {{ t('home.hero.subtitle') }}
            </p>
            <div class="d-flex flex-wrap ga-3">
              <v-btn color="primary" size="large" prepend-icon="mdi-login" to="/auth/login">
                {{ t('landing.nav.createAccount') }}
              </v-btn>
              <v-btn variant="tonal" size="large" prepend-icon="mdi-book-open-outline" to="/docs/getting-started">
                {{ t('home.hero.docsCta') }}
              </v-btn>
            </div>
          </v-col>
          <v-col cols="12" md="6">
            <B2bOrbitalDataVisual />
          </v-col>
        </v-row>
      </v-container>
    </section>

    <v-container class="py-10">
      <v-row>
        <v-col v-for="proof in proofItems" :key="proof.icon" cols="12" md="3">
          <B2bKpiCard :label="proof.title" :value="proof.value" :caption="proof.caption" :icon="proof.icon" />
        </v-col>
      </v-row>
    </v-container>

    <v-container class="py-8">
      <B2bServiceCarousel :services="carouselServices" :title="t('home.services.title')" />
      <div class="d-flex justify-end mt-4">
        <v-btn variant="text" to="/solutions" append-icon="mdi-arrow-right">
          {{ t('landing.nav.allSolutions') }}
        </v-btn>
      </div>
    </v-container>

    <v-container class="py-8">
      <v-row>
        <v-col cols="12" md="6">
          <B2bPageHeader :title="t('home.api.title')" :subtitle="t('home.api.subtitle')" />
          <B2bCodeBlock :code="curlExample" language="bash" />
        </v-col>
        <v-col cols="12" md="6">
          <B2bPageHeader :title="t('home.billing.title')" :subtitle="t('home.billing.subtitle')" />
          <v-list class="bg-transparent">
            <v-list-item v-for="item in billingRules" :key="item" prepend-icon="mdi-check-circle-outline" color="success">
              {{ item }}
            </v-list-item>
          </v-list>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import B2bCodeBlock from '~/components/B2bCodeBlock.vue'
import B2bKpiCard from '~/components/B2bKpiCard.vue'
import B2bOrbitalDataVisual from '~/components/B2bOrbitalDataVisual.vue'
import B2bPageHeader from '~/components/B2bPageHeader.vue'
import B2bServiceCarousel from '~/components/B2bServiceCarousel.vue'
import { featuredServices } from '~/domains/b2b/services'

const { t, tm } = useI18n()

useLocalizedPageSeo({
  titleKey: 'home.seo.title',
  descriptionKey: 'home.seo.description'
})

const { indexedProductsFormatted } = useCatalogStats()
const serviceCount = featuredServices().length

const proofItems = computed(() => [
  {
    title: t('home.proof.data.title'),
    value: indexedProductsFormatted.value,
    caption: t('home.proof.data.caption'),
    icon: 'mdi-database-outline'
  },
  {
    title: t('home.proof.price.title'),
    value: t('home.proof.price.value'),
    caption: t('home.proof.price.caption'),
    icon: 'mdi-currency-eur'
  },
  {
    title: t('home.proof.services.title'),
    value: String(serviceCount),
    caption: t('home.proof.services.caption'),
    icon: 'mdi-puzzle-outline'
  },
  {
    title: t('home.proof.nodata.title'),
    value: t('home.proof.nodata.value'),
    caption: t('home.proof.nodata.caption'),
    icon: 'mdi-credit-card-off-outline'
  }
])

const carouselServices = featuredServices()
const billingRules = computed(() => tm('home.billing.rules') as string[])

const curlExample = `curl -H "Authorization: Bearer pdapi_..." \\
  "https://api.product-data-api.com/api/v1/products/0885909950805/price?language=en"`
</script>
