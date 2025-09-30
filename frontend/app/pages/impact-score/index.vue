<template>
  <v-container fluid class="pa-0">
    <!-- Bloc héro (corps de page, pas de header/footer) -->
    <v-sheet color="primary" class="py-12">
      <v-container class="text-center">
        <h1 class="text-h4 text-md-h3 text-white font-weight-bold">
          L’Impact Score : évaluation de l'impact environnemental de vos
          produits
        </h1>
        <!-- Remplace le wiki ECOSCORE/1/ -->
        <div class="mt-4 text-white text-body-1">
          <TextContent bloc-id="ECOSCORE:1:" />
        </div>
      </v-container>
    </v-sheet>

    <!-- Contenu -->
    <v-container class="py-8">
      <v-row justify="center">
        <v-col cols="12" lg="9">
          <v-card elevation="2" class="pa-6">
            <!-- Qu’est-ce que l’Impact Score ? -->
            <v-card-title class="px-0">
              <h2 class="text-h6 text-md-h5">
                Qu’est-ce que l’Impact Score&nbsp;?
              </h2>
            </v-card-title>

            <v-card-text class="px-0">
              <div
                class="d-flex justify-center my-4"
                aria-label="Évaluation globale"
              >
                <v-rating
                  v-model="localRating"
                  half-increments
                  length="5"
                  readonly
                  :aria-label="`Évaluation : ${localRating} sur 5`"
                />
              </div>

              <!-- Remplace le wiki ECOSCORE/2/ -->
              <TextContent bloc-id="ECOSCORE:2:" />

              <v-responsive class="my-6" max-width="800">
                <v-img
                  src="https://nudger.fr/img/what-impactscore.png"
                  alt="Illustration Impact Score"
                  aspect-ratio="16/9"
                  cover
                />
              </v-responsive>

              <!-- Notre écoscore -->
              <h2
                id="ecoscore-transparent-innovant"
                class="text-h6 text-md-h5 my-6"
              >
                Notre ecoscore&nbsp;: transparent, innovant et performant
              </h2>

              <!-- Remplace le wiki ECOSCORE/3/ -->
              <TextContent bloc-id="ECOSCORE:3:" />

              <v-responsive class="my-6" max-width="800">
                <v-img
                  src="https://nudger.fr/img/impactscore-illustration.png"
                  alt="Illustration écoscore"
                  aspect-ratio="16/9"
                  cover
                />
              </v-responsive>

              <!-- Calcul -->
              <h2 id="calcule-impact-score" class="text-h6 text-md-h5 my-6">
                Comment est calculé l'Impact Score&nbsp;?
              </h2>
              <p class="mb-4">
                Les règles de calcul de l’ecoscore sont détaillées sur chaque
                produit (section «&nbsp;bilan écologique&nbsp;»). Chaque
                catégorie de produit a ses propres critères et pondérations.
                Voici les catégories couvertes :
              </p>

              <!-- Liste de verticales (liens absolus) -->
              <v-list density="comfortable" class="mb-4">
                <v-list-item
                  v-for="v in verticals"
                  :key="v.url"
                  :title="v.title"
                  :href="v.url"
                  link
                >
                  <template #append>
                    <v-icon icon="mdi-chevron-right" />
                  </template>
                </v-list-item>
              </v-list>

              <!-- Remplace le wiki ECOSCORE/4/ -->
              <div class="my-6">
                <TextContent bloc-id="ECOSCORE:4:" />
              </div>

              <!-- Tableau statique des critères -->
              <h2 class="text-h6 text-md-h5 mt-8">
                Critères et coefficients de l'ecoscore téléviseurs
              </h2>

              <v-table class="my-4" density="comfortable">
                <thead>
                  <tr>
                    <th class="text-left">Nom du critère</th>
                    <th class="text-left">Coefficient (0–1)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(c, i) in criterias" :key="i">
                    <td>{{ c.name }}</td>
                    <td>{{ formatCoeff(c.coefficient) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <th>Total</th>
                    <td>
                      <strong>{{ formatCoeff(total) }}</strong>
                    </td>
                  </tr>
                </tfoot>
              </v-table>

              <!-- Relativisation -->
              <v-row class="my-8" align="center">
                <v-col cols="12" lg="8">
                  <TextContent bloc-id="ECOSCORE:4-1:" />
                </v-col>
                <v-col cols="12" lg="4">
                  <v-responsive>
                    <v-img
                      src="https://nudger.fr/img/relativisation.png"
                      alt="Relativisation"
                      aspect-ratio="1"
                      contain
                    />
                  </v-responsive>
                </v-col>
              </v-row>

              <!-- Qualité de la donnée -->
              <v-row class="my-8" align="center">
                <v-col cols="12" lg="4">
                  <v-responsive>
                    <v-img
                      src="https://nudger.fr/img/data-quality.png"
                      alt="Qualité de la donnée"
                      aspect-ratio="1"
                      contain
                    />
                  </v-responsive>
                </v-col>
                <v-col cols="12" lg="8">
                  <TextContent bloc-id="ECOSCORE:4-2:" />
                </v-col>
              </v-row>

              <v-divider class="my-6" />
              <v-row>
                <v-col cols="12" class="d-flex flex-wrap ga-2">
                  <v-chip
                    size="small"
                    variant="tonal"
                    @click="scrollTo('#ecoscore-transparent-innovant')"
                  >
                    Transparence & innovation
                  </v-chip>
                  <v-chip
                    size="small"
                    variant="tonal"
                    @click="scrollTo('#calcule-impact-score')"
                  >
                    Calcul
                  </v-chip>
                  <v-chip
                    size="small"
                    variant="tonal"
                    @click="scrollTo('body')"
                  >
                    Haut de page
                  </v-chip>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-container>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  rating: { type: Number, default: 4.5 },
  verticals: { type: Array, default: () => [] }, // [{ title, url }]
  criterias: { type: Array, default: () => [] }, // [{ name, coefficient }]
  total: { type: Number, default: 1.0 },
})

const localRating = ref(props.rating)

function formatCoeff(n) {
  if (n == null || Number.isNaN(Number(n))) return '—'
  return Number(n).toFixed(1)
}

function scrollTo(selector) {
  const el = document.querySelector(selector)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>
