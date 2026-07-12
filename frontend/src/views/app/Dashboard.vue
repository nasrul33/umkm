<template>
  <main class="min-h-screen bg-gray-50 font-body p-8">
    <h1 class="font-display text-2xl mb-6">Ringkasan Keuangan</h1>
    <!-- SRS-B4-08: dashboard ringkasan (kas, laba, piutang/hutang jatuh tempo) -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
      <div class="bg-white rounded-lg p-5 shadow-sm">
        <p class="text-sm text-gray-500">Saldo Kas</p>
        <p class="text-2xl font-medium mt-1">Rp {{ summary.saldoKas?.toLocaleString('id-ID') }}</p>
      </div>
      <div class="bg-white rounded-lg p-5 shadow-sm">
        <p class="text-sm text-gray-500">Laba Bulan Ini</p>
        <p class="text-2xl font-medium mt-1">Rp {{ summary.labaBulanIni?.toLocaleString('id-ID') }}</p>
      </div>
      <div class="bg-white rounded-lg p-5 shadow-sm">
        <p class="text-sm text-gray-500">Piutang Jatuh Tempo</p>
        <p class="text-2xl font-medium mt-1">Rp {{ summary.piutangJatuhTempo?.toLocaleString('id-ID') }}</p>
      </div>
    </div>

    <router-link :to="{ name: 'transaction-wizard' }"
                 class="inline-block bg-brand-secondary text-white px-5 py-2.5 rounded-full hover:opacity-90 transition">
      + Catat Transaksi
    </router-link>
  </main>
</template>

<script setup>
import { ref, onMounted } from "vue";
import client from "../../api/client";

const summary = ref({});
onMounted(async () => {
  const { data } = await client.get("/app/reports/dashboard-summary");
  summary.value = data;
});
</script>
