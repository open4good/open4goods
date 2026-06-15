<template>
  <section class="section">
    <LandingRevealBlock>
      <v-container>
        <h2>{{ t('landing.why.title') }}</h2>
        <v-row class="mt-6" density="comfortable">
          <v-col v-for="item in items" :key="item.key" cols="12" md="4">
            <v-card class="landing-card fill-height" variant="tonal" rounded="lg">
              <v-card-item>
                <template #prepend>
                  <v-icon :icon="item.icon" color="primary" size="x-large" class="mr-2" />
                </template>
                <v-card-title class="text-h6">{{ t(`landing.why.items.${item.key}.title`) }}</v-card-title>
              </v-card-item>
              <v-card-text class="text-secondary">
                {{ t(`landing.why.items.${item.key}.description`) }}
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const items = [
  { key: 'sovereignty', icon: 'mdi-earth' },
  { key: 'compatibility', icon: 'mdi-api' },
  { key: 'security', icon: 'mdi-shield-check' }
]
</script>
