<template>
  <v-container
    class="d-flex flex-column align-center justify-center min-height-screen pa-8"
  >
    <v-row>
      <v-card class="text-center pa-8 mb-8 bg-green" elevation="4" rounded="lg">
        <h1 class="text-h3 font-weight-bold text-white mb-4"> Hello Nudger !</h1>
        <v-icon icon="mdi-thumb-up" color="white" size="x-large" />
        <p>
            beta2.nudger.fr reflète l'état du <a href="https://github.com/open4good/open4goods/tree/main/frontend" target="_blank"> main</a>.
            Il est livré en continue au travers de la CI/CD github.
        </p>
      </v-card>
    </v-row>

    <!-- 2. Carte Welcome to Vuetify centrée -->
    <div class="mb-6">
      <The-card />
    </div>

    <!-- 3. Bouton Open Dialog centré -->
    <div class="mb-6">
      <The-dialog />
    </div>

    <!-- 4. Bouton Blog centré -->
    <router-link to="/blog">
      <v-btn color="success" variant="elevated" size="large" rounded="pill">
        <v-icon start icon="mdi-book-open" />
        Blog
      </v-btn>
    </router-link>
  </v-container>
</template>

<!-- Page d'accueil simple et clean -->
