<template>
  <div class="card p-6 max-w-xl" v-if="laporan">
    <h2 class="font-display text-lg mb-4">Perubahan Modal (SAK EMKM) {{ laporan.tahun }}</h2>
    <dl class="text-sm">
      <div v-for="baris in daftar" :key="baris.label"
           class="flex items-center justify-between py-2.5 border-b border-slate-100">
        <dt class="text-slate-600">{{ baris.label }}</dt>
        <dd class="angka" :class="{ 'text-red-700': baris.nilai < 0 }">{{ formatRupiah(baris.nilai) }}</dd>
      </div>
      <div class="flex items-center justify-between pt-4">
        <dt class="font-semibold">Modal Akhir</dt>
        <dd class="angka-hero border-t-4 border-double border-slate-400 pt-2"
            :class="{ 'text-red-700': laporan.modalAkhir < 0 }">
          {{ formatRupiah(laporan.modalAkhir) }}
        </dd>
      </div>
    </dl>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { formatRupiah } from "../../utils/money";

const props = defineProps({ laporan: { type: Object, default: null } });

const daftar = computed(() => [
  { label: "Modal Awal", nilai: props.laporan.modalAwal },
  { label: "Setoran Pemilik", nilai: props.laporan.setoran },
  { label: "Pengambilan Pribadi (Prive)", nilai: -props.laporan.prive },
  { label: "Laba (Rugi) Tahun Berjalan", nilai: props.laporan.labaRugi }
]);
</script>
