<template>
  <div class="space-y-5">
    <div v-if="!seimbang" class="rounded-xl bg-amber-50 ring-1 ring-amber-200 text-amber-800 text-sm px-4 py-3">
      Total aset tidak sama dengan total kewajiban + modal. Kemungkinan cache laporan
      belum diperbarui — hubungi administrator.
    </div>

    <div v-for="grup in kelompok" :key="grup.tipe" class="card p-6">
      <h2 class="font-display text-lg mb-3">{{ grup.judul }}</h2>
      <table class="w-full text-sm">
        <tbody>
          <tr v-for="row in grup.rows" :key="row.kodeAkun" class="border-b border-slate-100 last:border-0">
            <td class="py-2 text-slate-400 w-16">{{ row.kodeAkun }}</td>
            <td class="py-2">{{ row.namaAkun }}</td>
            <td class="py-2 text-right angka" :class="{ 'text-red-700': tampil(row) < 0 }">
              {{ formatRupiah(tampil(row)) }}
            </td>
          </tr>
          <tr>
            <td colspan="2" class="pt-3 font-medium">Total {{ grup.judul }}</td>
            <td class="pt-3 text-right angka font-semibold border-t-2 border-slate-300">
              {{ formatRupiah(grup.total) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card-accent p-6 flex items-center justify-between">
      <p class="text-sm text-slate-500">Total Kewajiban + Modal</p>
      <p class="angka-hero">{{ formatRupiah(totalKewajibanModal) }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { displaySaldo, formatRupiah } from "../../utils/money";

const props = defineProps({ rows: { type: Array, default: () => [] } });

const tampil = (row) => displaySaldo(row.tipe, row.saldo);

const kelompok = computed(() =>
  [
    { tipe: "ASET", judul: "Aset" },
    { tipe: "KEWAJIBAN", judul: "Kewajiban" },
    { tipe: "MODAL", judul: "Modal" }
  ].map((g) => {
    const rows = props.rows.filter((r) => r.tipe === g.tipe);
    return { ...g, rows, total: rows.reduce((s, r) => s + tampil(r), 0) };
  })
);

const totalAset = computed(() => kelompok.value[0].total);
const totalKewajibanModal = computed(() => kelompok.value[1].total + kelompok.value[2].total);
const seimbang = computed(() => Math.abs(totalAset.value - totalKewajibanModal.value) < 0.005);
</script>
