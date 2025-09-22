<script setup lang="ts">
// Accès au state drawer du layout parent
const drawerStore = useState('mobileDrawer', () => false)

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value
}
</script>

<template>
    <Hero-section @toggle-drawer="toggleDrawer" />
</template>

<style lang="sass" scoped></style>
