<template>
  <div class="hero-action-card" :class="[`hero-action-card--${variant}`]">
    <div class="hero-action-card__background" aria-hidden="true" />
    <div class="hero-action-card__content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'default' | 'accent' | 'warning'
  }>(),
  {
    variant: 'default',
  }
)
</script>

<style scoped>
.hero-action-card {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  /* Fallback background if SVG fails or in contrast modes */
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-primary), 0.05),
    rgba(var(--v-theme-secondary), 0.1)
  );
  /* Modern projection shadow */
  box-shadow:
    0 10px 30px -10px rgba(var(--v-theme-primary), 0.15),
    0 4px 6px -4px rgba(0, 0, 0, 0.05);
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
  isolation: isolate;
}

.hero-action-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 20px 40px -12px rgba(var(--v-theme-primary), 0.25),
    0 8px 10px -6px rgba(0, 0, 0, 0.05);
}

.hero-action-card__background {
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image: url('/backgrounds/impact-score.svg');
  background-size: cover;
  background-position: center;
  opacity: 1;
  /* Use filter/mix-blend for better integration with dark/light modes if needed */
}

/* Variant adjustments if we want to shift hues */
.hero-action-card--accent .hero-action-card__background {
  filter: hue-rotate(45deg);
}

.hero-action-card--warning .hero-action-card__background {
  filter: hue-rotate(180deg);
}

.hero-action-card__content {
  position: relative;
  z-index: 1;
  padding: 1.5rem;
  /* Ensure text is readable over the SVG */
  color: rgb(
    var(--v-theme-surface-light)
  ); /* Provided SVG seems to have darkish/rich colors, so light text might be better, or we force a specific theme */
}

/* Adjust text color based on the SVG brightness. 
   The provided SVG has gradients. Let's assume we need to ensure contrast.
   If the SVG is very light, we might need dark text.
   Let's check the SVG content again. It has whites and blues.
*/
.hero-action-card__content {
  color: #ffffff; /* Forcing white text as the SVG has strong colors */
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Dark mode overrides if needed */
@media (prefers-color-scheme: dark) {
  .hero-action-card {
    box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.5);
  }
}
</style>
