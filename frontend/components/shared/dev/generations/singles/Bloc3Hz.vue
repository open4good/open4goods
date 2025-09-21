<template>
  <v-container fluid class="bloc-3-hz">
    <v-row justify="center" align="center" no-gutters>
      <v-col
        v-for="(section, index) in sections"
        :key="index"
        cols="12"
        sm="6"
        md="4"
        class="pa-2"
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

withDefaults(defineProps<{
  sections?: SectionConfig[]
}>(), {
  sections: () => [
    { color: 'grey-lighten-3', height: 393, width: 468 },
    { color: 'grey-lighten-3', height: 393, width: 469 },
    { color: 'grey-lighten-3', height: 393, width: 468 }
  ]
})
</script>

<style lang="sass" scoped>
.bloc-3-hz
  // Ensures full-width responsive layout
  width: 100%

  // Responsive adjustments
  @media (max-width: 960px)
    // Adjust for tablet/mobile views
    padding: 0 16px
</style>