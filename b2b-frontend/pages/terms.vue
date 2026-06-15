<template>
  <div>
    <B2bPageHeader :title="t('public.terms.title')" :subtitle="t('public.terms.subtitle')" />
    <v-card variant="flat" class="b2b-public-card">
      <v-card-text class="pa-5">
        <p v-for="paragraph in paragraphs" :key="paragraph" class="text-body-2 text-medium-emphasis">
          {{ paragraph }}
        </p>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import B2bPageHeader from '~/components/B2bPageHeader.vue'

const { t, tm } = useI18n()

useLocalizedPageSeo({
  titleKey: 'public.terms.seo.title',
  descriptionKey: 'public.terms.seo.description'
})

const paragraphs = computed(() => tm('public.terms.paragraphs') as string[])
</script>

<style scoped>
.b2b-public-card {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
</style>
