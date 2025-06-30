<!-- eslint-disable vue/no-v-html -->
<template>
  <div v-html="body" />
</template>

<script setup lang="ts">
defineProps<{ body: string }>()
</script>
