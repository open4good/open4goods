<template>
  <v-row
    class="product-ai-review-insight"
    :class="{ 'product-ai-review-insight--right': isRight }"
  >
    <v-col cols="12" md="4" class="product-ai-review-insight__media">
      <div class="product-ai-review-insight__media-frame">
        <v-img
          :src="imageSrc"
          :alt="imageAlt"
          height="140"
          width="100%"
          contain
        />
      </div>
    </v-col>
    <v-col cols="12" md="8" class="product-ai-review-insight__content">
      <!-- eslint-disable vue/no-v-html -->
      <div class="product-ai-review__card-text" v-html="contentHtml" />
      <!-- eslint-enable vue/no-v-html -->
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  contentHtml: {
    type: String,
    default: '',
  },
  imageSrc: {
    type: String,
    required: true,
  },
  imageAlt: {
    type: String,
    default: '',
  },
  imagePosition: {
    type: String as () => 'left' | 'right',
    default: 'left',
  },
})

const isRight = computed(() => props.imagePosition === 'right')
</script>

<style scoped>
.product-ai-review-insight {
  align-items: center;
  row-gap: 1rem;
}

.product-ai-review-insight__media-frame {
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-ai-review__card-text {
  font-size: 1rem;
  line-height: 1.5;
}

.product-ai-review-insight__media :deep(img) {
  object-fit: contain;
}

@media (min-width: 960px) {
  .product-ai-review-insight--right .product-ai-review-insight__media {
    order: 2;
  }

  .product-ai-review-insight--right .product-ai-review-insight__content {
    order: 1;
  }
}
</style>
