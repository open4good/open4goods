<template>
  <div class="d-flex align-center star-rating">
    <v-icon
      v-for="n in fullStars"
      :key="`full-${n}`"
      class="star star-full"
      color="warning"
      size="24"
    >mdi-star</v-icon>
    <div v-if="hasPartialStar" class="partial-star star-partial">
      <v-icon class="star-outline" color="warning" size="24">mdi-star-outline</v-icon>
      <v-icon
        class="star-front"
        color="warning"
        size="24"
        :style="{ width: `${partialWidth}%` }"
      >mdi-star</v-icon>
    </div>
    <v-icon
      v-for="n in emptyStars"
      :key="`empty-${n}`"
      class="star star-empty"
      color="warning"
      size="24"
    >mdi-star-outline</v-icon>
  </div>
</template>

<script setup lang="ts">
interface Props {
  rating: number
  max?: number
}

const props = withDefaults(defineProps<Props>(), {
  max: 5,
})

const fullStars = computed(() => Math.floor(props.rating))
const fraction = computed(() => {
  const value = props.rating - fullStars.value
  return value > 0 ? value : 0
})
const hasPartialStar = computed(
  () => fraction.value > 0 && fullStars.value < props.max
)
const partialWidth = computed(() => Math.round(fraction.value * 100))
const emptyStars = computed(
  () => props.max - fullStars.value - (hasPartialStar.value ? 1 : 0)
)
</script>

<style scoped>
.star-rating .star {
  line-height: 1;
}
.partial-star {
  position: relative;
  width: 24px;
  height: 24px;
}
.partial-star .star-outline,
.partial-star .star-front {
  position: absolute;
  top: 0;
  left: 0;
}
.partial-star .star-front {
  overflow: hidden;
}
</style>
