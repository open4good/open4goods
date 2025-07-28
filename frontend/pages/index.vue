<template>
  <v-container class="pa-0">
    <!-- Hero section with call to action -->
    <section class="hero d-flex flex-column align-center justify-center">
      <v-img src="/nudger-icon-512x512.png" width="120" class="mb-4" alt="Nudger logo" />
      <h1 class="text-h3 text-center font-weight-bold mb-2">Welcome to Nudger</h1>
      <p class="text-subtitle-1 mb-6 text-center">
        Compare appliances and make greener choices thanks to our Impact Score
      </p>
      <router-link to="/blog">
        <v-btn color="primary" size="large" rounded="pill" class="elevation-2">
          <v-icon start icon="mdi-book-open" />
          Read our blog
        </v-btn>
      </router-link>
    </section>

    <!-- Key features section -->
    <section class="features">
      <v-row justify="center" class="text-center">
        <v-col cols="12" md="4" class="mb-6">
          <v-card rounded="lg" elevation="4" class="pa-6">
            <v-icon size="48" color="primary" class="mb-3">mdi-leaf</v-icon>
            <div class="text-h6 font-weight-medium mb-2">Impact Score</div>
            <p class="mb-0">
              We aggregate energy data from multiple sources to help you choose
              the most efficient products.
            </p>
          </v-card>
        </v-col>

        <v-col cols="12" md="4" class="mb-6">
          <v-card rounded="lg" elevation="4" class="pa-6">
            <v-icon size="48" color="primary" class="mb-3">mdi-compare</v-icon>
            <div class="text-h6 font-weight-medium mb-2">Smart Comparison</div>
            <p class="mb-0">
              Easily compare offers and prices to find the best deal for your
              needs.
            </p>
          </v-card>
        </v-col>

        <v-col cols="12" md="4" class="mb-6">
          <v-card rounded="lg" elevation="4" class="pa-6">
            <v-icon size="48" color="primary" class="mb-3">mdi-account</v-icon>
            <div class="text-h6 font-weight-medium mb-2">Community Advice</div>
            <p class="mb-0">
              Benefit from user reviews and tips to make informed decisions.
            </p>
          </v-card>
        </v-col>
      </v-row>
    </section>

    <!-- Example content bloc -->
    <section>
      <TextContent blocId="Main.WebHome" />
    </section>
  </v-container>
</template>

<script setup lang="ts">
// Static landing page - no specific script logic yet
</script>

<style lang="sass" scoped>
.hero
  min-height: 60vh
  background: linear-gradient(45deg, #4caf50, #1976d2)
  color: white
  text-align: center
  padding: 80px 20px

.features
  padding: 60px 20px
</style>
