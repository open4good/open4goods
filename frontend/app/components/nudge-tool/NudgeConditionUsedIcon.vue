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

    <!-- Recycle arrows forming a circle-ish shape -->
    <path
      d="M80 30 
         A 30 30 0 0 1 106 45
         L 100 51"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
      fill="none"
    />
    <path d="M110 42 L106 45 L112 49" fill="currentColor" stroke="none" />
    <!-- Arrowhead 1 -->

    <path
      d="M110 60
         A 30 30 0 0 1 80 90
         L 74 90"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
      fill="none"
    />
    <path d="M110 60 L104 66" opacity="0" />
    <!-- Spacer -->
    <path
      d="M80 90 L84 94 L80 97"
      fill="none"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <!-- Arrowhead 2 (simple line) -->

    <!-- Better Recycle Symbol (Simplified) -->
    <path
      d="M80 35 L55 80 L105 80 Z"
      stroke="currentColor"
      stroke-width="3"
      stroke-linejoin="round"
      fill="none"
      opacity="0.3"
    />

    <!-- Circular arrows "Reuse" style -->
    <path
      d="M60 55 C 60 45, 70 35, 80 35 C 90 35, 100 45, 100 55"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
      fill="none"
    />
    <path
      d="M100 65 C 100 75, 90 85, 80 85 C 70 85, 60 75, 60 65"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
      fill="none"
    />
    <path
      d="M100 55 L 96 59"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
    />
    <path
      d="M60 65 L 64 61"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
    />
  </svg>
</template>

<style scoped>
.nudge-condition-icon {
  width: 100%;
  height: auto;
  color: rgb(var(--v-theme-secondary));
}
</style>
