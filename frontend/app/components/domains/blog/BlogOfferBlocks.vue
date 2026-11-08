<template>
  <section
    v-if="config"
    class="blog-offer-blocks"
    data-test="blog-offer-blocks"
    @click.capture="onBlocksClick"
  >
    <GuideProductGrid
      v-for="block in config.blocks"
      :key="block.id"
      :vertical="config.vertical"
      :top="3"
      :filters="block.filters"
      require-offers
      hide-when-empty
      class="blog-offer-blocks__block"
      :data-block="block.id"
    >
      <template #header>
        <h2 class="blog-offer-blocks__title">{{ t(block.titleKey) }}</h2>
      </template>
      <template #footer>
        <NuxtLink :to="config.categoryPath" class="blog-offer-blocks__more">
          {{ t('blog.offerBlocks.seeAll') }}
        </NuxtLink>
      </template>
    </GuideProductGrid>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import GuideProductGrid from '~/components/product/GuideProductGrid.vue'
import { useAnalytics } from '~/composables/useAnalytics'
import { getBlogOfferBlocks } from '~/utils/blog-offer-blocks'

const props = defineProps<{ slug?: string | null }>()

const { t } = useI18n()
const { trackEvent } = useAnalytics()

const config = computed(() => getBlogOfferBlocks(props.slug))

const onBlocksClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (!target?.closest || !config.value || !target.closest('a[href]')) {
    return
  }
  const blockId = target.closest<HTMLElement>('[data-block]')?.dataset.block
  if (!blockId) {
    return
  }
  trackEvent('blog-offer-block-click', {
    props: {
      article: props.slug,
      vertical: config.value.vertical,
      block: blockId,
    },
  })
}
</script>

<style scoped lang="sass">
.blog-offer-blocks
  margin-top: 2rem

  &__title
    font-size: clamp(1.25rem, 1.5vw, 1.5rem)
    margin: 1.5rem 0 0.75rem

  &__more
    display: inline-block
    margin-top: 0.5rem
    font-weight: 600
</style>
