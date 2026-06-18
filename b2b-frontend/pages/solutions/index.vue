<template>
  <v-container class="py-8">
    <B2bPageHeader
      :title="t('solutions.index.title')"
      :subtitle="t('solutions.index.subtitle')"
      :eyebrow="t('solutions.index.eyebrow')"
    />

    <section class="mt-8">
      <h2 class="text-h6 font-weight-bold mb-4">{{ t('solutions.index.sectionProduct') }}</h2>
      <v-row>
        <v-col
          v-for="service in productServices"
          :key="service.id"
          cols="12"
          sm="6"
          md="4"
          lg="3"
        >
          <B2bServiceCard :service="service" />
        </v-col>
      </v-row>
    </section>

    <section class="mt-10">
      <h2 class="text-h6 font-weight-bold mb-4">{{ t('solutions.index.sectionUtility') }}</h2>
      <v-row>
        <v-col
          v-for="service in utilityServices"
          :key="service.id"
          cols="12"
          sm="6"
          md="4"
          lg="3"
        >
          <B2bServiceCard :service="service" />
        </v-col>
      </v-row>
    </section>
  </v-container>
</template>

<script setup lang="ts">
import { allServices } from '~/domains/b2b/services'

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'solutions.index.seo.title',
  descriptionKey: 'solutions.index.seo.description'
})

const services = allServices()
const productServices = services.filter(s => s.category === 'product')
const utilityServices = services.filter(s => s.category === 'utility')
</script>
