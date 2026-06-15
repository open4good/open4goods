<template>
  <div>
    <section class="b2b-home-hero">
      <v-container>
        <v-row align="center" class="ga-8 ga-md-0">
          <v-col cols="12" md="6">
            <p class="text-overline text-primary mb-3">{{ t('home.hero.eyebrow') }}</p>
            <h1 class="text-h3 text-md-h2 font-weight-bold mb-4">{{ t('home.hero.title') }}</h1>
            <p class="text-body-1 text-medium-emphasis mb-6 b2b-home-hero__copy">
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
        <v-col v-for="proof in proofItems" :key="proof.title" cols="12" md="3">
          <B2bKpiCard :label="proof.title" :value="proof.value" :caption="proof.caption" :icon="proof.icon" />
        </v-col>
      </v-row>
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

const { t, tm } = useI18n()

useLocalizedPageSeo({
  titleKey: 'home.seo.title',
  descriptionKey: 'home.seo.description'
})

const proofItems = computed(() => [
  { title: t('home.proof.fresh.title'), value: t('home.proof.fresh.value'), caption: t('home.proof.fresh.caption'), icon: 'mdi-clock-check-outline' },
  { title: t('home.proof.nodata.title'), value: t('home.proof.nodata.value'), caption: t('home.proof.nodata.caption'), icon: 'mdi-credit-card-off-outline' },
  { title: t('home.proof.gtin.title'), value: t('home.proof.gtin.value'), caption: t('home.proof.gtin.caption'), icon: 'mdi-barcode-scan' },
  { title: t('home.proof.provenance.title'), value: t('home.proof.provenance.value'), caption: t('home.proof.provenance.caption'), icon: 'mdi-storefront-outline' }
])

const billingRules = computed(() => tm('home.billing.rules') as string[])

const curlExample = `curl -H "Authorization: Bearer pdapi_..." \\
  "https://api.product-data-api.com/api/v1/products/0885909950805/price?language=en"`
</script>

<style scoped>
.b2b-home-hero {
  padding-block: 72px 40px;
}

.b2b-home-hero__copy {
  max-width: 620px;
}
</style>
