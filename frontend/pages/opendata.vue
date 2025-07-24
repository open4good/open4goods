<template>
  <v-container>
    <v-row class="py-10 text-center">
      <v-col>
        <h1 class="text-h4 font-weight-bold">
          Open Data : Base de données GTIN, EAN et ISBN en accès libre
        </h1>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <v-card class="pa-6" elevation="4">
          <v-row>
            <v-col cols="12" md="3" class="text-center">
              <v-img src="/assets/img/opendata.jpg" height="200" alt="opendata" />
            </v-col>
            <v-col cols="12" md="9">
              <p>
                <strong>Libérez la donnée !</strong> Nous mettons à disposition
                une extraction de notre base de données, mise à jour chaque
                semaine.
              </p>
              <ul>
                <li>
                  Ces données sont délivrées sous licence
                  <a
                    href="https://spdx.org/licenses/ODbL-1.0.html#licenseText"
                    target="_blank"
                    rel="nofollow"
                    >Open Database Licence</a
                  >
                </li>
              </ul>
              <v-table density="comfortable" class="mt-4">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Mise à jour</th>
                    <th>Taille du fichier</th>
                    <th>Nombre de produits</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <th class="text-left">Dataset GTIN</th>
                    <td>{{ data?.gtinLastUpdated?.toLocaleString() }}</td>
                    <td>{{ data?.gtinFileSize }}</td>
                    <td>{{ data?.countGtin }}</td>
                    <td class="text-center">
                      <v-btn to="/opendata/gtin" color="primary" size="small">
                        Voir les détails et télécharger
                      </v-btn>
                    </td>
                  </tr>
                </tbody>
              </v-table>
              <v-expansion-panels class="mt-6">
                <v-expansion-panel title="Sources de données et contributions">
                  <v-expansion-panel-text>
                    Les extractions fournies par Nudger sont issues de nos
                    travaux d'agrégation des données produits. Nous intégrons une
                    grande variété de sources de données.
                  </v-expansion-panel-text>
                </v-expansion-panel>
                <v-expansion-panel title="Qu'est-ce que l'open data ?">
                  <v-expansion-panel-text>
                    L'open data désigne des données rendues accessibles à tous,
                    sans restriction d’accès.
                  </v-expansion-panel-text>
                </v-expansion-panel>
                <v-expansion-panel title="Pourquoi l'open data est-il important ?">
                  <v-expansion-panel-text>
                    L'open data est crucial pour promouvoir la transparence et
                    l’innovation.
                  </v-expansion-panel-text>
                </v-expansion-panel>
                <v-expansion-panel
                  title="Qui produit et met à disposition les données ouvertes ?"
                >
                  <v-expansion-panel-text>
                    Les principales sources sont les gouvernements, les
                    institutions publiques et parfois des entreprises privées.
                  </v-expansion-panel-text>
                </v-expansion-panel>
              </v-expansion-panels>
            </v-col>
          </v-row>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
const { data } = await useOpenData()
</script>
