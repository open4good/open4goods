<template>
  <header class="product-section-header">
    <component
      :is="tag"
      :id="headingId"
      class="product-section-header__title"
    >
      {{ title }}
    </component>
    <p v-if="subtitle" class="product-section-header__subtitle">
      {{ subtitle }}
    </p>
    <!-- eslint-disable vue/no-v-html -->
    <p
      v-else-if="subtitleHtml"
      class="product-section-header__subtitle"
      v-html="subtitleHtml"
    />
    <!-- eslint-enable vue/no-v-html -->
  </header>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    /** Pre-sanitized HTML subtitle, for the rare case (vigilance) that needs
     * rich text. Callers own sanitization; this only renders it. */
    subtitleHtml?: string
    headingId?: string
    tag?: 'h2' | 'h3'
  }>(),
  {
    subtitle: undefined,
    subtitleHtml: undefined,
    headingId: undefined,
    tag: 'h2',
  }
)
</script>

<style scoped>
.product-section-header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-section-header__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
  line-height: 1.1;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-section-header__subtitle {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  line-height: 1.5;
  max-width: 65ch;
}
</style>
