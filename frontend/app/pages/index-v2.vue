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

    <Bloc3Hz />

  </div>
</template>

<style lang="sass" scoped></style>
