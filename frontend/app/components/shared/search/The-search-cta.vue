<template>
  <div class="search-bar-container pa-0" fluid>
    <v-row align="center" justify="center" class="search-bar__row ga-2">
      <v-col cols="12" sm="auto" class="flex-grow-1 pa-0">
        <v-text-field
          v-model="searchQuery"
          placeholder="Nom, marque, produit..."
          variant="outlined"
          density="compact"
          hide-details
          @keyup.enter="onSearch"
          class="search-bar__input bg-white rounded-lg"
        />
      </v-col>
      <v-col cols="12" sm="auto" class="pa-0">
        <v-btn
          color="primary"
          variant="elevated"
          size="default"
          @click="onSearch"
          class="search-bar__button text-white font-weight-black m-0"
        >
          Nudge !
        </v-btn>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";

interface Props {
  modelValue?: string;
}

interface Emits {
  (e: "update:modelValue", value: string): void;
  (e: "search", query: string): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: "",
});

const emit = defineEmits<Emits>();

const searchQuery = ref(props.modelValue);

watch(
  () => props.modelValue,
  (newVal) => {
    searchQuery.value = newVal;
  }
);

const onSearch = () => {
  emit("update:modelValue", searchQuery.value);
  emit("search", searchQuery.value);
};
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

    // Remove black focus outline
    :deep(.v-field)
      &:focus-within
        outline: none
        box-shadow: none

    :deep(.v-field__outline__start),
    :deep(.v-field__outline__end),
    :deep(.v-field__outline__notch)
      border-color: inherit !important

    :deep(input)
      &:focus
        outline: none

  &__button
    white-space: nowrap
    width: 100%
    display: block

    @media (min-width: 600px)
      width: auto
      display: inline-block
</style>
