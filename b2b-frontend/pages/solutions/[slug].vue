<template>
  <v-container v-if="service" class="py-8">
    <div class="mb-2">
      <v-btn variant="text" prepend-icon="mdi-arrow-left" to="/solutions" size="small">
        {{ t('solutions.slug.backToAll') }}
      </v-btn>
    </div>

    <v-row align="start" class="mt-2">
      <v-col cols="12" md="7">
        <div class="d-flex align-center ga-3 mb-4">
          <v-avatar :color="service.status === 'live' ? 'primary' : 'secondary'" variant="tonal" rounded="lg" size="56">
            <v-icon :icon="service.icon" size="28" />
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold">{{ t(`services.${service.slug}.name`) }}</h1>
            <div class="d-flex ga-2 mt-1">
              <v-chip
                v-if="service.status === 'live'"
                color="success"
                size="small"
                variant="tonal"
                prepend-icon="mdi-check-circle-outline"
              >
                {{ t('services.badge.live') }}
              </v-chip>
              <v-chip
                v-else
                color="secondary"
                size="small"
                variant="tonal"
                prepend-icon="mdi-clock-outline"
              >
                {{ t('services.badge.comingSoon') }}
              </v-chip>
              <v-chip v-if="service.credits > 0" size="small" variant="outlined">
                {{ service.credits }} {{ t('services.badge.credits') }}
              </v-chip>
              <v-chip v-else size="small" variant="outlined">{{ t('services.badge.free') }}</v-chip>
            </div>
          </div>
        </div>

        <p class="text-body-1 mb-6">{{ t(`services.${service.slug}.description`) }}</p>

        <div class="d-flex flex-wrap ga-3 mb-8">
          <template v-if="service.status === 'live'">
            <v-btn
              v-if="service.docSlug"
              color="primary"
              prepend-icon="mdi-book-open-outline"
              :to="`/docs/${service.docSlug}`"
            >
              {{ t('solutions.slug.ctaDocs') }}
            </v-btn>
            <v-btn
              v-if="service.playgroundPath"
              variant="tonal"
              color="primary"
              prepend-icon="mdi-console-line"
              :to="service.playgroundPath"
            >
              {{ t('solutions.slug.ctaPlayground') }}
            </v-btn>
          </template>
          <template v-else>
            <v-btn color="primary" prepend-icon="mdi-email-outline" to="/contact">
              {{ t('solutions.slug.ctaNotify') }}
            </v-btn>
            <v-btn variant="tonal" prepend-icon="mdi-phone-outline" to="/contact">
              {{ t('solutions.slug.ctaContact') }}
            </v-btn>
          </template>
        </div>
      </v-col>

      <v-col cols="12" md="5">
        <v-card variant="outlined" class="pa-4">
          <p class="text-overline text-medium-emphasis mb-3">{{ t('solutions.slug.details') }}</p>
          <v-list density="compact" class="bg-transparent pa-0">
            <v-list-item prepend-icon="mdi-shape-outline" :title="t('solutions.slug.category')" :subtitle="t(`solutions.slug.categoryValue.${service.category}`)" />
            <v-list-item prepend-icon="mdi-identifier" :title="t('solutions.slug.facetId')" :subtitle="service.id" />
            <v-list-item
              prepend-icon="mdi-credit-card-outline"
              :title="t('solutions.slug.cost')"
              :subtitle="service.credits > 0 ? `${service.credits} ${t('services.badge.credits')} / ${t('solutions.slug.call')}` : t('services.badge.free')"
            />
          </v-list>
        </v-card>
      </v-col>
    </v-row>

    <v-divider class="my-6" />

    <div class="mt-6">
      <h2 class="text-h6 font-weight-bold mb-4">{{ t('solutions.slug.otherServices') }}</h2>
      <B2bServiceCarousel :services="otherServices" />
    </div>
  </v-container>

  <v-container v-else class="py-16 text-center">
    <v-icon icon="mdi-help-circle-outline" size="64" color="medium-emphasis" class="mb-4" />
    <h1 class="text-h5 font-weight-bold mb-2">{{ t('solutions.slug.notFound') }}</h1>
    <v-btn color="primary" to="/solutions" class="mt-4">{{ t('solutions.slug.backToAll') }}</v-btn>
  </v-container>
</template>

<script setup lang="ts">
import { getServiceBySlug, allServices } from '~/domains/b2b/services'

const { t } = useI18n()
const route = useRoute()
const slug = computed(() => route.params.slug as string)
const service = computed(() => getServiceBySlug(slug.value))

useLocalizedPageSeo({
  titleKey: service.value ? `services.${service.value.slug}.name` : 'solutions.slug.notFoundSeo',
  descriptionKey: service.value ? `services.${service.value.slug}.description` : 'solutions.slug.notFoundSeo'
})

const otherServices = computed(() =>
  allServices().filter(s => s.slug !== slug.value && s.featured).slice(0, 6)
)
</script>
