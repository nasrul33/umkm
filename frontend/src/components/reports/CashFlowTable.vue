<template>
  <div class="card p-6" v-if="laporan">
    <h2 class="font-display text-lg mb-4">Arus Kas (Metode Langsung) {{ tahun }}</h2>

    <div v-if="!laporan.kategori.length" class="text-center text-slate-500 text-sm py-8">
      Belum ada pergerakan kas tercatat untuk tahun {{ tahun }}.
    </div>

    <table v-else class="w-full text-sm">
      <thead>
        <tr class="text-left text-slate-500 border-b border-slate-200">
          <th class="py-2 font-medium">Kategori</th>
          <th class="py-2 font-medium text-right">Uang Masuk</th>
          <th class="py-2 font-medium text-right">Uang Keluar</th>
          <th class="py-2 font-medium text-right">Selisih</th>
        </tr>
      </thead>
      <tbody>
        <tr class="border-b border-slate-100">
          <td class="py-2.5 font-medium" colspan="3">Saldo Kas Awal</td>
          <td class="py-2.5 text-right angka">{{ formatRupiah(laporan.saldoKasAwal) }}</td>
        </tr>
        <tr v-for="row in laporan.kategori" :key="row.kategori" class="border-b border-slate-100">
          <td class="py-2.5">{{ labelKategori(row.kategori) }}</td>
          <td class="py-2.5 text-right angka">{{ formatRupiah(row.arusMasuk) }}</td>
          <td class="py-2.5 text-right angka text-red-700">{{ formatRupiah(-row.arusKeluar) }}</td>
          <td class="py-2.5 text-right angka" :class="{ 'text-red-700': row.arusBersih < 0 }">
            {{ formatRupiah(row.arusBersih) }}
          </td>
        </tr>
        <tr>
          <td class="pt-3 font-semibold" colspan="3">Saldo Kas Akhir</td>
          <td class="pt-3 text-right angka font-semibold border-t-2 border-slate-300">
            {{ formatRupiah(laporan.saldoKasAkhir) }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { formatRupiah } from "../../utils/money";

defineProps({ laporan: { type: Object, default: null }, tahun: Number });

const LABEL = {
  OPERASI: "Operasional (kegiatan usaha sehari-hari)",
  INVESTASI: "Investasi (beli/jual aset)",
  PENDANAAN: "Pendanaan (modal & pinjaman)"
};
const labelKategori = (k) => LABEL[k] ?? k;
</script>
