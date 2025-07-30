<template>
  <v-container class="d-flex justify-center">
    <v-card width="400">
      <v-card-title>Login</v-card-title>
      <v-card-text>
        <v-form @submit.prevent="submit">
          <v-text-field v-model="username" label="Username" required />
          <v-text-field
            v-model="password"
            label="Password"
            type="password"
            required
          />
          <v-btn
            type="submit"
            color="primary"
            :loading="loading"
            class="mt-4"
            block
            >Login</v-btn
          >
          <v-alert v-if="error" type="error" class="mt-4">{{ error }}</v-alert>
        </v-form>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

const submit = async () => {
  loading.value = true
  error.value = null
  try {
    await $fetch('/auth/login', {
      method: 'POST',
      body: { username: username.value, password: password.value },
    })
    await router.push('/')
  } catch (err) {
    error.value = 'Invalid credentials'
    console.error('Login error', err)
  } finally {
    loading.value = false
  }
}
</script>
