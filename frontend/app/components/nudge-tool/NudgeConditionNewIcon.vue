<template>
  <svg
    viewBox="0 0 160 120"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="nudge-condition-icon"
  >
    <!-- Background glow/shape -->
    <path
      d="M80 110C118.66 110 150 87.6142 150 60C150 32.3858 118.66 10 80 10C41.3401 10 10 32.3858 10 60C10 87.6142 41.3401 110 80 110Z"
      fill="currentColor"
      fill-opacity="0.05"
    />

    <!-- Box shape -->
    <rect
      x="50"
      y="40"
      width="60"
      height="50"
      rx="4"
      stroke="currentColor"
      stroke-width="3"
      fill="none"
    />
    <!-- Box lid/flaps -->
    <path
      d="M50 40 L80 25 L110 40"
      stroke="currentColor"
      stroke-width="3"
      stroke-linejoin="round"
      fill="none"
    />
    <path
      d="M80 40 L80 90"
      stroke="currentColor"
      stroke-width="2"
      stroke-dasharray="4 4"
      opacity="0.5"
    />

    <!-- Sparkles -->
    <path
      d="M125 30 L128 38 L136 39 L129 44 L130 52 L123 47 L116 52 L117 44 L110 39 L118 38 Z"
      fill="currentColor"
      opacity="0.8"
    />
    <path
      d="M35 80 L37 84 L41 84.5 L37.5 87 L38 91 L35 88.5 L32 91 L32.5 87 L29 84.5 L33 84 Z"
      fill="currentColor"
      opacity="0.6"
    />
    <circle cx="130" cy="80" r="3" fill="currentColor" opacity="0.4" />
  </svg>
</template>

<style scoped>
.nudge-condition-icon {
  width: 100%;
  height: auto;
  color: rgb(var(--v-theme-primary));
}
</style>
