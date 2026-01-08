<template>
  <svg
    viewBox="0 0 160 120"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="nudge-condition-icon"
  >
    <!-- Background glow -->
    <path
      d="M80 110C118.66 110 150 87.6142 150 60C150 32.3858 118.66 10 80 10C41.3401 10 10 32.3858 10 60C10 87.6142 41.3401 110 80 110Z"
      fill="currentColor"
      fill-opacity="0.05"
    />

    <!-- Circular Arrows (Recycle Metaphor) -->
    <path
      d="M80 35 C 60 35 45 50 45 70"
      stroke="currentColor"
      stroke-width="5"
      stroke-linecap="round"
      fill="none"
    />
    <path
      d="M40 65 L 45 70 L 55 62"
      stroke="currentColor"
      stroke-width="5"
      stroke-linecap="round"
      stroke-linejoin="round"
      fill="none"
    />

    <path
      d="M80 85 C 100 85 115 70 115 50"
      stroke="currentColor"
      stroke-width="5"
      stroke-linecap="round"
      fill="none"
    />
    <path
      d="M120 55 L 115 50 L 105 58"
      stroke="currentColor"
      stroke-width="5"
      stroke-linecap="round"
      stroke-linejoin="round"
      fill="none"
    />

    <!-- Center Icon/Dot -->
    <circle cx="80" cy="60" r="8" fill="currentColor" opacity="0.8" />
  </svg>
</template>

<style scoped>
.nudge-condition-icon {
  width: 100%;
  height: auto;
  color: rgb(var(--v-theme-brand-primary-alt));
  max-width: 100%;
  max-height: 100%;
}
</style>
