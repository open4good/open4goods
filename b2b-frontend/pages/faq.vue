<template>
  <div>
    <B2bPageHeader :title="t('public.faq.title')" :subtitle="t('public.faq.subtitle')" />
    <v-expansion-panels variant="accordion">
      <v-expansion-panel v-for="item in items" :key="item.question" :title="item.question" :text="item.answer" />
    </v-expansion-panels>
  </div>
</template>

<script setup lang="ts">
import B2bPageHeader from '~/components/B2bPageHeader.vue'

const { t, tm } = useI18n()

useLocalizedPageSeo({
  titleKey: 'public.faq.seo.title',
  descriptionKey: 'public.faq.seo.description'
})

const items = computed(() => tm('public.faq.items') as Array<{ question: string; answer: string }>)
</script>
