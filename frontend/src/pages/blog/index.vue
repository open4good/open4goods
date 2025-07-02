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
    <Pagination v-if="meta.totalPages > 1" :meta="meta" @change="pageNumber = $event" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAsyncData, useI18n } from '#imports'
import PostPreview from '@/components/blog/PostPreview.vue'
import Pagination from '@/components/Pagination.vue'
import { useBlogApi } from '@/composables/useBlogApi'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const blogApi = useBlogApi()
const selectedTag = ref<string | undefined>(route.query.tag as string)
const pageNumber = ref<number>(route.query.page ? Number(route.query.page) : 0)

const defaultPage = { page: { number: 0, size: 10, totalElements: 0, totalPages: 0 }, data: [] }

const { data: page } = await useAsyncData(
  'posts',
  () => blogApi.posts({ tag: selectedTag.value, pageNumber: pageNumber.value }),
  { default: () => defaultPage }
)
const posts = computed(() => page.value?.data ?? [])
const meta = computed(() => page.value?.page ?? defaultPage.page)
const { data: tags } = await useAsyncData('tags', () => blogApi.tags(), { default: () => [] })

async function load() {
  page.value = await blogApi.posts({ tag: selectedTag.value, pageNumber: pageNumber.value })
  router.replace({ query: { tag: selectedTag.value, page: pageNumber.value.toString() } })
}

watch(selectedTag, async () => {
  pageNumber.value = 0
  await load()
})

watch(pageNumber, async () => {
  await load()
})
</script>
