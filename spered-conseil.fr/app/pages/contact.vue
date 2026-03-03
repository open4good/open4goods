<template>
  <SectionPage :slug-segments="['contact']">
    <template #after-content>
      <v-container class="pb-10">
        <v-row>
          <v-col cols="12" md="10" lg="8" class="mx-auto">
            <ContactForm />
          </v-col>
        </v-row>
      </v-container>
    </template>
  </SectionPage>
</template>

<script setup lang="ts">
import ContactForm from '~/components/contact/ContactForm.vue'
import SectionPage from '~/components/content/SectionPage.vue'
</script>
