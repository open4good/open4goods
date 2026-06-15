<template>
  <v-card v-if="hasSites" class="manufacturing-chain" variant="tonal">
    <v-card-title class="text-subtitle-1 d-flex align-center">
      <v-icon icon="mdi-factory" class="mr-2" />
      Lieux de fabrication
    </v-card-title>

    <v-card-text>
      <v-list density="comfortable" class="bg-transparent pa-0">
        <v-list-item
          v-for="(site, index) in sites"
          :key="`${site.country}-${site.city ?? index}`"
          class="manufacturing-chain__site px-0"
        >
          <template #prepend>
            <v-icon :icon="siteIcon(site.type)" />
          </template>

          <v-list-item-title>
            {{ site.city ? `${site.city}, ` : ''
            }}{{ site.countryName ?? site.country }}
            <v-chip
              v-if="site.operator"
              size="x-small"
              variant="outlined"
              class="ml-2"
            >
              {{ site.operator }}
            </v-chip>
          </v-list-item-title>

          <v-list-item-subtitle>
            {{ siteTypeLabel(site.type) }}
          </v-list-item-subtitle>

          <!-- Distance slot is reserved at SSR time to avoid layout shift; it
               is filled after hydration once the client geoloc call returns. -->
          <template #append>
            <span class="manufacturing-chain__distance text-caption">
              <template v-if="distanceFor(site.country, site.city) != null">
                à&nbsp;~{{
                  formatKm(distanceFor(site.country, site.city))
                }}&nbsp;km de chez vous
              </template>
            </span>
          </template>
        </v-list-item>
      </v-list>

      <p
        v-if="hasOpenSupplyHub"
        class="text-caption text-medium-emphasis mt-2 mb-0"
      >
        Source des sites de production&nbsp;: Open Supply Hub (CC-BY-SA).
      </p>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { useManufacturingDistance } from '~/composables/useManufacturingDistance'

interface SourcedReference {
  url: string
  label?: string | null
  retrievedAt?: string | null
}

interface ManufacturingSite {
  categories?: string[]
  country: string
  countryName?: string | null
  city?: string | null
  latitude?: number | null
  longitude?: number | null
  type?: string | null
  operator?: string | null
  sources?: SourcedReference[]
}

interface BrandPayload {
  brandName: string
  companyName?: string | null
  company?: {
    manufacturing?: ManufacturingSite[]
  } | null
}

const props = defineProps({
  brandName: {
    type: String,
    required: true,
  },
  gtin: {
    type: Number,
    required: true,
  },
  verticalId: {
    type: String,
    default: undefined,
  },
})

// SSR fetch of the static (cacheable) manufacturing chain.
const { data: brand } = await useFetch<BrandPayload>(
  () => `/api/brands/${encodeURIComponent(props.brandName)}`,
  {
    query: computed(() =>
      props.verticalId ? { verticalId: props.verticalId } : {}
    ),
    key: computed(
      () => `brand-chain-${props.brandName}-${props.verticalId ?? 'all'}`
    ),
  }
)

const sites = computed<ManufacturingSite[]>(
  () => brand.value?.company?.manufacturing ?? []
)
const hasSites = computed(() => sites.value.length > 0)
const hasOpenSupplyHub = computed(() =>
  sites.value.some(site =>
    (site.sources ?? []).some(source => source.label === 'Open Supply Hub')
  )
)

// Client-only distance hydration.
const { distanceFor } = useManufacturingDistance(() => props.gtin)

function siteIcon(type?: string | null): string {
  switch (type) {
    case 'assembly':
      return 'mdi-wrench'
    case 'hq':
      return 'mdi-office-building'
    default:
      return 'mdi-factory'
  }
}

function siteTypeLabel(type?: string | null): string {
  switch (type) {
    case 'assembly':
      return 'Assemblage'
    case 'hq':
      return 'Siège'
    default:
      return 'Usine'
  }
}

function formatKm(km: number | null): string {
  if (km == null) {
    return ''
  }
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(km)
}
</script>

<style scoped>
.manufacturing-chain__distance {
  min-width: 9rem;
  text-align: right;
  display: inline-block;
}
</style>
