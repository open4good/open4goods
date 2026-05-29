<template>
  <div
    v-if="visibleChips.length"
    class="subcategory-chips"
    :aria-label="t('categories.navigation.verticals.subcategoriesAriaLabel')"
  >
    <v-chip
      v-for="chip in visibleChips"
      :key="chip.id"
      :to="chip.to"
      size="x-small"
      variant="tonal"
      color="primary"
      class="subcategory-chips__chip"
      @click.stop
    >
      {{ chip.label }}
    </v-chip>
    <v-chip
      v-if="overflowCount > 0"
      size="x-small"
      variant="outlined"
      color="primary"
      class="subcategory-chips__chip subcategory-chips__chip--overflow"
    >
      +{{ overflowCount }}
    </v-chip>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { VerticalSubCategoryDto } from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{
    subcategories?: VerticalSubCategoryDto[] | null
    parentUrl?: string | null
    max?: number
  }>(),
  {
    subcategories: () => [],
    parentUrl: null,
    max: 3,
  }
)

const { t } = useI18n()

const joinUrl = (base: string, slug: string) => {
  const normalizedBase = base.replace(/\/+$/, '')
  const normalizedSlug = slug.replace(/^\/+/, '')
  return `${normalizedBase}/${normalizedSlug}`
}

const allChips = computed(() => {
  const parentUrl = props.parentUrl?.trim()
  if (!parentUrl) return []

  return (props.subcategories ?? [])
    .filter(sub => Boolean(sub.slug?.trim()))
    .map(sub => ({
      id: sub.id ?? sub.slug ?? '',
      label: sub.h1Title?.trim() || sub.slug || '',
      to: joinUrl(parentUrl, sub.slug ?? ''),
    }))
})

const visibleChips = computed(() => allChips.value.slice(0, props.max))
const overflowCount = computed(() =>
  Math.max(0, allChips.value.length - props.max)
)
</script>

<style scoped>
.subcategory-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin-top: 0.5rem;
}

.subcategory-chips__chip {
  transition: transform 0.15s ease;
  text-decoration: none;
}

.subcategory-chips__chip:hover {
  transform: translateY(-1px);
}

.subcategory-chips__chip--overflow {
  opacity: 0.7;
}

@media (prefers-reduced-motion: reduce) {
  .subcategory-chips__chip {
    transition: none;
  }

  .subcategory-chips__chip:hover {
    transform: none;
  }
}
</style>
