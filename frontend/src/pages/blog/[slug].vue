<template>
  <div v-if="post">
    <h1>{{ post.title }}</h1>
    <Author :name="post.author" class="mb-2" />
    <PostContent :body="post.body || ''" />
  </div>
</template>

<script setup lang="ts">
import { useRoute, useAsyncData } from '#imports'
import Author from '@/components/blog/Author.vue'
import PostContent from '@/components/blog/PostContent.vue'
import { useBlogApi } from '@/composables/useBlogApi'

const blogApi = useBlogApi()
const route = useRoute()

const { data: post } = await useAsyncData('post', () => blogApi.post({ slug: route.params.slug as string }))
</script>
