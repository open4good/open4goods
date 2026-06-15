<template>
  <footer class="landing-footer">
    <v-container>
      <v-row class="mb-4">
        <v-col cols="12" md="4" class="pr-md-8">
          <div class="font-weight-bold mb-4 text-h6 text-white">{{ t('app.name') }}</div>
          <p class="footer-copy text-body-2">{{ t('landing.footer.baseline') }}</p>
        </v-col>
        <v-col cols="6" md="2">
          <div class="footer-head mb-4 font-weight-bold">{{ t('landing.footer.product') }}</div>
          <v-list class="bg-transparent pa-0" density="compact">
            <v-list-item class="px-0 min-height-0" to="/pricing" variant="plain" :ripple="false"><span class="text-caption">{{ t('landing.nav.pricing') }}</span></v-list-item>
            <v-list-item class="px-0 min-height-0" to="/docs/products/price/playground" variant="plain" :ripple="false"><span class="text-caption">{{ t('nav.playground') }}</span></v-list-item>
            <v-list-item class="px-0 min-height-0" to="/docs" variant="plain" :ripple="false"><span class="text-caption">{{ t('landing.footer.docs') }}</span></v-list-item>
          </v-list>
        </v-col>
        <v-col cols="6" md="2">
          <div class="footer-head mb-4 font-weight-bold">{{ t('landing.footer.resources') }}</div>
          <v-list class="bg-transparent pa-0" density="compact">
            <v-list-item class="px-0 min-height-0" to="/docs/getting-started" variant="plain" :ripple="false"><span class="text-caption">Getting started</span></v-list-item>
            <v-list-item class="px-0 min-height-0" to="/docs/api-reference" variant="plain" :ripple="false"><span class="text-caption">API reference</span></v-list-item>
            <v-list-item class="px-0 min-height-0" to="/contact" variant="plain" :ripple="false"><span class="text-caption">{{ t('landing.nav.contact') }}</span></v-list-item>
          </v-list>
        </v-col>
        <v-col cols="6" md="2">
          <div class="footer-head mb-4 font-weight-bold">{{ t('landing.footer.legal') }}</div>
          <v-list class="bg-transparent pa-0" density="compact">
            <v-list-item class="px-0 min-height-0" to="/legal" variant="plain" :ripple="false"><span class="text-caption">{{ t('public.legal.title') }}</span></v-list-item>
            <v-list-item class="px-0 min-height-0" to="/privacy" variant="plain" :ripple="false"><span class="text-caption">{{ t('public.privacy.title') }}</span></v-list-item>
            <v-list-item class="px-0 min-height-0" to="/terms" variant="plain" :ripple="false"><span class="text-caption">{{ t('public.terms.title') }}</span></v-list-item>
          </v-list>
        </v-col>
      </v-row>
      <v-divider class="mb-4 mt-2" style="opacity: 0.1"/>
      <div class="footer-bottom">
        <span>{{ t('landing.footer.signature') }}</span>
        <span class="d-flex ga-4">
          <NuxtLink to="/faq" class="text-decoration-none text-medium-emphasis">FAQ</NuxtLink>
          <NuxtLink to="/contact" class="text-decoration-none text-medium-emphasis">Contact</NuxtLink>
        </span>
      </div>
    </v-container>
  </footer>
</template>

<script setup lang="ts">
const { t } = useI18n()
</script>

<style scoped lang="scss">
.landing-footer {
  border-top: 1px solid var(--inf-token-color-line-subtle);
  padding: 42px 0 24px;
}

.footer-head,
.footer-bottom,
.footer-copy {
  color: var(--inf-token-color-text-secondary);
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 12px;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 600px) {
  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
