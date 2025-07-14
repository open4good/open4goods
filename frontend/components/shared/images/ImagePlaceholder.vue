<script setup lang="ts">
interface Props {
  width?: number | string
  height?: number | string
  text?: string
  icon?: string
}

const props = withDefaults(defineProps<Props>(), {
  width: 200,
  height: 200,
  text: 'Image non disponible',
  icon: 'mdi-image-off',
})
</script>

<template>
  <div
    class="image-placeholder"
    :style="{
      width: typeof width === 'number' ? `${width}px` : width,
      height: typeof height === 'number' ? `${height}px` : height,
    }"
  >
    <div class="placeholder-content">
      <v-icon
        :icon="icon"
        size="48"
        color="grey-lighten-1"
        class="placeholder-icon"
      />
      <p class="placeholder-text">{{ text }}</p>
    </div>
  </div>
</template>

<style scoped>
.image-placeholder {
  background: #f5f5f5;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.placeholder-content {
  text-align: center;
  color: #999;
}

.placeholder-icon {
  margin-bottom: 8px;
}

.placeholder-text {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}
</style>
