<template>
  <div class="product-impact-cta mb-6">
    <HeroActionCard variant="default">
      <CategoryHeroActionsContent
        :vertical-home-url="categoryLink"
        :category-name="categoryName"
        :show-nudge="false"
        :show-description="false"
        @open-nudge="emit('open-nudge')"
      />
    </HeroActionCard>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import HeroActionCard from '~/components/shared/HeroActionCard.vue'
import CategoryHeroActionsContent from '~/components/category/CategoryHeroActionsContent.vue'
import type { CategoryDto } from '~~/shared/api-client'

const props = defineProps<{
  categoryDetail: CategoryDto
}>()

const emit = defineEmits<{
  (event: 'open-nudge'): void
}>()

const categoryName = computed(() => {
  return props.categoryDetail.title ?? props.categoryDetail.name ?? ''
})

const categoryLink = computed(() => {
  return props.categoryDetail.verticalHomeUrl ?? ''
})
</script>
