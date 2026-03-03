<template>
  <v-container class="py-6">
    <v-row>
      <v-col cols="12" md="10" lg="8" class="mx-auto">
        <article>
          <h2 v-if="section.title" class="text-h4 mb-2">{{ section.title }}</h2>
          <p v-if="section.description" class="text-body-1 mb-4 text-medium-emphasis">
            {{ section.description }}
          </p>
          <ContentRenderer :value="section" />
        </article>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
defineProps<{
  section: Record<string, unknown>
}>()
</script>
