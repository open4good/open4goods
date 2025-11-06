<template>
  <v-breadcrumbs
    v-if="breadcrumbItems.length"
    class="category-navigation-breadcrumbs"
    :aria-label="ariaLabel"
    :items="breadcrumbItems"
    divider="/"
    tag="nav"
  >
    <template #item="{ item }">
      <v-breadcrumbs-item
        :disabled="item.disabled"
        :href="item.href"
        :to="item.to"
        class="category-navigation-breadcrumbs__item"
      >
        <span
          :class="[
            'category-navigation-breadcrumbs__label',
            item.disabled ? 'category-navigation-breadcrumbs__label--current' : 'category-navigation-breadcrumbs__label--link',
          ]"
        >
          {{ item.title }}
        </span>
      </v-breadcrumbs-item>
    </template>
  </v-breadcrumbs>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface BreadcrumbItem {
  title: string
  link?: string
}

const props = defineProps<{
  items: BreadcrumbItem[]
  ariaLabel: string
}>()

const visibleItems = computed(() => props.items.filter((item) => item.title?.trim().length))

const breadcrumbItems = computed(() => {
  const items = visibleItems.value

  return items.map((item, index) => {
    const rawLink = item.link?.trim()
    const isLast = index === items.length - 1

    if (!rawLink || isLast) {
      return {
        title: item.title,
        disabled: true,
      }
    }

    if (/^https?:\/\//i.test(rawLink)) {
      return {
        title: item.title,
        href: rawLink,
        disabled: false,
      }
    }

    return {
      title: item.title,
      to: rawLink,
      disabled: false,
    }
  })
})
</script>

<style scoped>
.category-navigation-breadcrumbs {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.category-navigation-breadcrumbs__item {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.category-navigation-breadcrumbs__label {
  transition: color 0.2s ease;
}

.category-navigation-breadcrumbs__label--link {
  color: inherit;
  text-decoration: none;
}

.category-navigation-breadcrumbs__label--link:hover,
.category-navigation-breadcrumbs__label--link:focus-visible {
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
}

.category-navigation-breadcrumbs__label--current {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.category-navigation-breadcrumbs :deep(.v-breadcrumbs-item__divider) {
  opacity: 0.6;
}
</style>
