<template>
  <v-container class="py-10" style="max-width:400px">
    <v-form @submit.prevent="onSubmit" v-model="valid">
      <v-text-field
        v-model="username"
        label="Username"
        required
        prepend-inner-icon="mdi-account"
      />
      <v-text-field
        v-model="password"
        label="Password"
        type="password"
        required
        prepend-inner-icon="mdi-lock"
      />
      <v-btn
        type="submit"
        color="primary"
        class="mt-4"
        :loading="loading"
        :disabled="!valid"
      >
        Login
      </v-btn>
    </v-form>
    <v-alert
      v-if="error"
      type="error"
      class="mt-4"
    >{{ error }}</v-alert>
  </v-container>
</template>

<script setup lang="ts">
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const valid = ref(true)
const router = useRouter()

const onSubmit = async () => {
  loading.value = true
  error.value = ''
  try {
    await $fetch('/auth/login', {
      method: 'POST',
      body: { username: username.value, password: password.value }
    })
    await router.push('/')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>
