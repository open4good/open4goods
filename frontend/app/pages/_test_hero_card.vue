<script setup lang="ts">
import RoundedCornerCard from '~/components/shared/cards/RoundedCornerCard.vue'
</script>

<template>
  <v-container>
    <v-row>
      <v-col cols="12" md="6">
        <RoundedCornerCard
          title="Hero Surface Card"
          subtitle="This uses the restored hero image"
          surface="hero"
          accent-corner="bottom-right"
          corner-icon="mdi-star"
          rounded="lg"
        >
          <p>
            This is a card with surface="hero". It should display the hero
            background from the theme.
          </p>
        </RoundedCornerCard>
      </v-col>
      <v-col cols="12" md="6">
        <RoundedCornerCard
          title="Standard Card"
          subtitle="Surface glass"
          surface="glass"
          accent-corner="bottom-left"
          corner-icon="mdi-home"
        >
          <p>Standard card for comparison.</p>
        </RoundedCornerCard>
      </v-col>
    </v-row>
  </v-container>
</template>
