<template>
  <main class="min-h-screen bg-gray-50 font-body p-8 max-w-lg mx-auto">
    <h1 class="font-display text-2xl mb-6">Catat Transaksi</h1>

    <!-- SRS-B3-01: pilihan wizard SELALU bahasa awam, bukan istilah akuntansi.
         "Jual Barang/Jasa" bukan "Buat Jurnal Penjualan" — lihat premium-ux-designer agent. -->
    <div v-if="!template" class="grid grid-cols-2 gap-3">
      <button v-for="t in templates" :key="t.kode" @click="template = t"
              class="bg-white rounded-lg p-4 shadow-sm text-left hover:shadow-md transition">
        <span class="text-2xl">{{ t.ikon }}</span>
        <p class="mt-2 font-medium">{{ t.labelAwam }}</p>
      </button>
    </div>

    <form v-else @submit.prevent="submit" class="bg-white rounded-lg p-6 shadow-sm">
      <p class="text-sm text-gray-500 mb-4">{{ template.labelAwam }}</p>

      <label class="block text-sm mb-1">Jumlah (Rp)</label>
      <input v-model="jumlah" type="number" min="0" step="0.01"
             class="w-full border rounded px-3 py-2 mb-4" required />

      <label class="block text-sm mb-1">Bayar dengan</label>
      <select v-model="metodePembayaran" class="w-full border rounded px-3 py-2 mb-4">
        <option v-for="m in metodeTersedia" :key="m.kode" :value="m.kode">{{ m.label }}</option>
      </select>

      <label class="block text-sm mb-1">Tanggal</label>
      <input v-model="tanggal" type="date" class="w-full border rounded px-3 py-2 mb-6" required />

      <div class="flex gap-3">
        <button type="button" @click="template = null" class="flex-1 border rounded py-2">Batal</button>
        <button type="submit" class="flex-1 bg-brand-primary text-white rounded py-2 hover:opacity-90 transition">
          Simpan
        </button>
      </div>
    </form>
  </main>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import client from "../../api/client";

const router = useRouter();
const template = ref(null);
const jumlah = ref("");
const metodePembayaran = ref("CASH");
const tanggal = ref(new Date().toISOString().slice(0, 10));

// SRS-B3-01: setiap template punya JournalRuleMapper sendiri di backend
// (lihat TransactionWizardService.java) — frontend hanya kirim kode template + input awam,
// backend yang menerjemahkan ke jurnal double-entry.
const templates = [
  { kode: "JUAL_BARANG_JASA", labelAwam: "Jual Barang/Jasa", ikon: "🛍️" },
  { kode: "TERIMA_PEMBAYARAN", labelAwam: "Terima Pembayaran", ikon: "💰" },
  { kode: "BELI_BAHAN", labelAwam: "Beli Bahan/Barang", ikon: "📦" },
  { kode: "BAYAR_BIAYA", labelAwam: "Bayar Biaya Operasional", ikon: "🧾" }
];

// Bahasa awam, bukan istilah akuntansi: "Belum dibayar" bukan "Piutang/Hutang".
// Backend (JournalRuleMapper per template) yang menerjemahkan ke akun yang benar.
const metodeDasar = [
  { kode: "CASH", label: "Tunai" },
  { kode: "TRANSFER", label: "Transfer Bank" },
  { kode: "QRIS", label: "QRIS" }
];
const metodeTersedia = computed(() => {
  if (template.value?.kode === "JUAL_BARANG_JASA")
    return [...metodeDasar, { kode: "RECEIVABLE", label: "Belum dibayar (jual kredit)" }];
  if (template.value?.kode === "BELI_BAHAN")
    return [...metodeDasar, { kode: "PAYABLE", label: "Bayar nanti (beli kredit)" }];
  return metodeDasar;
});

async function submit() {
  await client.post("/app/transaksi", {
    kodeTemplate: template.value.kode,
    jumlah: jumlah.value,
    metodePembayaran: metodePembayaran.value,
    tanggal: tanggal.value
  });
  router.push({ name: "app-dashboard" });
}
</script>
