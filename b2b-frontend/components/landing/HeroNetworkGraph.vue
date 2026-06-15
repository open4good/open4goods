<template>
  <div class="graph-wrap">
    <svg viewBox="0 0 880 220" class="graph" role="img" :aria-label="t('landing.hero.graphLabel')">
      <title>{{ t('landing.hero.graphLabel') }}</title>
      <line x1="120" y1="110" x2="440" y2="55" class="link" />
      <line x1="120" y1="110" x2="440" y2="110" class="link" />
      <line x1="120" y1="110" x2="440" y2="165" class="link" />
      <line x1="440" y1="55" x2="760" y2="65" class="link" />
      <line x1="440" y1="110" x2="760" y2="110" class="link" />
      <line x1="440" y1="165" x2="760" y2="155" class="link" />

      <circle cx="120" cy="110" r="6" class="node node--client" />
      <circle cx="440" cy="55" r="5" class="node" />
      <circle cx="440" cy="110" r="6" class="node node--router" />
      <circle cx="440" cy="165" r="5" class="node" />
      <circle cx="760" cy="65" r="4" class="node" />
      <circle cx="760" cy="110" r="4" class="node" />
      <circle cx="760" cy="155" r="4" class="node" />

      <text x="90" y="140" class="label">{{ t('landing.hero.client') }}</text>
      <text x="410" y="195" class="label">{{ t('landing.hero.router') }}</text>
      <text x="730" y="185" class="label">{{ t('landing.hero.gpuNodes') }}</text>
    </svg>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()
</script>

<style scoped lang="scss">
.graph-wrap {
  width: min(100%, 920px);
  margin: 24px auto 0;
}

.graph {
  width: 100%;
}

.link {
  stroke: var(--inf-token-color-line-subtle);
  stroke-width: 1;
}

.node {
  fill: var(--inf-token-color-accent-primary-soft);
  animation: pulse 3s infinite ease-in-out;

  &--router {
    fill: var(--inf-token-color-accent-primary);
  }

  &--client {
    animation-delay: 0.6s;
  }
}

.label {
  fill: var(--inf-token-color-text-secondary);
  font-size: 12px;
}

@keyframes pulse {
  0%,
  100% { opacity: 0.45; }
  50% { opacity: 1; }
}
</style>
