<template>
  <div>
    <B2bPageHeader :title="t('public.legal.title')" :subtitle="t('public.legal.subtitle')" />
    <v-list class="bg-transparent">
      <v-list-item to="/privacy" prepend-icon="mdi-shield-account-outline" :title="t('public.privacy.title')" />
      <v-list-item to="/terms" prepend-icon="mdi-file-document-outline" :title="t('public.terms.title')" />
    </v-list>
  </div>
</template>

<script setup lang="ts">
import B2bPageHeader from '~/components/B2bPageHeader.vue'

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'public.legal.seo.title',
  descriptionKey: 'public.legal.seo.description'
})
</script>
