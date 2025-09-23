<script setup lang="ts">
const drawerStore = useState("mobileDrawer", () => false);

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value;
};
</script>

<template>
  <div>
    <Hero-section @toggle-drawer="toggleDrawer" />

    <The-items-selector />

    <Bloc3Hz />

    <TheMainFooter>
      <template #footer>
        <the-main-footer-content />
      </template>
    </TheMainFooter>
  </div>
</template>

<style lang="sass" scoped></style>
