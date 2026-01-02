<template>
  <div class="product-hero-background" aria-hidden="true">
    <div class="product-hero-background__gradient" />

    <transition name="fade">
      <div
        v-if="resolvedImage"
        class="product-hero-background__image"
        :class="motionClass"
        :style="imageStyle"
      />
      <div
        v-else-if="fallbackBackgroundSrc"
        class="product-hero-background__image product-hero-background__image--fallback"
        :class="motionClass"
        :style="fallbackStyle"
      />
    </transition>

    <div class="product-hero-background__grid" />
    <div class="product-hero-background__overlay" />
    <div
      class="product-hero-background__glow product-hero-background__glow--primary"
    />
    <div
      class="product-hero-background__glow product-hero-background__glow--accent"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type PropType } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { useHeroBackgroundAsset } from '~/composables/useThemedAsset'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const heroBackgroundAsset = useHeroBackgroundAsset()
const fallbackBackgroundSrc = computed(() => heroBackgroundAsset.value?.trim())

const fallbackStyle = computed(() => {
  if (!fallbackBackgroundSrc.value) return {}
  return {
    backgroundImage: `url('${fallbackBackgroundSrc.value}')`,
  }
})

const resolvedImage = computed(() => {
  const p = props.product
  if (!p) return null

  // Priority 1: Main Cover Path matches
  if (p.resources?.coverImagePath?.trim()) {
    return p.resources.coverImagePath.trim()
  }

  // Priority 2: External Cover
  if (p.resources?.externalCover?.trim()) {
    return p.resources.externalCover.trim()
  }

  // Priority 3: First valid Gallery Image (check size if possible, but for now existence)
  if (p.resources?.images?.length) {
    const firstvalid = p.resources.images.find(
      img => img.url?.trim() || img.originalUrl?.trim()
    )
    if (firstvalid) {
      return (firstvalid.url || firstvalid.originalUrl)?.trim()
    }
  }

  return null
})

const imageStyle = computed(() => {
  if (!resolvedImage.value) return {}
  // Ensure we handle both relative and absolute URLs if needed, though usually just URL is fine
  return {
    backgroundImage: `url('${resolvedImage.value}')`,
  }
})

const motionClass = ref('product-hero-background__image--drift')

onMounted(() => {
  motionClass.value =
    Math.random() > 0.5
      ? 'product-hero-background__image--drift'
      : 'product-hero-background__image--zoom'
})
</script>

<style scoped>
.product-hero-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  border-radius: inherit;
  background:
    radial-gradient(
      circle at 20% 20%,
      rgba(var(--v-theme-hero-gradient-start), 0.12),
      transparent 45%
    ),
    radial-gradient(
      circle at 90% 10%,
      rgba(var(--v-theme-hero-gradient-end), 0.08),
      transparent 40%
    ),
    linear-gradient(
      140deg,
      rgba(var(--v-theme-surface-ice-050), 0.82),
      rgba(var(--v-theme-surface-glass), 0.94)
    );
}

.product-hero-background__gradient {
  position: absolute;
  inset: -20%;
  background: radial-gradient(
    circle at 50% 20%,
    rgba(var(--v-theme-hero-overlay-strong), 0.08),
    rgba(var(--v-theme-hero-gradient-mid), 0.06),
    transparent 70%
  );
}

.product-hero-background__image {
  position: absolute;
  inset: -10%;
  background-size: cover;
  background-position: center;
  opacity: 0.25; /* "Transparent / Opacified" effect */
  filter: saturate(1.1) blur(3px); /* Shading effect */
  background-attachment: scroll;
  transition: opacity 0.5s ease;
  will-change: transform;
}

.product-hero-background__image--fallback {
  opacity: 0.35;
  filter: saturate(1.15) blur(2px);
}

.product-hero-background__image--drift {
  animation: hero-drift 28s ease-in-out infinite alternate;
}

.product-hero-background__image--zoom {
  animation: hero-zoom 32s ease-in-out infinite alternate;
}

@keyframes hero-drift {
  0% {
    transform: scale(1.04) translate3d(-1.5%, -1%, 0);
  }
  100% {
    transform: scale(1.08) translate3d(1.5%, 1%, 0);
  }
}

@keyframes hero-zoom {
  0% {
    transform: scale(1.03);
  }
  100% {
    transform: scale(1.12);
  }
}

@media (max-width: 960px) {
  .product-hero-background__image--drift {
    animation-duration: 40s;
  }

  .product-hero-background__image--zoom {
    animation-duration: 44s;
  }
}

@media (prefers-reduced-motion: reduce) {
  .product-hero-background__image--drift,
  .product-hero-background__image--zoom {
    animation: none;
    transform: scale(1.02);
  }
}

.product-hero-background__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(
      rgba(var(--v-theme-border-primary-strong), 0.07) 1px,
      transparent 1px
    ),
    linear-gradient(
      90deg,
      rgba(var(--v-theme-border-primary-strong), 0.07) 1px,
      transparent 1px
    );
  background-size: 120px 120px;
  mask-image: radial-gradient(
    circle at 50% 30%,
    rgba(0, 0, 0, 0.6),
    transparent 70%
  );
}

.product-hero-background__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(var(--v-theme-surface-default), 0.2) 0%,
    rgba(var(--v-theme-surface-default), 0.6) 100%
  );
  mix-blend-mode: overlay;
}

.product-hero-background__glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.product-hero-background__glow--primary {
  top: -80px;
  left: -120px;
  background: rgba(var(--v-theme-hero-gradient-start), 0.35);
}

.product-hero-background__glow--accent {
  bottom: -180px;
  right: -140px;
  background: rgba(var(--v-theme-hero-gradient-end), 0.38);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
