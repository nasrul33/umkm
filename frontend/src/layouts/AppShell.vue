<template>
  <div class="min-h-screen bg-slate-50 font-body lg:flex">
    <!-- Sidebar desktop -->
    <aside class="hidden lg:flex lg:flex-col w-[260px] shrink-0 bg-brand-primary text-white min-h-screen sticky top-0">
      <div class="px-6 py-8">
        <p class="font-display text-xl leading-tight">SIA-UMKM</p>
        <p class="text-xs text-white/60 mt-1">Aplikasi Akuntansi</p>
      </div>
      <nav class="flex-1 px-3 space-y-1">
        <router-link v-for="item in menu" :key="item.name" :to="{ name: item.name }"
                     class="flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm text-white/80 transition-colors duration-150 hover:bg-white/10 hover:text-white"
                     :class="{ 'bg-white/10 text-white border-l-[3px] border-brand-secondary pl-[13px]': isActive(item.name) }">
          <span aria-hidden="true">{{ item.ikon }}</span>{{ item.label }}
        </router-link>
      </nav>
      <div class="px-6 py-6 border-t border-white/10 text-sm">
        <p class="text-white/80">{{ auth.role === 'OWNER' ? 'Pemilik' : 'Staf Input' }}</p>
        <button @click="keluar" class="mt-2 text-white/60 hover:text-white transition-colors">Keluar</button>
      </div>
    </aside>

    <!-- Topbar mobile -->
    <header class="lg:hidden sticky top-0 z-20 bg-brand-primary text-white px-5 py-3 flex items-center justify-between">
      <p class="font-display">SIA-UMKM</p>
      <button @click="keluar" class="text-xs text-white/70">Keluar</button>
    </header>

    <!-- Konten -->
    <main class="flex-1 min-w-0 p-6 lg:p-10 pb-24 lg:pb-10">
      <div class="max-w-6xl mx-auto">
        <router-view />
      </div>
    </main>

    <!-- Bottom-nav mobile -->
    <nav class="lg:hidden fixed bottom-0 inset-x-0 z-20 bg-white border-t border-slate-200 grid gap-1 px-2 py-1.5"
         :style="{ gridTemplateColumns: `repeat(${menu.length}, minmax(0, 1fr))` }">
      <router-link v-for="item in menu" :key="item.name" :to="{ name: item.name }"
                   class="flex flex-col items-center gap-0.5 py-1 text-[11px] text-slate-500"
                   :class="{ 'text-brand-secondary font-medium': isActive(item.name) }">
        <span class="text-lg leading-none"
              :class="item.name === 'transaction-wizard' ? 'bg-brand-secondary text-white rounded-full w-9 h-9 flex items-center justify-center -mt-4 shadow-md' : ''">
          {{ item.ikon }}
        </span>
        {{ item.label }}
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

// Menu Laporan & Pajak hanya utk OWNER (UX-gating; otorisasi tetap di backend).
const menu = computed(() => [
  { name: "app-dashboard", label: "Beranda", ikon: "🏠" },
  { name: "transaction-wizard", label: "Catat", ikon: "＋" },
  ...(auth.isOwner
    ? [
        { name: "app-reports", label: "Laporan", ikon: "📄" },
        { name: "app-tax", label: "Pajak", ikon: "🧾" }
      ]
    : [])
]);

const isActive = (name) => route.name === name;

function keluar() {
  auth.logout();
  router.push({ name: "login" });
}
</script>
