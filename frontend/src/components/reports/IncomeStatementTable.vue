<template>
  <div class="space-y-5">
    <div v-if="!rows.length" class="card p-10 text-center text-slate-500 text-sm">
      Belum ada transaksi tercatat.
      <router-link :to="{ name: 'transaction-wizard' }" class="block mt-3">
        <span class="btn-secondary">Catat Transaksi Pertama</span>
      </router-link>
    </div>

    <template v-else>
      <div v-for="grup in kelompok" :key="grup.tipe" class="card p-6">
        <h2 class="font-display text-lg mb-3">{{ grup.judul }}</h2>
        <table class="w-full text-sm">
          <tbody>
            <tr v-for="row in grup.rows" :key="row.kodeAkun + row.namaAkun" class="border-b border-slate-100 last:border-0">
              <td class="py-2 text-slate-400 w-16">{{ row.kodeAkun }}</td>
              <td class="py-2">{{ row.namaAkun }}</td>
              <td class="py-2 text-right angka" :class="{ 'text-red-700': tampil(row) < 0 }">
                {{ formatRupiah(tampil(row)) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card-accent p-6 flex items-center justify-between">
        <p class="text-sm text-slate-500">Laba (Rugi) Bersih</p>
        <p class="angka-hero" :class="{ 'text-red-700': labaBersih < 0 }">
          {{ formatRupiah(labaBersih) }}
        </p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { formatRupiah } from "../../utils/money";

const props = defineProps({ rows: { type: Array, default: () => [] } });

// vw_income_statement berkonvensi kredit - debit (BEDA dari neraca):
// pendapatan sudah positif, beban sudah negatif — tampilkan apa adanya.
const tampil = (row) => Number(row.saldo);

const kelompok = computed(() =>
  [
    { tipe: "PENDAPATAN", judul: "Pendapatan" },
    { tipe: "BEBAN", judul: "Beban" }
  ].map((g) => ({ ...g, rows: props.rows.filter((r) => r.tipe === g.tipe) }))
);

// pendapatan tampil positif, beban tampil negatif -> jumlah = laba bersih
const labaBersih = computed(() => props.rows.reduce((s, r) => s + tampil(r), 0));
</script>
