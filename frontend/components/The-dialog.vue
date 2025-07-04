<template>
  <div class="pa-10">
    <v-dialog max-width="500">
      <template #activator="{ props: activatorProps }">
        <div class="d-flex justify-center align-center">
          <v-btn
            v-bind="activatorProps"
            color="surface-variant"
            text="Click me"
            variant="flat"
          />
        </div>
      </template>

      <template #default="{ isActive }">
        <v-card title="Dialog">
          <v-card-text>
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua.
          </v-card-text>

          <v-card-actions>
            <v-spacer />

            <v-btn text="Close Dialog" @click="isActive.value = false" />
          </v-card-actions>
        </v-card>
      </template>
    </v-dialog>
  </div>
</template>

<script setup>
// Component logic here
</script>
