<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="11">
        <B2bPageHeader
          :title="t('playground.title')"
          :subtitle="t('playground.subtitle')"
        />
        <B2bFacetPlayground :facet="FACET_PRICE" />
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { FACET_PRICE } from '~/domains/b2b/facets'

definePageMeta({ width: 'fluid' })

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'playground.seo.title',
  descriptionKey: 'playground.seo.description'
})
</script>
