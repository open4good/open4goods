<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="11">
        <B2bPageHeader
          :title="t('barcodes.playground.title')"
          :subtitle="t('barcodes.playground.subtitle')"
        />
        <B2bBarcodePlayground />
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ width: 'fluid' })

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'barcodes.playground.seo.title',
  descriptionKey: 'barcodes.playground.seo.description'
})
</script>
