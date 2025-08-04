<script setup lang="ts">
interface Category {
  title: string
  image: string
  href: string
}

// Static list of categories for display
const categories: Category[] = [
  {
    title: 'Refrigerators',
    image: 'https://via.placeholder.com/300x200?text=Refrigerators',
    href: '/categories/refrigerators',
  },
  {
    title: 'Washing Machines',
    image: 'https://via.placeholder.com/300x200?text=Washing+Machines',
    href: '/categories/washing-machines',
  },
  {
    title: 'Dishwashers',
    image: 'https://via.placeholder.com/300x200?text=Dishwashers',
    href: '/categories/dishwashers',
  },
]
</script>

<template>
  <v-carousel class="category-carousel" show-arrows>
    <v-carousel-item
      v-for="(category, index) in categories"
      :key="index"
      class="category-item"
    >
      <NuxtLink :to="category.href" class="d-block h-100">
        <v-img
          :src="category.image"
          :alt="category.title"
          cover
          class="fill-height"
        />
        <div class="carousel-title">{{ category.title }}</div>
      </NuxtLink>
    </v-carousel-item>
  </v-carousel>
</template>

<style scoped lang="sass">
.category-carousel
  height: 200px

@media (min-width: 600px)
  .category-carousel
    height: 300px

.carousel-title
  position: absolute
  bottom: 0
  left: 0
  right: 0
  background: rgba(0, 0, 0, 0.5)
  color: white
  text-align: center
  padding: 0.5rem
</style>
