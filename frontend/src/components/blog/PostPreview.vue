<template>
  <article class="border rounded p-4 mb-4">
    <h2 class="text-xl font-bold">{{ post.title }}</h2>
    <Author :name="post.author" class="mt-2" />
    <p class="mt-2">{{ post.summary }}</p>
    <ul v-if="post.category?.length" class="flex gap-2 mt-2">
      <li v-for="tag in post.category" :key="tag" class="text-sm bg-gray-200 px-2 rounded">
        {{ tag }}
      </li>
    </ul>
  </article>
</template>

<script setup lang="ts">
import Author from './Author.vue'
import type { BlogPostDto } from '@/api'

defineProps<{ post: BlogPostDto }>()
</script>
