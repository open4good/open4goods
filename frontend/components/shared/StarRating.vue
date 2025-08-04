<script setup lang="ts">
interface Props {
  rating: number
  max?: number
}

const props = withDefaults(defineProps<Props>(), {
  max: 5,
})

// Determine which icon to display for each star position
const stars = computed(() => {
  const fullStars = Math.floor(props.rating)
  const hasFraction = props.rating - fullStars > 0

  return Array.from({ length: props.max }, (_, index) => {
    if (index < fullStars) {
      return 'mdi-star'
    }
    if (index === fullStars && hasFraction) {
      return 'mdi-star-half-full'
    }
    return 'mdi-star-outline'
  })
})
</script>

<template>
  <div class="star-rating">
    <v-icon
      v-for="(icon, index) in stars"
      :key="index"
      :icon="icon"
      class="mx-1"
    />
  </div>
</template>

<style scoped>
.star-rating {
  display: flex;
  align-items: center;
}
</style>
