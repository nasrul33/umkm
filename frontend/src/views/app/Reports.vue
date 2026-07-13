<template>
  <div>
    <div class="flex flex-wrap items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="font-display text-2xl">Laporan Keuangan</h1>
        <p class="text-sm text-slate-500 mt-0.5">Disusun otomatis dari transaksi yang tercatat</p>
      </div>
      <div class="flex items-center gap-2">
        <select v-model.number="tahun" class="border border-slate-300 rounded-full px-4 py-2 text-sm bg-white">
          <option v-for="t in daftarTahun" :key="t" :value="t">{{ t }}</option>
        </select>
        <button class="btn-outline" :disabled="mengunduh" @click="unduh('pdf')">Unduh PDF</button>
        <button class="btn-outline" :disabled="mengunduh" @click="unduh('excel')">Unduh Excel</button>
      </div>
    </div>

    <div role="tablist" class="flex flex-wrap gap-2 mb-6">
      <button v-for="t in tabs" :key="t.id" role="tab" :aria-selected="tab === t.id"
              @click="tab = t.id"
              class="rounded-full px-4 py-2 text-sm transition-colors duration-150 border"
              :class="tab === t.id
                ? 'bg-brand-primary text-white border-brand-primary'
                : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'">
        <span class="font-medium">{{ t.judul }}</span>
        <span class="hidden sm:inline text-xs opacity-70"> — {{ t.sub }}</span>
      </button>
    </div>

    <div v-if="memuat" class="card p-6 space-y-3">
      <div v-for="i in 5" :key="i" class="h-5 rounded bg-slate-100 animate-pulse"></div>
    </div>
    <template v-else>
      <IncomeStatementTable v-if="tab === 'laba-rugi'" :rows="labaRugi" :tahun="tahun" />
      <BalanceSheetTable v-else-if="tab === 'neraca'" :rows="neraca" />
      <CashFlowTable v-else-if="tab === 'arus-kas'" :laporan="arusKas" :tahun="tahun" />
      <EquityChangeCard v-else-if="tab === 'perubahan-modal'" :laporan="perubahanModal" />
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import client from "../../api/client";
import BalanceSheetTable from "../../components/reports/BalanceSheetTable.vue";
import IncomeStatementTable from "../../components/reports/IncomeStatementTable.vue";
import CashFlowTable from "../../components/reports/CashFlowTable.vue";
import EquityChangeCard from "../../components/reports/EquityChangeCard.vue";

const tahunKini = new Date().getFullYear();
const daftarTahun = Array.from({ length: tahunKini - 2022 + 1 }, (_, i) => tahunKini - i);

const tab = ref("laba-rugi"); // paling sering dicek pemilik
const tahun = ref(tahunKini);
const memuat = ref(true);
const mengunduh = ref(false);
const neraca = ref([]);
const labaRugi = ref([]);
const arusKas = ref(null);
const perubahanModal = ref(null);

const tabs = [
  { id: "laba-rugi", judul: "Laba Rugi", sub: "Untung atau rugi usaha Anda" },
  { id: "neraca", judul: "Neraca", sub: "Harta, utang, dan modal usaha" },
  { id: "arus-kas", judul: "Arus Kas", sub: "Dari mana uang masuk, ke mana keluar" },
  { id: "perubahan-modal", judul: "Perubahan Modal", sub: "Perkembangan modal pemilik" }
];

async function muat() {
  memuat.value = true;
  try {
    const [n, l, a, p] = await Promise.all([
      client.get("/app/reports/balance-sheet"),
      client.get("/app/reports/income-statement"),
      client.get("/app/reports/cash-flow", { params: { tahun: tahun.value } }),
      client.get("/app/reports/equity-change", { params: { tahun: tahun.value } })
    ]);
    neraca.value = n.data;
    labaRugi.value = l.data;
    arusKas.value = a.data;
    perubahanModal.value = p.data;
  } finally {
    memuat.value = false;
  }
}

async function unduh(format) {
  mengunduh.value = true;
  try {
    const { data } = await client.get(`/app/reports/export/${format}`, {
      params: { tahun: tahun.value },
      responseType: "blob"
    });
    const url = URL.createObjectURL(data);
    const a = document.createElement("a");
    a.href = url;
    a.download = `laporan-keuangan-${tahun.value}.${format === "excel" ? "xlsx" : "pdf"}`;
    a.click();
    URL.revokeObjectURL(url);
  } finally {
    mengunduh.value = false;
  }
}

onMounted(muat);
watch(tahun, muat);
</script>
