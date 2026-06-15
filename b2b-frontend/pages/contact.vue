<template>
  <div>
    <B2bPageHeader :title="t('public.contact.title')" :subtitle="t('public.contact.subtitle')" />
    <v-row>
      <v-col cols="12" md="6">
        <v-card variant="flat" class="b2b-public-card">
          <v-card-text class="pa-5">
            <h2 class="text-h6 font-weight-bold mb-2">{{ t('public.contact.supportTitle') }}</h2>
            <p class="text-body-2 text-medium-emphasis mb-4">{{ t('public.contact.supportCopy') }}</p>
            <v-btn color="primary" href="mailto:support@product-data-api.com" prepend-icon="mdi-email-outline">
              support@product-data-api.com
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="6">
        <v-card variant="flat" class="b2b-public-card">
          <v-card-text class="pa-5">
            <h2 class="text-h6 font-weight-bold mb-2">{{ t('public.contact.salesTitle') }}</h2>
            <p class="text-body-2 text-medium-emphasis mb-4">{{ t('public.contact.salesCopy') }}</p>
            <v-btn variant="tonal" href="mailto:sales@product-data-api.com" prepend-icon="mdi-handshake-outline">
              sales@product-data-api.com
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import B2bPageHeader from '~/components/B2bPageHeader.vue'

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'public.contact.seo.title',
  descriptionKey: 'public.contact.seo.description'
})
</script>

<style scoped>
.b2b-public-card {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
</style>
