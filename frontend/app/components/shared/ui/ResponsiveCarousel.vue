<template>
  <div class="responsive-carousel">
    <v-carousel
      v-model="currentSlide"
      class="responsive-carousel__container"
      :aria-label="ariaLabel"
      :show-arrows="showControls"
      :cycle="showControls"
      :hide-delimiters="slides.length <= 1"
      :hide-delimiter-background="true"
      :touch="showControls"
      height="auto"
      role="region"
    >
      <v-carousel-item
        v-for="(slideItems, index) in slides"
        :key="`carousel-slide-${index}`"
      >
        <div class="responsive-carousel__slide" role="list">
          <div
            v-for="(item, itemIndex) in slideItems"
            :key="itemKey(item, itemIndex)"
            class="responsive-carousel__item"
            role="listitem"
          >
            <slot name="item" :item="item" :index="itemIndex" />
          </div>
        </div>
      </v-carousel-item>
    </v-carousel>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'

type BreakpointKey = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

const props = withDefaults(
  defineProps<{
    items: unknown[]
    ariaLabel: string
    breakpoints?: Partial<Record<BreakpointKey, number>>
    itemKey?: (item: unknown, index: number) => string | number
  }>(),
  {
    breakpoints: () => ({ xs: 1, sm: 1, md: 2, lg: 3, xl: 3 }),
    itemKey: (_item: unknown, index: number) => index,
  }
)

const display = useDisplay()

const currentSlide = ref(0)

const itemsPerSlide = computed(() => {
  const config = props.breakpoints ?? {}

  if (display.xl.value) {
    return Math.max(
      1,
      config.xl ?? config.lg ?? config.md ?? config.sm ?? config.xs ?? 1
    )
  }

  if (display.lg.value) {
    return Math.max(1, config.lg ?? config.md ?? config.sm ?? config.xs ?? 1)
  }

  if (display.md.value) {
    return Math.max(1, config.md ?? config.sm ?? config.xs ?? 1)
  }

  if (display.sm.value) {
    return Math.max(1, config.sm ?? config.xs ?? 1)
  }

  return Math.max(1, config.xs ?? 1)
})

const slides = computed(() => {
  const chunkSize = itemsPerSlide.value
  const groups: unknown[][] = []

  props.items.forEach((item, index) => {
    const groupIndex = Math.floor(index / chunkSize)
    if (!groups[groupIndex]) {
      groups[groupIndex] = []
    }
    groups[groupIndex]?.push(item)
  })

  return groups
})

const showControls = computed(() => slides.value.length > 1)

watch(
  () => slides.value.length,
  length => {
    if (currentSlide.value >= length) {
      currentSlide.value = 0
    }
  }
)

watch(
  () => props.items.length,
  () => {
    currentSlide.value = 0
  }
)

watch(
  () => props.items,
  () => {
    currentSlide.value = 0
  },
  { deep: false }
)
</script>

<style scoped>
.responsive-carousel__container {
  background: transparent;
}

.responsive-carousel__slide {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  padding: 0 0.5rem 0.5rem;
}

.responsive-carousel__item {
  height: 100%;
  display: flex;
}

.responsive-carousel__item > :deep(*) {
  flex: 1 1 auto;
}

@media (max-width: 600px) {
  .responsive-carousel__slide {
    gap: 1.25rem;
    padding-inline: 0;
  }
}
</style>
