<script setup lang="ts">
// import { PropType } from 'vue'

interface NudgerToolsButtonProps {
  label?: string
  onClick?: () => void
}

const props = withDefaults(defineProps<NudgerToolsButtonProps>(), {
  label: 'Nudger tools',
  onClick: undefined,
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const handleClick = () => {
  if (props.onClick) {
    props.onClick()
  }
  emit('click')
}
</script>

<template>
  <v-btn
    color="teal-accent-3"
    class="nudger-tools-button"
    rounded="lg"
    @click="handleClick"
  >
    <span class="nudger-tools-button__text">{{ label }}</span>
  </v-btn>
</template>

<style lang="sass" scoped>
.nudger-tools-button
  font-family: 'Rubik One', sans-serif
  font-weight: 400
  text-transform: none
  letter-spacing: normal

  &__text
    font-size: 17.494px
    color: black
</style>
