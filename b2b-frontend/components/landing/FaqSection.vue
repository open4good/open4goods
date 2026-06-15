<template>
  <section id="faq" class="section">
    <LandingRevealBlock>
      <v-container>
        <h2>{{ t('landing.faq.title') }}</h2>
        <v-expansion-panels v-model="openedPanel" class="mt-6" variant="accordion">
          <v-expansion-panel v-for="key in faqItemKeys" :key="key" :elevation="0">
            <v-expansion-panel-title>
              {{ t(`landing.faq.items.${key}.q`) }}
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              {{ t(`landing.faq.items.${key}.a`) }}
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
const { t, tm } = useI18n()
const { openedPanel } = useFaqState()

const faqItemKeys = computed(() => {
  const items = tm('landing.faq.items') as Record<string, unknown>
  return items && typeof items === 'object' ? Object.keys(items) : []
})

const faqSchema = computed(() => JSON.stringify({
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: faqItemKeys.value.map(key => ({
    '@type': 'Question',
    name: t(`landing.faq.items.${key}.q`),
    acceptedAnswer: {
      '@type': 'Answer',
      text: t(`landing.faq.items.${key}.a`)
    }
  }))
}))

useHead({
  script: [{ type: 'application/ld+json', innerHTML: faqSchema }]
})
</script>
