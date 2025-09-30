<template>
  <v-container class="container-section_bloc-3" fluid>
    <v-row justify="center" align="center" no-gutters>
      <v-col
        v-for="(section, index) in sections"
        :key="index"
        cols="12"
        sm="6"
        md="4"
        class="pa-2 d-flex justify-center"
      >
        <v-sheet
          :color="section.color"
          :height="section.height"
          :width="section.width"
          rounded="xl"
          class="d-flex justify-center align-center"
        >
          <!-- Placeholder for section content -->
        </v-sheet>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
interface SectionConfig {
  color: string
  height: number | string
  width: number | string
}

withDefaults(
  defineProps<{
    sections?: SectionConfig[]
  }>(),
  {
    sections: () => [
      { color: 'grey', height: 300, width: 400 },
      { color: 'grey', height: 300, width: 400 },
      { color: 'grey', height: 300, width: 400 },
    ],
  }
)
</script>

<style lang="sass" scoped>
.container-section_bloc-3
  min-height: 100vh
</style>
