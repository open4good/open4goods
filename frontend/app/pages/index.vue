<script setup lang="ts">
const drawerStore = useState("mobileDrawer", () => false);

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value;
};
</script>

<template>
  <div class="full-height">
    <Hero-section @toggle-drawer="toggleDrawer" />

    <The-section-items-slide />

    <Bloc-3-hz />

    <The-bloc-text-image
      image-src="https://placehold.co"
      title="Main feature text"
      subtitle="This is a subtitle"
      description="Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
      ctaLabel="bouton"
    />

    <v-container>
      <The-search-cta />
    </v-container>

    <The-bloc-features />
  </div>
</template>

<style lang="sass" scoped></style>
