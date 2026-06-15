<template>
  <div class="b2b-playground-shell">
    <section class="b2b-playground-shell__request">
      <slot name="request" />
    </section>
    <section class="b2b-playground-shell__response">
      <slot name="response" />
    </section>
    <aside v-if="$slots.examples" class="b2b-playground-shell__examples">
      <slot name="examples" />
    </aside>
  </div>
</template>

<style scoped>
.b2b-playground-shell {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
  gap: 16px;
}

.b2b-playground-shell__request,
.b2b-playground-shell__response,
.b2b-playground-shell__examples {
  min-width: 0;
}

.b2b-playground-shell__examples {
  grid-column: 1 / -1;
}

@media (max-width: 960px) {
  .b2b-playground-shell {
    grid-template-columns: 1fr;
  }
}
</style>
