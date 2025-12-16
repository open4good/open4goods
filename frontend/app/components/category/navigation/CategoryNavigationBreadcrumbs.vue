<template>
  <nav
    v-if="breadcrumbItems.length"
    class="category-navigation-breadcrumbs"
    :class="`category-navigation-breadcrumbs--${variant}`"
    :aria-label="ariaLabel"
    itemscope
    itemtype="https://schema.org/BreadcrumbList"
  >
    <ol class="category-navigation-breadcrumbs__list">
      <li
        v-for="(item, index) in breadcrumbItems"
        :key="`${item.title}-${index}`"
        class="category-navigation-breadcrumbs__item"
        itemprop="itemListElement"
        itemscope
        itemtype="https://schema.org/ListItem"
      >
        <NuxtLink
          v-if="item.type === 'internal'"
          :to="item.to"
          class="category-navigation-breadcrumbs__link category-navigation-breadcrumbs__link--interactive"
          itemprop="item"
        >
          <span class="category-navigation-breadcrumbs__label" itemprop="name">{{ item.title }}</span>
        </NuxtLink>

        <a
          v-else-if="item.type === 'external'"
          :href="item.href"
          rel="noopener noreferrer"
          class="category-navigation-breadcrumbs__link category-navigation-breadcrumbs__link--interactive"
          itemprop="item"
        >
          <span class="category-navigation-breadcrumbs__label" itemprop="name">{{ item.title }}</span>
        </a>

        <span
          v-else
          class="category-navigation-breadcrumbs__link category-navigation-breadcrumbs__link--current"
          aria-current="page"
        >
          <span class="category-navigation-breadcrumbs__label" itemprop="name">{{ item.title }}</span>
        </span>

        <span
          v-if="variant === 'inline' && index < breadcrumbItems.length - 1"
          class="category-navigation-breadcrumbs__divider"
          aria-hidden="true"
        >
          {{ separator }}
        </span>

        <meta itemprop="position" :content="String(index + 1)" />
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface BreadcrumbItem {
  title: string
  link?: string
}

type BreadcrumbRenderItem =
  | { title: string; type: 'current' }
  | { title: string; type: 'external'; href: string }
  | { title: string; type: 'internal'; to: string }

const props = withDefaults(
  defineProps<{
    items: BreadcrumbItem[]
    ariaLabel: string
    variant?: 'inline' | 'pills'
    separator?: string
  }>(),
  {
    variant: 'inline',
    separator: '/',
  },
)

const visibleItems = computed(() => props.items.filter((item) => item.title?.trim().length))

const breadcrumbItems = computed<BreadcrumbRenderItem[]>(() => {
  const items = visibleItems.value

  return items.map((item, index) => {
    const rawLink = item.link?.trim()
    const isLast = index === items.length - 1

    if (!rawLink || isLast) {
      return { title: item.title, type: 'current' }
    }

    if (/^https?:\/\//iu.test(rawLink)) {
      return { title: item.title, type: 'external', href: rawLink }
    }

    const normalized = rawLink.startsWith('/') || rawLink.startsWith('#') ? rawLink : `/${rawLink}`
    return { title: item.title, type: 'internal', to: normalized }
  })
})
</script>

<style scoped>
.category-navigation-breadcrumbs {
  display: inline-flex;
  justify-content: center;
  width: 100%;
  font-size: 0.875rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.category-navigation-breadcrumbs__list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0;
  margin: 0;
  list-style: none;
}

.category-navigation-breadcrumbs__item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.category-navigation-breadcrumbs__link {
  display: inline-flex;
  align-items: center;
  color: inherit;
  text-decoration: none;
  transition: color 0.2s ease;
}

.category-navigation-breadcrumbs__link--interactive {
  cursor: pointer;
}

.category-navigation-breadcrumbs__link--current {
  cursor: default;
  pointer-events: none;
}

.category-navigation-breadcrumbs__link--interactive:hover,
.category-navigation-breadcrumbs__link--interactive:focus-visible {
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
}

.category-navigation-breadcrumbs__link:focus-visible {
  outline: none;
}

.category-navigation-breadcrumbs__label {
  font-weight: 500;
}

.category-navigation-breadcrumbs__item:last-child .category-navigation-breadcrumbs__label {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.category-navigation-breadcrumbs__divider {
  opacity: 0.6;
  color: inherit;
}

.category-navigation-breadcrumbs--pills {
  width: 100%;
  justify-content: flex-start;
}

.category-navigation-breadcrumbs--pills .category-navigation-breadcrumbs__list {
  gap: 0.6rem;
}

.category-navigation-breadcrumbs--pills .category-navigation-breadcrumbs__divider {
  display: none;
}

.category-navigation-breadcrumbs--pills .category-navigation-breadcrumbs__link {
  padding: 0.35rem 0.9rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-hero-overlay-soft), 0.18);
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-hero-pill-on-dark), 1);
}

.category-navigation-breadcrumbs--pills .category-navigation-breadcrumbs__link--interactive:hover,
.category-navigation-breadcrumbs--pills .category-navigation-breadcrumbs__link--interactive:focus-visible {
  background: rgba(var(--v-theme-hero-overlay-soft), 0.3);
  color: rgba(var(--v-theme-hero-pill-on-dark), 1);
  text-decoration: none;
}

.category-navigation-breadcrumbs--pills .category-navigation-breadcrumbs__link--current {
  background: rgba(var(--v-theme-hero-overlay-soft), 0.28);
  opacity: 0.95;
}
</style>
