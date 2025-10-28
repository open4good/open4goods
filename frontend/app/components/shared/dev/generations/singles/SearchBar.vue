<template>
  <v-container class="search-bar pa-0" fluid>
    <v-row align="center" class="search-bar__row ga-2">
      <v-col cols="12" sm="auto" class="flex-grow-1">
        <v-text-field
          v-model="searchQuery"
          placeholder="Search"
          variant="outlined"
          density="compact"
          hide-details
          @keyup.enter="onSearch"
          class="search-bar__input"
        />
      </v-col>
      <v-col cols="12" sm="auto">
        <v-btn
          color="primary"
          variant="elevated"
          size="default"
          @click="onSearch"
          class="search-bar__button"
        >
          Search
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  modelValue?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'search', query: string): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: ''
})

const emit = defineEmits<Emits>()

const searchQuery = ref(props.modelValue)

watch(
  () => props.modelValue,
  (newVal) => {
    searchQuery.value = newVal
  }
)

const onSearch = () => {
  emit('update:modelValue', searchQuery.value)
  emit('search', searchQuery.value)
}
</script>

<style lang="sass" scoped>
.search-bar
  // Base container styles
  display: flex
  align-items: center

  &__row
    width: 100%

  &__input
    width: 100%

  &__button
    white-space: nowrap
    width: 100%

    @media (min-width: 600px)
      width: auto
</style>
