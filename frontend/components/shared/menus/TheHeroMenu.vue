<template>
    <v-container>
        <menu>
            <v-list>
                <v-list-item
                    v-for="(item, index) in ['Home', 'About', 'Services', 'Contact']"
                    :key="index"
                >
                    <v-list-item-title>{{ item }}</v-list-item-title>
                </v-list-item>
            </v-list>
        </menu>
    </v-container>
</template>

<script setup lang="ts">
// content
</script>

<style scoped lang="sass"></style>
