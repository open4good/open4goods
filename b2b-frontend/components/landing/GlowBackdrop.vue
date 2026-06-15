<template>
  <div class="glow-backdrop" aria-hidden="true" />
</template>

<style scoped lang="scss">
.glow-backdrop {
  position: absolute;
  inset: -20% 0 auto;
  height: 480px;
  background: radial-gradient(circle at center, var(--inf-token-color-accent-glow), transparent 65%);
  pointer-events: none;
  filter: blur(20px);
}
</style>
