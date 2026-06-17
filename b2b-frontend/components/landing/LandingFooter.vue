<template>
  <v-footer color="surface-variant" class="pa-0">
    <v-container class="py-8">
      <v-row class="mb-4">
        <v-col cols="12" md="4" class="pr-md-8">
          <div class="font-weight-bold mb-3 text-h6">{{ t('app.name') }}</div>
          <p class="text-body-2 text-medium-emphasis">{{ t('landing.footer.baseline') }}</p>
        </v-col>
        <v-col cols="6" md="2">
          <div class="font-weight-bold text-caption text-medium-emphasis mb-3 text-uppercase">{{ t('landing.footer.product') }}</div>
          <v-list class="bg-transparent pa-0" density="compact">
            <v-list-item class="px-0" to="/pricing" variant="plain" :ripple="false"><span class="text-caption">{{ t('landing.nav.pricing') }}</span></v-list-item>
            <v-list-item class="px-0" to="/docs/products/price/playground" variant="plain" :ripple="false"><span class="text-caption">{{ t('nav.playground') }}</span></v-list-item>
            <v-list-item class="px-0" to="/docs" variant="plain" :ripple="false"><span class="text-caption">{{ t('landing.footer.docs') }}</span></v-list-item>
          </v-list>
        </v-col>
        <v-col cols="6" md="2">
          <div class="font-weight-bold text-caption text-medium-emphasis mb-3 text-uppercase">{{ t('landing.footer.resources') }}</div>
          <v-list class="bg-transparent pa-0" density="compact">
            <v-list-item class="px-0" to="/docs/getting-started" variant="plain" :ripple="false"><span class="text-caption">Getting started</span></v-list-item>
            <v-list-item class="px-0" to="/docs/api-reference" variant="plain" :ripple="false"><span class="text-caption">API reference</span></v-list-item>
            <v-list-item class="px-0" to="/contact" variant="plain" :ripple="false"><span class="text-caption">{{ t('landing.nav.contact') }}</span></v-list-item>
          </v-list>
        </v-col>
        <v-col cols="6" md="2">
          <div class="font-weight-bold text-caption text-medium-emphasis mb-3 text-uppercase">{{ t('landing.footer.legal') }}</div>
          <v-list class="bg-transparent pa-0" density="compact">
            <v-list-item class="px-0" to="/legal" variant="plain" :ripple="false"><span class="text-caption">{{ t('public.legal.title') }}</span></v-list-item>
            <v-list-item class="px-0" to="/privacy" variant="plain" :ripple="false"><span class="text-caption">{{ t('public.privacy.title') }}</span></v-list-item>
            <v-list-item class="px-0" to="/terms" variant="plain" :ripple="false"><span class="text-caption">{{ t('public.terms.title') }}</span></v-list-item>
          </v-list>
        </v-col>
      </v-row>
      <v-divider class="mb-4" />
      <div class="d-flex justify-space-between align-center flex-wrap ga-2">
        <span class="text-caption text-medium-emphasis">{{ t('landing.footer.signature') }}</span>
        <div class="d-flex ga-4">
          <NuxtLink to="/faq" class="text-decoration-none text-caption text-medium-emphasis">FAQ</NuxtLink>
          <NuxtLink to="/contact" class="text-decoration-none text-caption text-medium-emphasis">Contact</NuxtLink>
        </div>
      </div>
    </v-container>
  </v-footer>
</template>

<script setup lang="ts">
const { t } = useI18n()
</script>
