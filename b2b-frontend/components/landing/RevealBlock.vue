<template>
  <div ref="target" class="reveal-block">
    <transition name="reveal" appear>
      <div v-show="isVisible" class="reveal-block__inner">
        <slot />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
const { prefersReducedMotion } = useReducedMotion()
const { target, isVisible } = useScrollReveal(prefersReducedMotion)
</script>

<style scoped lang="scss">
.reveal-block {
  width: 100%;
}

.reveal-block__inner {
  width: 100%;
}

.reveal-enter-active,
.reveal-leave-active {
  transition: opacity 320ms ease, transform 320ms ease;
}

.reveal-enter-from,
.reveal-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (prefers-reduced-motion: reduce) {
  .reveal-enter-active,
  .reveal-leave-active {
    transition: none;
  }

  .reveal-enter-from,
  .reveal-leave-to {
    opacity: 1;
    transform: none;
  }
}
</style>
