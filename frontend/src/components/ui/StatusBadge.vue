<template>
  <span class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ring-1"
        :class="gaya">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({ status: { type: String, required: true } });

// Label FAKTUAL — tanpa kata bernuansa opini ("Aman"/"Bahaya") — BR-B5-11.
const MAP = {
  DI_BAWAH_BATAS: ["Di bawah batas", "bg-emerald-50 text-emerald-800 ring-emerald-200"],
  MENDEKATI_BATAS: ["Mendekati batas", "bg-amber-50 text-amber-800 ring-amber-200"],
  MELEBIHI_BATAS: ["Melebihi batas", "bg-red-50 text-red-800 ring-red-200"],
  TIDAK_BERHAK_PPH_FINAL: ["Tidak memenuhi kriteria PPh Final", "bg-slate-100 text-slate-800 ring-slate-300"],
  DRAFT: ["Draft", "bg-slate-100 text-slate-700 ring-slate-200"],
  REQUESTED: ["Diproses", "bg-blue-50 text-blue-800 ring-blue-200"],
  ISSUED: ["Kode terbit", "bg-emerald-50 text-emerald-800 ring-emerald-200"],
  PENDING_MANUAL: ["Menunggu input manual", "bg-amber-50 text-amber-800 ring-amber-200"],
  FAILED: ["Gagal", "bg-red-50 text-red-800 ring-red-200"]
};

const label = computed(() => (MAP[props.status] ?? [props.status])[0]);
const gaya = computed(() => (MAP[props.status] ?? [null, "bg-slate-100 text-slate-700 ring-slate-200"])[1]);
</script>
