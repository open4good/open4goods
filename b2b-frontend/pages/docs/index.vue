<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="10">
        <v-card class="mb-4" variant="outlined">
          <v-card-text>
            <p class="text-overline mb-1">{{ t('docs.badge') }}</p>
            <h1 class="text-h4 font-weight-bold mb-2">{{ t('docs.title') }}</h1>
            <p class="text-medium-emphasis mb-0">{{ t('docs.subtitle') }}</p>
          </v-card-text>
        </v-card>

        <InfContentExplorer section-prefix="docs/" scope="public" show-locale-selector />
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()

definePageMeta({
  width: 'semi-fluid'
})

useLocalizedPageSeo({
  titleKey: 'docs.seo.title',
  descriptionKey: 'docs.seo.description'
})
</script>
