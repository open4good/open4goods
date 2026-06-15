<template>
  <div class="b2b-orbital-data-visual" aria-hidden="true">
    <div class="b2b-orbital-data-visual__core">
      <v-icon icon="mdi-barcode-scan" size="40" />
      <span>GTIN</span>
    </div>
    <div
      v-for="item in satellites"
      :key="item.label"
      class="b2b-orbital-data-visual__satellite"
      :style="{ '--x': item.x, '--y': item.y }"
    >
      <v-icon :icon="item.icon" size="20" />
      <span>{{ item.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const satellites = [
  { label: 'Price', icon: 'mdi-currency-eur', x: '12%', y: '18%' },
  { label: 'Freshness', icon: 'mdi-clock-check-outline', x: '68%', y: '14%' },
  { label: 'Provenance', icon: 'mdi-storefront-outline', x: '74%', y: '66%' },
  { label: 'Credits', icon: 'mdi-credit-card-outline', x: '18%', y: '72%' }
]
</script>

<style scoped>
.b2b-orbital-data-visual {
  position: relative;
  min-height: 320px;
  border: 1px solid rgba(var(--v-theme-primary), 0.16);
  border-radius: 8px;
  background:
    radial-gradient(circle at center, rgba(var(--v-theme-primary), 0.12), transparent 34%),
    rgb(var(--v-theme-surface));
}

.b2b-orbital-data-visual::before {
  position: absolute;
  inset: 14%;
  border: 1px solid rgba(var(--v-theme-primary), 0.22);
  border-radius: 999px;
  content: "";
}

.b2b-orbital-data-visual__core,
.b2b-orbital-data-visual__satellite {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  background: rgb(var(--v-theme-surface));
  color: rgb(var(--v-theme-primary));
}

.b2b-orbital-data-visual__core {
  inset: 50% auto auto 50%;
  width: 112px;
  height: 112px;
  border-radius: 999px;
  transform: translate(-50%, -50%);
  font-weight: 700;
}

.b2b-orbital-data-visual__satellite {
  left: var(--x);
  top: var(--y);
  width: 92px;
  min-height: 72px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 600;
}
</style>
