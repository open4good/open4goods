<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="11">
        <B2bPageHeader
          :title="t('barcodes.check.playground.title')"
          :subtitle="t('barcodes.check.playground.subtitle')"
        />
        <B2bBarcodeCheckPlayground />
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ width: 'fluid' })

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'barcodes.check.playground.seo.title',
  descriptionKey: 'barcodes.check.playground.seo.description'
})
</script>
