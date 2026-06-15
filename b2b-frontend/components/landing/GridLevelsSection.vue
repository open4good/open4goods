<template>
  <section id="levels" class="section">
    <LandingRevealBlock>
      <v-container>
        <h2>{{ t('landing.levels.title') }}</h2>
        <v-row class="mt-6" align="stretch" density="comfortable">
          <v-col v-for="tier in tiers" :key="tier" cols="12" md="4">
            <v-card
              class="landing-card fill-height"
              :class="{ 'landing-card--highlight': tier === 'trusted' }"
              variant="outlined"
              :elevation="tier === 'trusted' ? 6 : 0"
            >
              <v-card-item>
                <template v-if="tier === 'trusted'" #append>
                  <v-chip color="primary" size="x-small" variant="flat">
                    {{ t('landing.levels.trusted.badge') }}
                  </v-chip>
                </template>
                <v-card-title>{{ t(`landing.levels.${tier}.title`) }}</v-card-title>
                <v-card-subtitle>{{ t(`landing.levels.${tier}.label`) }}</v-card-subtitle>
              </v-card-item>
              <v-card-text>
                <v-list density="compact" bg-color="transparent">
                  <v-list-item v-for="key in getPointKeys(tier)" :key="key">
                    <template #prepend>
                      <v-icon
                        icon="mdi-check"
                        size="small"
                        :color="tier === 'trusted' ? 'primary' : undefined"
                      />
                    </template>
                    <v-list-item-title>{{ t(`landing.levels.${tier}.points.${key}`) }}</v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
const { t, tm } = useI18n()
const tiers = ['public', 'trusted', 'hds']

function getPointKeys(tier: string): string[] {
  const points = tm(`landing.levels.${tier}.points`) as Record<string, unknown>
  return points && typeof points === 'object' ? Object.keys(points) : []
}
</script>

<style scoped lang="scss">
.landing-card--highlight {
  border-color: var(--inf-token-color-accent-primary) !important;
}
</style>
