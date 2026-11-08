<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type {
  OpenDataLinkPage,
  OpenDataLinkTarget,
} from '~/composables/useAnalytics'

const props = defineProps<{
  page: OpenDataLinkPage
}>()

const { t } = useI18n()
const { trackOpenDataOutboundClick } = useAnalytics()

const mirrors: {
  id: Extract<OpenDataLinkTarget, 'datagouv' | 'zenodo' | 'kaggle' | 'figshare'>
  href: string
}[] = [
  {
    id: 'datagouv',
    href: 'https://www.data.gouv.fr/datasets/base-de-codes-barres-noms-et-categories-produits',
  },
  { id: 'zenodo', href: 'https://doi.org/10.5281/zenodo.14035436' },
  {
    id: 'kaggle',
    href: 'https://www.kaggle.com/datasets/goulvenfuret/gtin-ean-products-database',
  },
  {
    id: 'figshare',
    href: 'https://figshare.com/articles/dataset/Datasets_of_products_and_books_identified_by_ISBN_GTIN/27605688',
  },
]
</script>

<template>
  <section class="opendata-mirrors" aria-labelledby="opendata-mirrors-heading">
    <v-container max-width="lg">
      <h2 id="opendata-mirrors-heading" class="opendata-mirrors__title">
        {{ t('opendata.mirrors.title') }}
      </h2>
      <p class="opendata-mirrors__subtitle">
        {{ t('opendata.mirrors.referenceLicense') }}
      </p>

      <v-row>
        <v-col
          v-for="mirror in mirrors"
          :key="mirror.id"
          cols="12"
          sm="6"
          md="3"
        >
          <v-card
            class="opendata-mirrors__card"
            variant="outlined"
            rounded="xl"
          >
            <v-card-item>
              <h3 class="opendata-mirrors__name">
                <a
                  :href="mirror.href"
                  target="_blank"
                  rel="noopener"
                  class="opendata-mirrors__link"
                  :data-mirror="mirror.id"
                  :aria-label="
                    t('opendata.mirrors.linkAriaLabel', {
                      name: t(`opendata.mirrors.items.${mirror.id}.name`),
                    })
                  "
                  @click="
                    trackOpenDataOutboundClick({
                      block: 'mirrors',
                      target: mirror.id,
                      page: props.page,
                    })
                  "
                >
                  {{ t(`opendata.mirrors.items.${mirror.id}.name`) }}
                  <v-icon
                    icon="mdi-open-in-new"
                    size="small"
                    aria-hidden="true"
                  />
                </a>
              </h3>
            </v-card-item>
            <v-card-text class="opendata-mirrors__license">
              {{ t(`opendata.mirrors.items.${mirror.id}.license`) }}
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-mirrors
  padding: clamp(2rem, 5vw, 3.5rem) 0

.opendata-mirrors__title
  margin: 0 0 0.5rem
  font-size: clamp(1.75rem, 3vw, 2.2rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-mirrors__subtitle
  margin: 0 0 1.5rem
  color: rgba(var(--v-theme-text-neutral-secondary), 1)

.opendata-mirrors__card
  height: 100%

.opendata-mirrors__name
  margin: 0
  font-size: 1.15rem

.opendata-mirrors__link
  color: rgb(var(--v-theme-primary))
  font-weight: 700

.opendata-mirrors__license
  color: rgba(var(--v-theme-text-neutral-secondary), 1)
</style>
