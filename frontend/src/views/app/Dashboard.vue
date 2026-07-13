<template>
  <div>
    <h1 class="font-display text-2xl mb-6">Ringkasan Keuangan</h1>
    <!-- SRS-B4-08: dashboard ringkasan (kas, laba, piutang/hutang jatuh tempo) -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
      <div v-for="kartu in kartuAngka" :key="kartu.label" class="card p-5">
        <p class="text-sm text-slate-500">{{ kartu.label }}</p>
        <div v-if="memuat" class="h-8 mt-1 rounded bg-slate-100 animate-pulse"></div>
        <p v-else class="text-2xl font-medium mt-1 angka">{{ formatRupiah(kartu.nilai) }}</p>
      </div>

      <router-link v-if="auth.isOwner" :to="{ name: 'app-tax' }"
                   class="card p-5 flex flex-col justify-between hover:shadow-md transition-shadow duration-150">
        <p class="text-sm text-slate-500">Pajak & Ambang Omzet</p>
        <p class="text-sm font-medium mt-1" style="color: var(--brand-secondary, #8A6D3B)">
          Lihat status ambang &amp; PPh masa →
        </p>
      </router-link>
    </div>

    <router-link :to="{ name: 'transaction-wizard' }" class="btn-secondary">
      + Catat Transaksi
    </router-link>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import client from "../../api/client";
import { formatRupiah } from "../../utils/money";
import { useAuthStore } from "../../stores/auth";

const auth = useAuthStore();
const summary = ref({});
const memuat = ref(true);

const kartuAngka = computed(() => [
  { label: "Saldo Kas", nilai: summary.value.saldoKas ?? 0 },
  { label: "Laba Bulan Ini", nilai: summary.value.labaBulanIni ?? 0 },
  { label: "Piutang Jatuh Tempo", nilai: summary.value.piutangJatuhTempo ?? 0 }
]);

onMounted(async () => {
  try {
    const { data } = await client.get("/app/reports/dashboard-summary");
    summary.value = data;
  } finally {
    memuat.value = false;
  }
});
</script>
