<template>
  <nav
    v-if="items.length"
    class="category-navigation-breadcrumbs"
    :aria-label="ariaLabel"
  >
    <ol class="category-navigation-breadcrumbs__list">
      <li
        v-for="(item, index) in items"
        :key="`${item.title}-${index}`"
        class="category-navigation-breadcrumbs__item"
      >
        <span v-if="!item.link" class="category-navigation-breadcrumbs__current">
          {{ item.title }}
        </span>
        <NuxtLink
          v-else
          :to="item.link"
          class="category-navigation-breadcrumbs__link"
        >
          {{ item.title }}
        </NuxtLink>
        <span
          v-if="index < items.length - 1"
          aria-hidden="true"
          class="category-navigation-breadcrumbs__separator"
        >
          /
        </span>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
interface BreadcrumbItem {
  title: string
  link?: string
}

defineProps<{
  items: BreadcrumbItem[]
  ariaLabel: string
}>()
</script>

<style scoped>
.category-navigation-breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.category-navigation-breadcrumbs__list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-navigation-breadcrumbs__item {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.category-navigation-breadcrumbs__link {
  color: inherit;
  text-decoration: none;
  transition: color 0.2s ease;
}

.category-navigation-breadcrumbs__link:hover,
.category-navigation-breadcrumbs__link:focus-visible {
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
}

.category-navigation-breadcrumbs__separator {
  opacity: 0.6;
}

.category-navigation-breadcrumbs__current {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}
</style>
