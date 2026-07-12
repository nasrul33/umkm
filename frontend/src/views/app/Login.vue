<template>
  <main class="min-h-screen flex items-center justify-center bg-gray-50 font-body">
    <form @submit.prevent="login" class="bg-white p-8 rounded-lg shadow-sm w-full max-w-sm">
      <h1 class="font-display text-2xl mb-6 text-center">Masuk ke Aplikasi Akuntansi</h1>
      <input v-model="username" type="text" placeholder="Email/username"
             class="w-full border rounded px-3 py-2 mb-3" />
      <input v-model="password" type="password" placeholder="Password"
             class="w-full border rounded px-3 py-2 mb-4" />
      <button type="submit" class="w-full bg-brand-primary text-white rounded py-2 hover:opacity-90 transition">
        Masuk
      </button>
      <p v-if="error" class="text-red-600 text-sm mt-3">{{ error }}</p>
    </form>
  </main>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../../stores/auth";
import client from "../../api/client";

const username = ref("");
const password = ref("");
const error = ref("");
const router = useRouter();
const auth = useAuthStore();

async function login() {
  try {
    const { data } = await client.post("/app/auth/login", { username: username.value, password: password.value });
    auth.setSession(data.token, data.role);
    router.push({ name: "app-dashboard" });
  } catch (e) {
    error.value = "Login gagal. Periksa kembali username/password.";
  }
}
</script>
