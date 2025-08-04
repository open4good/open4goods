<template>
  <v-container class="py-4">
    <v-row>
      <v-col v-for="item in items" :key="item.title" cols="12" md="4">
        <v-card>
          <v-card-title class="d-flex align-center">
            <v-icon class="me-2">{{ item.icon }}</v-icon>
            {{ item.title }}
          </v-card-title>
          <v-card-text>{{ item.description }}</v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
definePageMeta({
  title: 'Test Page',
})

useHead({
  title: 'Test Page',
  meta: [
    {
      name: 'description',
      content: 'Demonstration page using Vuetify components',
    },
  ],
})

interface Item {
  title: string
  description: string
  icon: string
}

const items: Item[] = [
  {
    title: 'Card 1',
    description: 'Placeholder content for card one.',
    icon: 'mdi-image',
  },
  {
    title: 'Card 2',
    description: 'Placeholder content for card two.',
    icon: 'mdi-image',
  },
  {
    title: 'Card 3',
    description: 'Placeholder content for card three.',
    icon: 'mdi-image',
  },
]
</script>

<style scoped></style>
