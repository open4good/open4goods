<template>
  <section class="product-sources" :aria-labelledby="headingId">
    <ProductSectionHeader
      :title="t('product.sources.title')"
      :subtitle="t('product.sources.subtitle')"
      :heading-id="headingId"
    />

    <ul class="product-sources__list">
      <li
        v-for="source in displayedSources"
        :key="source.name"
        class="product-sources__item"
        data-testid="product-source"
      >
        <a
          v-if="source.portalUrl"
          :href="source.portalUrl"
          target="_blank"
          rel="nofollow noopener noreferrer"
          class="product-sources__name"
        >
          {{ source.name }}
        </a>
        <span v-else class="product-sources__name">{{ source.name }}</span>

        <p v-if="source.attribution" class="product-sources__attribution">
          {{ source.attribution }}
        </p>

        <p v-if="source.license" class="product-sources__license">
          <a
            v-if="source.licenseUrl"
            :href="source.licenseUrl"
            target="_blank"
            rel="nofollow noopener noreferrer"
          >
            {{ t('product.sources.license', { license: source.license }) }}
          </a>
          <template v-else>
            {{ t('product.sources.license', { license: source.license }) }}
          </template>
        </p>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ProductSectionHeader from '~/components/product/ProductSectionHeader.vue'
import type { ProductSourceDto } from '~~/shared/api-client'

type DisplayedSource = {
  name: string
  portalUrl: string | null
  license: string | null
  licenseUrl: string | null
  attribution: string | null
}

const props = withDefaults(
  defineProps<{
    /** Sources of the product (`datasources.sources`), already sorted. */
    sources?: readonly ProductSourceDto[] | null
    headingId?: string
  }>(),
  {
    sources: () => [],
    headingId: 'product-sources-title',
  }
)

const { t } = useI18n()

const text = (value: string | null | undefined) => {
  const trimmed = typeof value === 'string' ? value.trim() : ''
  return trimmed.length ? trimmed : null
}

// A source that states no licence shows its name only: nothing is guessed.
const displayedSources = computed<DisplayedSource[]>(() =>
  (props.sources ?? []).flatMap(source => {
    const name = text(source?.name)
    if (!name) {
      return []
    }
    return [
      {
        name,
        portalUrl: text(source.portalUrl),
        license: text(source.license),
        licenseUrl: text(source.licenseUrl),
        attribution: text(source.attribution),
      },
    ]
  })
)
</script>

<style scoped>
.product-sources {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-sources__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 16rem), 1fr));
  gap: 0.75rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.product-sources__item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: rgba(var(--v-theme-surface-primary-050), 0.9);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
  overflow-wrap: anywhere;
}

.product-sources__name {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

a.product-sources__name {
  color: rgb(var(--v-theme-primary));
}

.product-sources__attribution,
.product-sources__license {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-sources__license a {
  color: inherit;
}
</style>
