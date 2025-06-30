<template>
  <div>
    <h1>{{ t('blog.title') }}</h1>
    <div class="my-2">
      <label class="mr-2">{{ t('blog.tags') }}</label>
      <select v-model="selectedTag" class="border px-2 py-1">
        <option value="">All</option>
        <option v-for="tag in tags" :key="tag.name" :value="tag.name">
          {{ tag.name }} ({{ tag.count }})
        </option>
      </select>
    </div>
    <PostPreview v-for="p in posts" :key="p.url" :post="p" />
    <p v-if="posts.length === 0">{{ t('blog.noPosts') }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAsyncData, useI18n } from '#imports'
import PostPreview from '@/components/blog/PostPreview.vue'
import { useBlogApi } from '@/composables/useBlogApi'

const { t } = useI18n()
const route = useRoute()
const blogApi = useBlogApi()
const selectedTag = ref<string | undefined>(route.query.tag as string)

const { data: posts } = await useAsyncData('posts', () => blogApi.posts({ tag: selectedTag.value }))
const { data: tags } = await useAsyncData('tags', () => blogApi.tags())

watch(selectedTag, async () => {
  posts.value = await blogApi.posts({ tag: selectedTag.value })
})
</script>
